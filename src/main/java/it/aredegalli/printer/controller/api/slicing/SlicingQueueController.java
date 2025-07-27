package it.aredegalli.printer.controller.api.slicing;

import it.aredegalli.printer.dto.slicing.queue.SlicingQueueCreateDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingRequestDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/slicing/queue")
@RequiredArgsConstructor
public class SlicingQueueController {

    private final SlicingService slicingService;
    private final SlicingQueueRepository slicingQueueRepository;
    private final LogService log;
    private final SlicingEngineSelector engineSelector;

    @GetMapping()
    public ResponseEntity<List<SlicingQueueDto>> getQueueByUserId(@RequestParam() String userId) {
        log.info("SlicingQueueController", "Getting queue for user id: " + userId);

        var queue = slicingService.getAllSlicingQueueByCreatedUserId(userId);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(queue);
    }

    @PostMapping()
    public ResponseEntity<Map<String, Object>> queueSlicing(@Valid @RequestBody SlicingQueueCreateDto request) {
        log.info("SlicingQueueController", "Queueing slicing request for model: " + request.getModelId());

        try {
            UUID queueId = slicingService.queueSlicing(
                    request.getModelId(),
                    request.getSlicingPropertyId(),
                    request.getUserId(),
                    request.getPriority()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("queueId", queueId);
            response.put("status", "queued");
            response.put("message", "Slicing request queued successfully");
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to queue slicing: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<SlicingQueueDto> getQueueStatus(@PathVariable UUID id) {
        log.info("SlicingQueueController", "Getting queue status for: " + id);

        var queue = slicingService.getQueueStatus(id);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(queue);
    }

    @PostMapping("/slice-now")
    public ResponseEntity<Map<String, Object>> sliceImmediately(@Valid @RequestBody SlicingRequestDto request) {
        log.info("SlicingQueueController", "Immediate slicing request for model: " + request.getModelId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Immediate slicing not yet implemented. Use queue endpoint instead.");
        response.put("timestamp", Instant.now());

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        log.info("SlicingQueueController", "Getting queue statistics");

        Map<String, Object> response = new HashMap<>();

        try {
            long totalQueued = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long totalProcessing = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());
            long totalCompleted = slicingQueueRepository.countByStatus(SlicingStatus.COMPLETED.getCode());
            long totalFailed = slicingQueueRepository.countByStatus(SlicingStatus.FAILED.getCode());

            response.put("total_queued", totalQueued);
            response.put("total_processing", totalProcessing);
            response.put("total_completed", totalCompleted);
            response.put("total_failed", totalFailed);
            response.put("total_all", totalQueued + totalProcessing + totalCompleted + totalFailed);

            // Calculate estimated wait time (simplified)
            double avgProcessingTimeMinutes = 5.0; // Rough estimate
            double estimatedWaitMinutes = totalQueued * avgProcessingTimeMinutes;
            response.put("estimated_wait_time_minutes", estimatedWaitMinutes);

            response.put("average_processing_time_minutes", avgProcessingTimeMinutes);
            response.put("status", "success");
            response.put("timestamp", Instant.now());

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to get queue stats: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("timestamp", Instant.now());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSlicingHealth() {
        log.info("SlicingQueueController", "Getting slicing system health");

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> engines = engineSelector.getAvailableEngines();
            boolean hasEngines = !engines.isEmpty();

            long queuedJobs = slicingQueueRepository.countByStatus(SlicingStatus.QUEUED.getCode());
            long processingJobs = slicingQueueRepository.countByStatus(SlicingStatus.PROCESSING.getCode());

            String healthStatus;
            if (!hasEngines) {
                healthStatus = "unhealthy";
            } else if (queuedJobs > 50) {
                healthStatus = "degraded";
            } else {
                healthStatus = "healthy";
            }

            response.put("status", healthStatus);
            response.put("available_engines", engines.size());
            response.put("engines", engines);
            response.put("queued_jobs", queuedJobs);
            response.put("processing_jobs", processingJobs);
            response.put("timestamp", Instant.now());

            if (!hasEngines) {
                response.put("error", "No slicing engines available");
            } else if (queuedJobs > 50) {
                response.put("warning", "High queue backlog: " + queuedJobs + " jobs");
            }

        } catch (Exception e) {
            log.error("SlicingQueueController", "Health check failed: " + e.getMessage());
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
            response.put("timestamp", Instant.now());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{queueId}")
    public ResponseEntity<Map<String, Object>> cancelSlicing(@PathVariable UUID queueId) {
        log.info("SlicingQueueController", "Cancelling slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();

        try {
            SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);

            if (queue == null) {
                response.put("status", "error");
                response.put("error", "Queue not found");
                return ResponseEntity.notFound().build();
            }

            String currentStatus = queue.getStatus();

            // Can only cancel queued jobs
            if (SlicingStatus.QUEUED.getCode().equals(currentStatus)) {
                queue.setStatus(SlicingStatus.FAILED.getCode());
                queue.setErrorMessage("Cancelled by user");
                queue.setCompletedAt(Instant.now());
                slicingQueueRepository.save(queue);

                response.put("status", "cancelled");
                response.put("message", "Slicing request cancelled successfully");
                log.info("SlicingQueueController", "Successfully cancelled queue: " + queueId);

            } else if (SlicingStatus.PROCESSING.getCode().equals(currentStatus)) {
                response.put("status", "error");
                response.put("error", "Cannot cancel job that is currently processing");

            } else {
                response.put("status", "error");
                response.put("error", "Job is already completed or failed");
            }

            response.put("queueId", queueId);
            response.put("previous_status", currentStatus);
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to cancel slicing: " + e.getMessage());

            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("queueId", queueId);
            response.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/retry/{queueId}")
    public ResponseEntity<Map<String, Object>> retrySlicing(@PathVariable UUID queueId) {
        log.info("SlicingQueueController", "Retrying slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();

        try {
            SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);

            if (queue == null) {
                response.put("status", "error");
                response.put("error", "Queue not found");
                return ResponseEntity.notFound().build();
            }

            String currentStatus = queue.getStatus();

            // Can only retry failed jobs
            if (SlicingStatus.FAILED.getCode().equals(currentStatus)) {
                queue.setStatus(SlicingStatus.QUEUED.getCode());
                queue.setErrorMessage(null);
                queue.setStartedAt(null);
                queue.setCompletedAt(null);
                queue.setProgressPercentage(0);
                // Increase priority slightly for retries
                queue.setPriority(Math.min(queue.getPriority() + 1, 10));
                slicingQueueRepository.save(queue);

                response.put("status", "queued");
                response.put("message", "Slicing request queued for retry");
                log.info("SlicingQueueController", "Successfully queued retry for queue: " + queueId);

            } else if (SlicingStatus.COMPLETED.getCode().equals(currentStatus)) {
                response.put("status", "error");
                response.put("error", "Cannot retry completed job");

            } else {
                response.put("status", "error");
                response.put("error", "Can only retry failed jobs");
            }

            response.put("queueId", queueId);
            response.put("previous_status", currentStatus);
            response.put("current_status", queue.getStatus());
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to retry slicing: " + e.getMessage());

            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("queueId", queueId);
            response.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Bulk operations for queue management
     */
    @PostMapping("/bulk/cancel")
    public ResponseEntity<Map<String, Object>> bulkCancelQueued(@RequestParam String userId) {
        log.info("SlicingQueueController", "Bulk cancelling queued jobs for user: " + userId);

        Map<String, Object> response = new HashMap<>();

        try {
            List<SlicingQueue> queuedJobs = slicingQueueRepository
                    .findByCreatedByUserId(userId)
                    .stream()
                    .filter(job -> SlicingStatus.QUEUED.getCode().equals(job.getStatus()))
                    .toList();

            int cancelledCount = 0;
            for (SlicingQueue job : queuedJobs) {
                job.setStatus(SlicingStatus.FAILED.getCode());
                job.setErrorMessage("Bulk cancelled by user");
                job.setCompletedAt(Instant.now());
                slicingQueueRepository.save(job);
                cancelledCount++;
            }

            response.put("status", "success");
            response.put("cancelled_count", cancelledCount);
            response.put("message", "Cancelled " + cancelledCount + " queued jobs");
            response.put("userId", userId);
            response.put("timestamp", Instant.now());

            log.info("SlicingQueueController", "Bulk cancelled " + cancelledCount + " jobs for user: " + userId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to bulk cancel: " + e.getMessage());

            response.put("status", "error");
            response.put("error", e.getMessage());
            response.put("userId", userId);
            response.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(response);
        }
    }
}