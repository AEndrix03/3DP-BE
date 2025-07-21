package it.aredegalli.printer.service.slicing.container;

import it.aredegalli.printer.dto.storage.UploadResult;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.resource.FileResource;
import it.aredegalli.printer.model.slicing.*;
import it.aredegalli.printer.repository.resource.FileResourceRepository;
import it.aredegalli.printer.repository.slicing.SlicingJobAssignmentRepository;
import it.aredegalli.printer.repository.slicing.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import it.aredegalli.printer.service.slicing.engine.DockerSlicerEngine;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing Docker slicing containers
 */
@Service
@RequiredArgsConstructor
public class ContainerManager {

    private final SlicingContainerRepository containerRepository;
    private final SlicingJobAssignmentRepository jobAssignmentRepository;
    private final ContainerLoadBalancer loadBalancer;
    private final FileResourceService fileResourceService;
    private final StorageService storageService;
    private final FileResourceRepository fileResourceRepository;
    private final SlicingResultRepository slicingResultRepository;
    private final LogService logService;
    private final RestTemplate restTemplate;

    // Container cache for performance
    private final Map<String, SlicingContainer> containerCache = new ConcurrentHashMap<>();
    private final Map<String, ContainerHealthStatus> healthCache = new ConcurrentHashMap<>();

    @Value("${slicing.engines.docker.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${slicing.engines.docker.health-check.interval-seconds:30}")
    private int healthCheckIntervalSeconds;

    @Value("${slicing.engines.docker.job.timeout-seconds:600}")
    private int jobTimeoutSeconds;

    @PostConstruct
    public void initialize() {
        if (discoveryEnabled) {
            logService.info("ContainerManager", "Initializing container discovery...");
            discoverContainers();
        }

        refreshContainerCache();
        logService.info("ContainerManager", "Container Manager initialized with " + containerCache.size() + " containers");
    }

    // ======================================
    // MAIN SLICING OPERATIONS
    // ======================================

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

        // Use load balancer to select best container
        SlicingContainer selected = loadBalancer.selectContainer(availableContainers, model, properties);

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
    public SlicingResult executeSlicing(SlicingContainer container, Model model, SlicingProperty properties) {
        logService.info("ContainerManager",
                String.format("Executing slicing on container %s for model %s",
                        container.getContainerName(), model.getName()));

        SlicingJobAssignment assignment = null;

        try {
            // 1. Create job assignment record
            assignment = createJobAssignment(null, container); // Queue ID would come from SlicingQueue

            // 2. Mark container as busy
            updateContainerJobCount(container.getId(), 1);

            // 3. Prepare slicing request
            SlicingRequest request = prepareSlicingRequest(model, properties);

            // 4. Execute slicing via HTTP API
            SlicingResponse response = executeContainerSlicing(container, request);

            // 5. Process response and create SlicingResult
            SlicingResult result = processSlicingResponse(response, model, properties);

            // 6. Update assignment as completed
            updateJobAssignmentStatus(assignment, SlicingJobAssignmentStatus.COMPLETED, null);

            return result;

        } catch (Exception e) {
            // Update assignment as failed
            if (assignment != null) {
                updateJobAssignmentStatus(assignment, SlicingJobAssignmentStatus.FAILED, e.getMessage());
            }

            logService.error("ContainerManager",
                    String.format("Slicing failed on container %s: %s", container.getContainerName(), e.getMessage()));

            throw new ContainerSlicingException("Container slicing failed: " + e.getMessage(), e);

        } finally {
            // Always release container resource
            updateContainerJobCount(container.getId(), -1);
        }
    }

    // ======================================
    // CONTAINER DISCOVERY AND HEALTH
    // ======================================

