package it.aredegalli.printer.service.slicing.container;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.*;
import it.aredegalli.printer.repository.resource.FileResourceRepository;
import it.aredegalli.printer.repository.slicing.SlicingContainerRepository;
import it.aredegalli.printer.repository.slicing.SlicingJobAssignmentRepository;
import it.aredegalli.printer.repository.slicing.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import it.aredegalli.printer.service.slicing.engine.DockerSlicerEngine;
import it.aredegalli.printer.service.storage.StorageService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ContainerManager {

    private final SlicingContainerRepository containerRepository;
    private final LogService logService;
    private final RestTemplate restTemplate;

    // Container cache per performance
    private final Map<String, SlicingContainer> containerCache = new ConcurrentHashMap<>();
    private final Map<String, ContainerHealthStatus> healthCache = new ConcurrentHashMap<>();

    @Value("${slicing.engines.docker.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${slicing.engines.docker.health-check.interval-seconds:30}")
    private int healthCheckIntervalSeconds;

    @PostConstruct
    public void initialize() {
        try {
            if (discoveryEnabled) {
                logService.info("ContainerManager", "Initializing container discovery...");
                discoverContainers();
            } else {
                logService.info("ContainerManager", "Container discovery disabled, loading from database...");
                refreshContainerCache();
            }

            logService.info("ContainerManager",
                    "Container Manager initialized with " + containerCache.size() + " containers");

        } catch (Exception e) {
            logService.error("ContainerManager",
                    "Failed to initialize ContainerManager: " + e.getMessage());
            // Non rilanciare l'eccezione per evitare il fallimento dell'avvio dell'applicazione
        }
    }

    /**
     * Discover containers with error handling
     */
    @Transactional
    public void discoverContainers() {
        logService.info("ContainerManager", "Starting container discovery...");

        try {
            // Get configured containers from database
            List<SlicingContainer> configuredContainers = containerRepository.findAll();

            if (configuredContainers.isEmpty()) {
                logService.warn("ContainerManager", "No containers configured in database");
                return;
            }

            for (SlicingContainer container : configuredContainers) {
                try {
                    // Check if container is reachable
                    ContainerHealthStatus health = checkContainerHealth(container);

                    if (health.isHealthy()) {
                        containerCache.put(container.getContainerId(), container);
                        healthCache.put(container.getContainerId(), health);

                        // Update container status in database with transaction
                        updateContainerStatusSafe(container.getId(), ContainerStatus.HEALTHY);

                        logService.info("ContainerManager",
                                String.format("Discovered healthy container: %s at %s:%d",
                                        container.getContainerName(), container.getHost(), container.getPort()));
                    } else {
                        updateContainerStatusSafe(container.getId(), ContainerStatus.UNHEALTHY);
                        logService.warn("ContainerManager",
                                String.format("Container %s is unhealthy: %s",
                                        container.getContainerName(), health.getErrorMessage()));
                    }

                } catch (Exception e) {
                    logService.error("ContainerManager",
                            String.format("Failed to check container %s: %s",
                                    container.getContainerName(), e.getMessage()));

                    updateContainerStatusSafe(container.getId(), ContainerStatus.UNKNOWN);
                }
            }

        } catch (Exception e) {
            logService.error("ContainerManager",
                    "Container discovery failed: " + e.getMessage());
        }

        logService.info("ContainerManager",
                String.format("Container discovery completed. Found %d healthy containers",
                        containerCache.size()));
    }

    /**
     * Safe method to update container status with proper transaction handling
     */
    @Transactional
    public void updateContainerStatusSafe(UUID containerId, ContainerStatus status) {
        try {
            containerRepository.updateStatus(containerId, status, Instant.now());
            logService.debug("ContainerManager",
                    String.format("Updated container %s status to %s", containerId, status));
        } catch (Exception e) {
            logService.error("ContainerManager",
                    String.format("Failed to update container %s status: %s", containerId, e.getMessage()));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerHealthResponse {
        private String prusaslicer_version;
        private Long available_memory;
        private Integer active_jobs;
        private String status;
    }

    /**
     * Health check with better error handling
     */
    private ContainerHealthStatus checkContainerHealth(SlicingContainer container) {
        try {
            String healthUrl = String.format("http://%s:%d/health", container.getHost(), container.getPort());

            ResponseEntity<ContainerHealthResponse> response = restTemplate.getForEntity(
                    healthUrl, ContainerHealthResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ContainerHealthResponse healthData = response.getBody();

                if (healthData == null) {
                    return ContainerHealthStatus.unhealthy("Empty health response");
                }

                String slicerVersion = healthData.getPrusaslicer_version() != null ?
                        healthData.getPrusaslicer_version() : "unknown";

                long availableMemory = healthData.getAvailable_memory() != null ?
                        healthData.getAvailable_memory() : 0L;

                int activeJobs = healthData.getActive_jobs() != null ?
                        healthData.getActive_jobs() : 0;

                return ContainerHealthStatus.healthy(slicerVersion, availableMemory, activeJobs);

            } else {
                return ContainerHealthStatus.unhealthy("HTTP " + response.getStatusCode().value());
            }

        } catch (ResourceAccessException e) {
            // Specifico per errori di connessione
            return ContainerHealthStatus.unhealthy("Connection timeout: " + e.getMessage());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Specifico per errori HTTP
            return ContainerHealthStatus.unhealthy("HTTP error " + e.getStatusCode().value() + ": " + e.getMessage());
        } catch (Exception e) {
            return ContainerHealthStatus.unhealthy("Health check failed: " + e.getMessage());
        }
    }

    /**
     * Scheduled health check with transaction support
     */
    @Scheduled(fixedDelayString = "#{${slicing.engines.docker.health-check.interval-seconds:30} * 1000}")
    @Transactional
    public void performHealthChecks() {
        if (!discoveryEnabled || containerCache.isEmpty()) {
            return;
        }

        logService.debug("ContainerManager", "Performing scheduled health checks...");

        for (SlicingContainer container : containerCache.values()) {
            try {
                ContainerHealthStatus health = checkContainerHealth(container);
                healthCache.put(container.getContainerId(), health);

                ContainerStatus newStatus = health.isHealthy() ?
                        ContainerStatus.HEALTHY : ContainerStatus.UNHEALTHY;

                updateContainerStatusSafe(container.getId(), newStatus);

                if (!health.isHealthy()) {
                    logService.warn("ContainerManager",
                            String.format("Container %s became unhealthy: %s",
                                    container.getContainerName(), health.getErrorMessage()));
                }

            } catch (Exception e) {
                logService.error("ContainerManager",
                        String.format("Health check failed for container %s: %s",
                                container.getContainerName(), e.getMessage()));
            }
        }
    }

    /**
     * Load containers from database without health checks
     */
    @Transactional(readOnly = true)
    public void refreshContainerCache() {
        try {
            containerCache.clear();
            List<SlicingContainer> containers = containerRepository.findByStatus(ContainerStatus.HEALTHY);

            for (SlicingContainer container : containers) {
                containerCache.put(container.getContainerId(), container);
                // Set a default healthy status for cached containers
                healthCache.put(container.getContainerId(),
                        ContainerHealthStatus.healthy("unknown", 0, 0));
            }

            logService.info("ContainerManager",
                    String.format("Loaded %d containers from database", containers.size()));

        } catch (Exception e) {
            logService.error("ContainerManager",
                    "Failed to refresh container cache: " + e.getMessage());
        }
    }

    /**
     * Get available containers for slicing
     */
    public List<SlicingContainer> getAvailableContainers() {
        return containerCache.values().stream()
                .filter(container -> {
                    ContainerHealthStatus health = healthCache.get(container.getContainerId());
                    return health != null && health.isHealthy() &&
                            container.getCurrentActiveJobs() < container.getMaxConcurrentJobs();
                })
                .sorted(Comparator.comparing(SlicingContainer::getPriority))
                .toList();
    }

    /**
     * Check if any containers are available
     */
    public boolean hasAvailableContainers() {
        return !getAvailableContainers().isEmpty();
    }

    /**
     * Get engine version from available containers
     */
    public String getEngineVersion() {
        return containerCache.values().stream()
                .findFirst()
                .map(container -> {
                    ContainerHealthStatus health = healthCache.get(container.getContainerId());
                    return health != null ? health.getSlicerVersion() : "unknown";
                })
                .orElse("unknown");
    }

    // Metodi aggiuntivi per il management dei container...

    public Map<String, Integer> getContainersByType() {
        return containerCache.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        SlicingContainer::getContainerType,
                        java.util.stream.Collectors.summingInt(c -> 1)
                ));
    }

    public int getHealthyContainerCount() {
        return (int) healthCache.values().stream()
                .filter(ContainerHealthStatus::isHealthy)
                .count();
    }

    // ======================================
// METODI MANCANTI DA AGGIUNGERE AL ContainerManager
// ======================================

    // 1. Aggiungere questa dipendenza alla classe ContainerManager
    private final ContainerLoadBalancer loadBalancer;
    private final FileResourceService fileResourceService;
    private final StorageService storageService;
    private final FileResourceRepository fileResourceRepository;
    private final SlicingResultRepository slicingResultRepository;
    private final SlicingJobAssignmentRepository jobAssignmentRepository;

    /**
     * Select the optimal container for a slicing job
     */
    public SlicingContainer selectOptimalContainer(Model model, SlicingProperty properties) {
        logService.debug("ContainerManager", "Selecting optimal container for model: " + model.getName());

        // Get available containers
        List<SlicingContainer> availableContainers = getAvailableContainers();
        if (availableContainers.isEmpty()) {
            logService.warn("ContainerManager", "No available containers found");
            return null;
        }

        // Simple load balancing - select least busy container
        // Se hai un LoadBalancer, usa quello, altrimenti questa implementazione semplice
        SlicingContainer selected = availableContainers.stream()
                .min(Comparator.comparingInt(SlicingContainer::getCurrentActiveJobs))
                .orElse(null);

        if (selected != null) {
            logService.info("ContainerManager",
                    String.format("Selected container %s (type: %s) for model %s",
                            selected.getContainerName(), selected.getContainerType(), model.getName()));
        }

        return selected;
    }

    /**
     * Execute slicing on a specific container
     */
    @Transactional
    public SlicingResult executeSlicing(SlicingContainer container, Model model, SlicingProperty properties) {
        logService.info("ContainerManager",
                String.format("Executing slicing on container %s for model %s",
                        container.getContainerName(), model.getName()));

        SlicingJobAssignment assignment = null;

        try {
            // 1. Create job assignment record (se hai il repository)
            // assignment = createJobAssignment(null, container);

            // 2. Mark container as busy
            updateContainerJobCount(container.getId(), 1);

            // 3. Prepare slicing request
            SlicingRequest request = prepareSlicingRequest(model, properties);

            // 4. Execute slicing via HTTP API
            SlicingResponse response = executeContainerSlicing(container, request);

            // 5. Process response and create SlicingResult
            SlicingResult result = processSlicingResponse(response, model, properties);

            // 6. Update assignment as completed (se hai il sistema di assignment)
            // updateJobAssignmentStatus(assignment, SlicingJobAssignmentStatus.COMPLETED, null);

            return result;

        } catch (Exception e) {
            // Update assignment as failed
            // if (assignment != null) {
            //     updateJobAssignmentStatus(assignment, SlicingJobAssignmentStatus.FAILED, e.getMessage());
            // }

            logService.error("ContainerManager",
                    String.format("Slicing failed on container %s: %s", container.getContainerName(), e.getMessage()));

            throw new ContainerSlicingException("Container slicing failed: " + e.getMessage(), e);

        } finally {
            // Always release container resource
            updateContainerJobCount(container.getId(), -1);
        }
    }

    /**
     * Get container statistics
     */
    public DockerSlicerEngine.ContainerStats getContainerStatistics() {
        List<SlicingContainer> allContainers = List.copyOf(containerCache.values());

        int total = allContainers.size();
        int available = (int) allContainers.stream()
                .filter(c -> {
                    ContainerHealthStatus health = healthCache.get(c.getContainerId());
                    return health != null && health.isHealthy() &&
                            c.getCurrentActiveJobs() < c.getMaxConcurrentJobs();
                })
                .count();
        int busy = (int) allContainers.stream()
                .filter(c -> c.getCurrentActiveJobs() >= c.getMaxConcurrentJobs())
                .count();
        int unhealthy = (int) allContainers.stream()
                .filter(c -> {
                    ContainerHealthStatus health = healthCache.get(c.getContainerId());
                    return health == null || !health.isHealthy();
                })
                .count();

        Map<String, Integer> byType = getContainersByType();
        double avgHealth = healthCache.values().stream()
                .filter(ContainerHealthStatus::isHealthy)
                .mapToDouble(h -> 100.0) // Healthy = 100%
                .average()
                .orElse(0.0);

        return new DockerSlicerEngine.ContainerStats(total, available, busy, unhealthy, byType, avgHealth);
    }

    /**
     * Update container job count
     */
    @Transactional
    public void updateContainerJobCount(UUID containerId, int delta) {
        try {
            containerRepository.updateActiveJobCount(containerId, delta);
            logService.debug("ContainerManager",
                    String.format("Updated container %s job count by %d", containerId, delta));
        } catch (Exception e) {
            logService.error("ContainerManager",
                    String.format("Failed to update container %s job count: %s", containerId, e.getMessage()));
        }
    }

// ======================================
// METODI DI SUPPORTO PER LO SLICING
// ======================================

    private SlicingRequest prepareSlicingRequest(Model model, SlicingProperty properties) throws Exception {
        // Download STL data (se hai FileResourceService)
        // try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
        //     byte[] stlData = stlStream.readAllBytes();
        //     String stlBase64 = Base64.getEncoder().encodeToString(stlData);

        // Per ora una implementazione semplificata
        Map<String, Object> config = new HashMap<>();
        config.put("layer_height", properties.getLayerHeightMm());
        config.put("first_layer_height", properties.getFirstLayerHeightMm());
        config.put("perimeters", properties.getPerimeterCount());
        config.put("infill_density", properties.getInfillPercentage());
        config.put("print_speed", properties.getPrintSpeedMmS());
        config.put("extruder_temp", properties.getExtruderTempC());
        config.put("bed_temp", properties.getBedTempC());

        return SlicingRequest.builder()
                .jobId(UUID.randomUUID().toString())
                .stlData("") // Placeholder - implementare con FileResourceService
                .config(config)
                .modelName(model.getName())
                .build();
        // }
    }

    private SlicingResponse executeContainerSlicing(SlicingContainer container, SlicingRequest request) {
        try {
            String slicingUrl = String.format("http://%s:%d/slice", container.getHost(), container.getPort());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SlicingRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<SlicingResponse> response = restTemplate.postForEntity(slicingUrl, entity, SlicingResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new ContainerSlicingException("Container returned non-OK status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new ContainerSlicingException("Failed to communicate with container: " + e.getMessage(), e);
        }
    }

    private SlicingResult processSlicingResponse(SlicingResponse response, Model model, SlicingProperty properties) throws Exception {
        if (!response.isSuccess()) {
            throw new ContainerSlicingException("Container slicing failed: " + response.getError());
        }

        // Implementazione semplificata - sostituire con la logica completa quando hai tutti i servizi
        SlicingResult slicingResult = SlicingResult.builder()
                .sourceFile(model.getFileResource())
                .generatedFile(null) // Implementare con StorageService
                .slicingProperty(properties)
                .lines(response.getMetrics() != null ? response.getMetrics().getLines() : 0)
                .createdAt(Instant.now())
                .build();

        // return slicingResultRepository.save(slicingResult);
        return slicingResult; // Placeholder
    }

    // Exception for container operations
    public static class ContainerSlicingException extends RuntimeException {
        public ContainerSlicingException(String message) {
            super(message);
        }

        public ContainerSlicingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}