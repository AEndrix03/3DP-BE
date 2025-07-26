package it.aredegalli.printer.controller.api.slicing;

import it.aredegalli.printer.dto.slicing.queue.SlicingQueueCreateDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingRequestDto;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/slicing/queue")
@RequiredArgsConstructor
public class SlicingQueueController {

    private final SlicingService slicingService;
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

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to queue slicing: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());

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

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        log.info("SlicingQueueController", "Getting queue statistics");

        Map<String, Object> response = new HashMap<>();

        try {
            response.put("total_queued", 0);
            response.put("total_processing", 0);
            response.put("total_completed", 0);
            response.put("total_failed", 0);
            response.put("average_wait_time_minutes", 0);
            response.put("average_processing_time_minutes", 0);
            response.put("status", "success");

        } catch (Exception e) {
            log.error("SlicingQueueController", "Failed to get queue stats: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
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

            response.put("status", hasEngines ? "healthy" : "degraded");
            response.put("available_engines", engines.size());
            response.put("engines", engines);
            response.put("timestamp", java.time.Instant.now());

            if (!hasEngines) {
                response.put("warning", "No slicing engines available");
            }

        } catch (Exception e) {
            log.error("SlicingQueueController", "Health check failed: " + e.getMessage());
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancel/{queueId}")
    public ResponseEntity<Map<String, Object>> cancelSlicing(@PathVariable UUID queueId) {
        log.info("SlicingQueueController", "Cancelling slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Slicing cancellation not yet implemented");
        response.put("queueId", queueId);

        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/retry/{queueId}")
    public ResponseEntity<Map<String, Object>> retrySlicing(@PathVariable UUID queueId) {
        log.info("SlicingQueueController", "Retrying slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Slicing retry not yet implemented");
        response.put("queueId", queueId);

        return ResponseEntity.accepted().body(response);
    }
}