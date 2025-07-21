package it.aredegalli.printer.service.slicing.engine;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.SlicingContainer;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.model.slicing.SlicingResult;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.container.ContainerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Docker-based slicing engine that delegates slicing to Docker containers
 */
@Component("dockerSlicerEngine")
@ConditionalOnProperty(name = "slicing.engines.docker.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DockerSlicerEngine implements SlicingEngine {

    private final ContainerManager containerManager;
    private final LogService logService;

    @Override
    public SlicingResult slice(Model model, SlicingProperty properties) {
        logService.info("DockerSlicerEngine", "Starting Docker-based slicing for model: " + model.getId());

        try {
            // 1. Find best available container
            SlicingContainer container = containerManager.selectOptimalContainer(model, properties);
            if (container == null) {
                throw new SlicingException("No available containers for slicing");
            }

            logService.info("DockerSlicerEngine",
                    String.format("Selected container: %s (type: %s, priority: %d)",
                            container.getContainerName(), container.getContainerType(), container.getPriority()));

            // 2. Execute slicing on selected container
            SlicingResult result = containerManager.executeSlicing(container, model, properties);

            logService.info("DockerSlicerEngine",
                    String.format("Slicing completed on container %s for model %s",
                            container.getContainerName(), model.getName()));

            return result;

        } catch (Exception e) {
            logService.error("DockerSlicerEngine", "Docker slicing failed: " + e.getMessage());
            throw new SlicingException("Docker slicing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateModel(Model model) {
        if (model == null || model.getFileResource() == null) {
            return false;
        }

        // Check if we have available containers
        boolean hasAvailableContainers = containerManager.hasAvailableContainers();
        if (!hasAvailableContainers) {
            logService.warn("DockerSlicerEngine", "No available Docker containers for slicing");
            return false;
        }

        // Standard model validation
        String fileType = model.getFileResource().getFileType();
        if (fileType == null || !fileType.toLowerCase().contains("stl")) {
            logService.warn("DockerSlicerEngine", "Unsupported file type: " + fileType);
            return false;
        }

        long fileSize = model.getFileResource().getFileSize();
        if (fileSize <= 0 || fileSize > 500 * 1024 * 1024) { // 500MB limit for Docker
            logService.warn("DockerSlicerEngine", "Invalid file size for Docker slicing: " + fileSize);
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "DockerSlicer";
    }

    @Override
    public String getVersion() {
        try {
            // Get version from container manager
            return containerManager.getEngineVersion();
        } catch (Exception e) {
            logService.warn("DockerSlicerEngine", "Could not determine Docker engine version");
            return "unknown";
        }
    }

    /**
     * Get statistics about available containers
     */
    public ContainerStats getContainerStats() {
        return containerManager.getContainerStatistics();
    }

    /**
     * Check if any containers are available
     */
    public boolean isAvailable() {
        return containerManager.hasAvailableContainers();
    }

    /**
     * Get number of available containers by type
     */
    public java.util.Map<String, Integer> getContainersByType() {
        return containerManager.getContainersByType();
    }

    // Custom exception for Docker slicing
    public static class SlicingException extends RuntimeException {
        public SlicingException(String message) {
            super(message);
        }

        public SlicingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    // Statistics container
    public static class ContainerStats {
        private final int totalContainers;
        private final int availableContainers;
        private final int busyContainers;
        private final int unhealthyContainers;
        private final java.util.Map<String, Integer> containersByType;
        private final double averageHealthScore;

        public ContainerStats(int total, int available, int busy, int unhealthy,
                              java.util.Map<String, Integer> byType, double healthScore) {
            this.totalContainers = total;
            this.availableContainers = available;
            this.busyContainers = busy;
            this.unhealthyContainers = unhealthy;
            this.containersByType = byType;
            this.averageHealthScore = healthScore;
        }

        // Getters
        public int getTotalContainers() {
            return totalContainers;
        }

        public int getAvailableContainers() {
            return availableContainers;
        }

        public int getBusyContainers() {
            return busyContainers;
        }

        public int getUnhealthyContainers() {
            return unhealthyContainers;
        }

        public java.util.Map<String, Integer> getContainersByType() {
            return containersByType;
        }

        public double getAverageHealthScore() {
            return averageHealthScore;
        }

        public boolean isHealthy() {
            return availableContainers > 0 && averageHealthScore >= 70;
        }
    }
}