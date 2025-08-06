package it.aredegalli.printer.controller.api.slicing;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueRepository;
import it.aredegalli.printer.scheduled.slicing.SlicingQueueProcessor;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/slicing/monitoring")
@RequiredArgsConstructor
public class SlicingMonitoringController {

    private final SlicingQueueRepository slicingQueueRepository;
    private final SlicingQueueProcessor queueProcessor;
    private final LogService logService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        logService.info("SlicingMonitoringController", "Getting comprehensive queue statistics");

        try {
            Map<String, Object> stats = new HashMap<>();

            long queued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());
            long completed = slicingQueueRepository.countByStatus(SlicingStatus.COMPLETED.getCode());
            long failed = slicingQueueRepository.countByStatus(SlicingStatus.FAILED.getCode());

            stats.put("current_status", Map.of(
                    "queued", queued,
                    "processing", processing,
                    "completed", completed,
                    "failed", failed,
                    "total", queued + processing + completed + failed
            ));

            stats.put("processor_stats", Map.of(
                    "enabled", queueProcessor.isProcessingEnabled(),
                    "currently_processing", queueProcessor.getCurrentlyProcessing(),
                    "total_processed", queueProcessor.getProcessedCount(),
                    "total_failed", queueProcessor.getFailedCount(),
                    "last_processing_time", queueProcessor.getLastProcessingTime()
            ));

            stats.put("performance", Map.of(
                    "success_rate", calculateSuccessRate(completed, failed),
                    "queue_health", queued < 10 ? "healthy" : queued < 50 ? "warning" : "critical"
            ));

            stats.put("timestamp", Instant.now());
            stats.put("status", "success");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logService.error("SlicingMonitoringController", "Failed to get queue stats: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getDetailedHealth() {
        logService.info("SlicingMonitoringController", "Getting detailed health information");

        try {
            Health health = queueProcessor.health();

            Map<String, Object> response = new HashMap<>();
            response.put("status", health.getStatus().getCode());
            response.put("details", health.getDetails());
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logService.error("SlicingMonitoringController", "Health check failed: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getQueueSummary() {
        logService.debug("SlicingMonitoringController", "Getting queue summary");

        try {
            long queued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            Map<String, Object> summary = new HashMap<>();
            summary.put("queued", queued);
            summary.put("processing", processing);
            summary.put("processor_enabled", queueProcessor.isProcessingEnabled());
            summary.put("health_status", determineHealthStatus(queued, processing));
            summary.put("timestamp", Instant.now());

            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            logService.error("SlicingMonitoringController", "Failed to get queue summary: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getProcessingMetrics() {
        logService.debug("SlicingMonitoringController", "Getting processing metrics");

        try {
            Map<String, Object> metrics = new HashMap<>();

            metrics.put("total_processed", queueProcessor.getProcessedCount());
            metrics.put("total_failed", queueProcessor.getFailedCount());
            metrics.put("currently_processing", queueProcessor.getCurrentlyProcessing());

            long totalProcessed = queueProcessor.getProcessedCount();
            long totalFailed = queueProcessor.getFailedCount();
            double successRate = calculateSuccessRate(totalProcessed, totalFailed);

            metrics.put("success_rate", successRate);
            metrics.put("failure_rate", 100.0 - successRate);

            metrics.put("last_processing_time", queueProcessor.getLastProcessingTime());
            metrics.put("timestamp", Instant.now());

            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            logService.error("SlicingMonitoringController", "Failed to get metrics: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/failed-jobs")
    public ResponseEntity<Map<String, Object>> getFailedJobsInfo() {
        logService.info("SlicingMonitoringController", "Getting failed jobs information");

        try {
            long failedCount = slicingQueueRepository.countByStatus(SlicingStatus.FAILED.getCode());

            Map<String, Object> response = new HashMap<>();
            response.put("failed_jobs_count", failedCount);
            response.put("recommendations", getFailureRecommendations(failedCount));
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logService.error("SlicingMonitoringController", "Failed to get failed jobs info: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private double calculateSuccessRate(long successful, long failed) {
        long total = successful + failed;
        if (total == 0) return 100.0;
        return (double) successful / total * 100.0;
    }

    private String determineHealthStatus(long queued, long processing) {
        if (processing == 0 && queued > 0) {
            return "warning";
        } else if (queued > 50) {
            return "critical";
        } else if (queued > 10) {
            return "warning";
        } else {
            return "healthy";
        }
    }

    private Map<String, Object> getFailureRecommendations(long failedCount) {
        Map<String, Object> recommendations = new HashMap<>();

        if (failedCount == 0) {
            recommendations.put("status", "good");
            recommendations.put("message", "No failed jobs detected");
        } else if (failedCount < 5) {
            recommendations.put("status", "monitor");
            recommendations.put("message", "Few failed jobs - monitor for patterns");
            recommendations.put("actions", new String[]{
                    "Check individual job error messages",
                    "Verify model file integrity"
            });
        } else {
            recommendations.put("status", "investigate");
            recommendations.put("message", "Multiple failed jobs detected");
            recommendations.put("actions", new String[]{
                    "Check CuraEngine service availability",
                    "Verify network connectivity",
                    "Review timeout configurations",
                    "Check disk space and memory"
            });
        }

        return recommendations;
    }
}