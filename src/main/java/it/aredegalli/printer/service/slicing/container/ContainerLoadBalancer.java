package it.aredegalli.printer.service.slicing.container;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.SlicingContainer;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ContainerLoadBalancer {

    private final LogService logService;

    public SlicingContainer selectContainer(List<SlicingContainer> availableContainers,
                                            Model model, SlicingProperty properties) {

        if (availableContainers.isEmpty()) {
            return null;
        }

        logService.debug("ContainerLoadBalancer",
                String.format("Selecting from %d available containers for model: %s",
                        availableContainers.size(), model.getName()));

        // 1. Filter by job priority and model characteristics
        List<SlicingContainer> candidates = filterCandidateContainers(availableContainers, model, properties);

        if (candidates.isEmpty()) {
            candidates = availableContainers; // Fallback to all available
        }

        // 2. Apply load balancing algorithm
        return applyLoadBalancingAlgorithm(candidates);
    }

    private List<SlicingContainer> filterCandidateContainers(List<SlicingContainer> containers,
                                                             Model model, SlicingProperty properties) {

        long modelSizeMB = model.getFileResource().getFileSize() / (1024 * 1024);

        // Priority-based filtering
        if (shouldUsePriorityContainer(properties)) {
            List<SlicingContainer> priorityContainers = containers.stream()
                    .filter(c -> "priority".equals(c.getContainerType()))
                    .collect(Collectors.toList());

            if (!priorityContainers.isEmpty()) {
                logService.debug("ContainerLoadBalancer", "Using priority containers for high-priority job");
                return priorityContainers;
            }
        }

        // Size-based filtering
        if (modelSizeMB > 50) { // Large models
            List<SlicingContainer> standardContainers = containers.stream()
                    .filter(c -> !"batch".equals(c.getContainerType()))
                    .collect(Collectors.toList());

            if (!standardContainers.isEmpty()) {
                logService.debug("ContainerLoadBalancer", "Using standard containers for large model");
                return standardContainers;
            }
        } else if (modelSizeMB < 5) { // Small models
            List<SlicingContainer> batchContainers = containers.stream()
                    .filter(c -> "batch".equals(c.getContainerType()))
                    .collect(Collectors.toList());

            if (!batchContainers.isEmpty()) {
                logService.debug("ContainerLoadBalancer", "Using batch containers for small model");
                return batchContainers;
            }
        }

        return containers; // No special filtering needed
    }

    private SlicingContainer applyLoadBalancingAlgorithm(List<SlicingContainer> containers) {
        // Weighted scoring algorithm considering:
        // 1. Container priority (lower = better)
        // 2. Current load (lower = better)
        // 3. Recent performance (success rate, etc.)

        return containers.stream()
                .min(Comparator.comparing(this::calculateContainerScore))
                .orElse(null);
    }

    private double calculateContainerScore(SlicingContainer container) {
        double score = 0.0;

        // Priority weight (0-100, lower priority = lower score)
        score += container.getPriority() * 10;

        // Load weight (0-100, lower load = lower score)
        score += container.getLoadPercentage();

        // Health weight (penalties for recent failures, etc.)
        double successRate = calculateSuccessRate(container);
        score += (100 - successRate) * 0.5;

        logService.debug("ContainerLoadBalancer",
                String.format("Container %s score: %.2f (priority=%d, load=%.1f%%, success=%.1f%%)",
                        container.getContainerName(), score, container.getPriority(),
                        container.getLoadPercentage(), successRate));

        return score;
    }

    private boolean shouldUsePriorityContainer(SlicingProperty properties) {
        // Logic to determine if job needs priority container
        // Could be based on:
        // - Explicit priority flag
        // - Job complexity
        // - User/customer tier
        // - Time constraints

        return false; // Placeholder - implement based on business rules
    }

    private double calculateSuccessRate(SlicingContainer container) {
        long total = container.getTotalJobsProcessed();
        long failed = container.getTotalJobsFailed();

        if (total == 0) return 100.0; // New container, assume 100%

        return ((double) (total - failed) / total) * 100.0;
    }
}