    /**
     * Discover available containers from Docker or configuration
     */
    public void discoverContainers() {
        logService.info("ContainerManager", "Starting container discovery...");

        // Get configured containers from database
        List<SlicingContainer> configuredContainers = containerRepository.findAll();

        for (SlicingContainer container : configuredContainers) {
            try {
                // Check if container is reachable
                ContainerHealthStatus health = checkContainerHealth(container);

                if (health.isHealthy()) {
                    containerCache.put(container.getContainerId(), container);
                    healthCache.put(container.getContainerId(), health);

                    // Update container status in database
                    updateContainerStatus(container.getId(), ContainerStatus.HEALTHY);

                    logService.info("ContainerManager",
                            String.format("Discovered healthy container: %s at %s:%d",
                                    container.getContainerName(), container.getHost(), container.getPort()));
                } else {
                    updateContainerStatus(container.getId(), ContainerStatus.UNHEALTHY);
                    logService.warn("ContainerManager",
                            String.format("Container %s is unhealthy: %s",
                                    container.getContainerName(), health.getErrorMessage()));
                }

            } catch (Exception e) {
                updateContainerStatus(container.getId(), ContainerStatus.UNKNOWN);
                logService.error("ContainerManager",
                        String.format("Failed to check container %s: %s",
                                container.getContainerName(), e.getMessage()));
            }
        }

        logService.info("ContainerManager",
                String.format("Container discovery completed. Found %d healthy containers",
                        containerCache.size()));
    }

    /**
     * Scheduled health check for all containers
     */
    @Scheduled(fixedDelayString = "#{${slicing.engines.docker.health-check.interval-seconds:30} * 1000}")
    public void performHealthChecks() {
        if (!discoveryEnabled) return;

        logService.debug("ContainerManager", "Performing scheduled health checks...");

        for (SlicingContainer container : containerCache.values()) {
            try {
                ContainerHealthStatus health = checkContainerHealth(container);
                healthCache.put(container.getContainerId(), health);

                ContainerStatus newStatus = health.isHealthy() ? ContainerStatus.HEALTHY : ContainerStatus.UNHEALTHY;
                updateContainerStatus(container.getId(), newStatus);

                if (!health.isHealthy()) {
                    logService.warn("ContainerManager",
                            String.format("Container %s became unhealthy: %s",
                                    container.getContainerName(), health.getErrorMessage()));
                }

            } catch (Exception e) {
                logService.error("ContainerManager",
                        String.format("Health check failed for container %s: %s",
                                container.getContainerName(), e.getMessage()));

                updateContainerStatus(container.getId(), ContainerStatus.UNKNOWN);
                healthCache.put(container.getContainerId(),
                        ContainerHealthStatus.unhealthy("Health check failed: " + e.getMessage()));
            }
        }
    }

    private ContainerHealthStatus checkContainerHealth(SlicingContainer container) {
        try {
            String healthUrl = String.format("http://%s:%d/health", container.getHost(), container.getPort());

            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> healthData = response.getBody();

                return ContainerHealthStatus.healthy(
                        (String) healthData.get("prusaslicer_version"),
                        ((Number) healthData.get("available_memory")).longValue(),
                        0 // active jobs - would come from health data
                );
            } else {
                return ContainerHealthStatus.unhealthy("HTTP " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            return ContainerHealthStatus.unhealthy("Connection failed: " + e.getMessage());
        }
    }

    // ======================================
    // SLICING REQUEST PROCESSING
    // ======================================

    private SlicingRequest prepareSlicingRequest(Model model, SlicingProperty properties) throws Exception {
        // Download STL data
        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
            byte[] stlData = stlStream.readAllBytes();
            String stlBase64 = Base64.getEncoder().encodeToString(stlData);

            // Prepare configuration
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
                    .stlData(stlBase64)
                    .config(config)
                    .modelName(model.getName())
                    .build();
        }
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

        // Decode G-code data
        byte[] gcodeBytes = Base64.getDecoder().decode(response.getGcodeData());
        String filename = model.getName().replaceAll("\\.[^.]*$", "") + ".gcode";

        // Upload G-code to storage
        UploadResult result = storageService.upload(
                new ByteArrayInputStream(gcodeBytes),
                gcodeBytes.length,
                "text/plain",
                PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME
        );

        FileResource gcodeFile = fileResourceRepository.save(FileResource.builder()
                .fileName(filename)
                .fileType("text/plain")
                .fileSize(gcodeBytes.length)
                .fileHash(result.getHashBytes())
                .objectKey(result.getObjectKey())
                .bucketName(PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME)
                .uploadedAt(Instant.now())
                .build());

        // Create SlicingResult
        SlicingResult slicingResult = SlicingResult.builder()
                .sourceFile(model.getFileResource())
                .generatedFile(gcodeFile)
                .slicingProperty(properties)
                .lines(response.getMetrics() != null ? response.getMetrics().getLines() : 0)
                .createdAt(Instant.now())
                .build();

        return slicingResultRepository.save(slicingResult);
    }

