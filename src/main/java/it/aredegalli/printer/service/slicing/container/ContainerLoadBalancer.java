package it.aredegalli.printer.service.slicing.container;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.SlicingContainer;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Simple load balancer for selecting optimal containers
 */
@Component
@RequiredArgsConstructor
public class ContainerLoadBalancer {

    private final LogService logService;

    /**
     * Select the best container based on current load and priority
     */
    public SlicingContainer selectContainer(List<SlicingContainer> availableContainers,
                                            Model model,
                                            SlicingProperty properties) {

        if (availableContainers == null || availableContainers.isEmpty()) {
            logService.warn("ContainerLoadBalancer", "No containers available for selection");
            return null;
        }

        // Strategy: Select container with lowest current job count and highest priority
        SlicingContainer selected = availableContainers.stream()
                .sorted(Comparator
                        .comparingInt(SlicingContainer::getCurrentActiveJobs) // Least busy first
                        .thenComparing(SlicingContainer::getPriority) // Then by priority
                        .thenComparing(SlicingContainer::getContainerName)) // Then by name for consistency
                .findFirst()
                .orElse(null);

        if (selected != null) {
            logService.debug("ContainerLoadBalancer",
                    String.format("Selected container %s - Active jobs: %d/%d, Priority: %d",
                            selected.getContainerName(),
                            selected.getCurrentActiveJobs(),
                            selected.getMaxConcurrentJobs(),
                            selected.getPriority()));
        }

        return selected;
    }

    /**
     * Calculate container load score (0-100, lower is better)
     */
    public double calculateLoadScore(SlicingContainer container) {
        if (container.getMaxConcurrentJobs() == 0) {
            return 100.0; // Container not available
        }

        double loadRatio = (double) container.getCurrentActiveJobs() / container.getMaxConcurrentJobs();
        return loadRatio * 100.0;
    }

    /**
     * Check if container can handle additional job
     */
    public boolean canAcceptJob(SlicingContainer container) {
        return container.getCurrentActiveJobs() < container.getMaxConcurrentJobs();
    }
}