    // ======================================
    // UTILITY METHODS
    // ======================================

    public List<SlicingContainer> getAvailableContainers() {
        return containerCache.values().stream()
                .filter(container -> {
                    ContainerHealthStatus health = healthCache.get(container.getContainerId());
                    return health != null && health.isHealthy() &&
                            container.getCurrentActiveJobs() < container.getMaxConcurrentJobs();
                })
                .sorted(Comparator.comparing(SlicingContainer::getPriority))
                .collect(Collectors.toList());
    }

    public boolean hasAvailableContainers() {
        return !getAvailableContainers().isEmpty();
    }

    public Map<String, Integer> getContainersByType() {
        return containerCache.values().stream()
                .collect(Collectors.groupingBy(
                        SlicingContainer::getContainerType,
                        Collectors.summingInt(c -> 1)
                ));
    }

    public String getEngineVersion() {
        return containerCache.values().stream()
                .findFirst()
                .map(container -> {
                    ContainerHealthStatus health = healthCache.get(container.getContainerId());
                    return health != null ? health.getSlicerVersion() : "unknown";
                })
                .orElse("unknown");
    }

    public DockerSlicerEngine.ContainerStats getContainerStatistics() {
        List<SlicingContainer> allContainers = new ArrayList<>(containerCache.values());

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

    private void refreshContainerCache() {
        containerCache.clear();
        List<SlicingContainer> containers = containerRepository.findByStatus(ContainerStatus.HEALTHY);
        for (SlicingContainer container : containers) {
            containerCache.put(container.getContainerId(), container);
        }
    }

    private void updateContainerStatus(UUID containerId, ContainerStatus status) {
        containerRepository.updateStatus(containerId, status, Instant.now());
    }

    private void updateContainerJobCount(UUID containerId, int delta) {
        containerRepository.updateActiveJobCount(containerId, delta);
    }

    private SlicingJobAssignment createJobAssignment(UUID queueId, SlicingContainer container) {
        SlicingJobAssignment assignment = SlicingJobAssignment.builder()
                .slicingQueueId(queueId) // This would come from actual queue
                .containerId(container.getId())
                .assignedAt(Instant.now())
                .assignmentStatus(SlicingJobAssignmentStatus.ASSIGNED)
                .assignmentPriority(5) // Default priority
                .build();

        return jobAssignmentRepository.save(assignment);
    }

    private void updateJobAssignmentStatus(SlicingJobAssignment assignment,
                                           SlicingJobAssignmentStatus status,
                                           String errorMessage) {
        assignment.setAssignmentStatus(status);
        if (status == SlicingJobAssignmentStatus.STARTED) {
            assignment.setStartedAt(Instant.now());
        } else if (status == SlicingJobAssignmentStatus.COMPLETED || status == SlicingJobAssignmentStatus.FAILED) {
            assignment.setCompletedAt(Instant.now());
            if (errorMessage != null) {
                assignment.setLastErrorMessage(errorMessage);
            }
        }

        jobAssignmentRepository.save(assignment);
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