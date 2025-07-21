package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueCreateDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingRequestDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngine;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/slicing")
@RequiredArgsConstructor
public class SlicingController {

    private final SlicingService slicingService;
    private final LogService log;

    // Enhanced dependencies for engine management
    private final SlicingEngineSelector engineSelector;

    // ======================================
    // EXISTING ENDPOINTS (Enhanced)
    // ======================================

    @GetMapping()
    public ResponseEntity<SlicingResultDto> getSlicingResultById(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "getSlicingResultById with id: " + id);
        return ResponseEntity.ok(slicingService.getSlicingResultById(id));
    }

    @GetMapping("/source")
    public ResponseEntity<List<SlicingResultDto>> getAllSlicingResultBySourceId(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "getAllSlicingResultBySourceId with source id: " + id);
        return ResponseEntity.ok(slicingService.getAllSlicingResultBySourceId(id));
    }

    @DeleteMapping()
    public ResponseEntity<Void> deleteSlicingResultById(@RequestParam("id") @NotNull UUID id) {
        log.info("SlicingController", "deleteSlicingResultById with id: " + id);
        slicingService.deleteSlicingResultById(id);
        return ResponseEntity.noContent().build();
    }

    // ======================================
    // QUEUE MANAGEMENT ENDPOINTS
    // ======================================

    @PostMapping("/queue")
    public ResponseEntity<Map<String, Object>> queueSlicing(@Valid @RequestBody SlicingQueueCreateDto request) {
        log.info("SlicingController", "Queueing slicing request for model: " + request.getModelId());

        try {
            UUID queueId = slicingService.queueSlicing(
                    request.getModelId(),
                    request.getSlicingPropertyId(),
                    request.getPriority()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("queueId", queueId);
            response.put("status", "queued");
            response.put("message", "Slicing request queued successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("SlicingController", "Failed to queue slicing: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/queue/{id}")
    public ResponseEntity<SlicingQueueDto> getQueueStatus(@PathVariable UUID id) {
        log.info("SlicingController", "Getting queue status for: " + id);

        var queue = slicingService.getQueueStatus(id);
        if (queue == null) {
            return ResponseEntity.notFound().build();
        }

        SlicingQueueDto dto = SlicingQueueDto.builder()
                .id(queue.getId())
                .modelId(queue.getModel().getId())
                .slicingPropertyId(queue.getSlicingProperty().getId())
                .priority(queue.getPriority())
                .status(SlicingStatus.valueOf(queue.getStatus()))
                .createdAt(queue.getCreatedAt())
                .startedAt(queue.getStartedAt())
                .completedAt(queue.getCompletedAt())
                .errorMessage(queue.getErrorMessage())
                .progressPercentage(queue.getProgressPercentage())
                .build();

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/slice-now")
    public ResponseEntity<Map<String, Object>> sliceImmediately(@Valid @RequestBody SlicingRequestDto request) {
        log.info("SlicingController", "Immediate slicing request for model: " + request.getModelId());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Immediate slicing not yet implemented. Use queue endpoint instead.");

        return ResponseEntity.accepted().body(response);
    }

    // ======================================
    // NEW ENGINE MANAGEMENT ENDPOINTS
    // ======================================

    @GetMapping("/engines")
    public ResponseEntity<Map<String, Object>> getAvailableEngines() {
        log.info("SlicingController", "Getting available slicing engines");

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, String> engines = engineSelector.getAvailableEngines();
            SlicingEngine defaultEngine = engineSelector.getDefaultEngine();

            response.put("available_engines", engines);
            response.put("default_engine", defaultEngine.getName());
            response.put("default_version", defaultEngine.getVersion());
            response.put("total_count", engines.size());
            response.put("status", "success");

        } catch (Exception e) {
            log.error("SlicingController", "Failed to get engines: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/engines/{engineName}")
    public ResponseEntity<Map<String, Object>> getEngineInfo(@PathVariable String engineName) {
        log.info("SlicingController", "Getting engine info for: " + engineName);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean available = engineSelector.isEngineAvailable(engineName);

            if (available) {
                SlicingEngine engine = engineSelector.getEngine(engineName);
                response.put("name", engine.getName());
                response.put("version", engine.getVersion());
                response.put("available", true);
                response.put("status", "success");
            } else {
                response.put("name", engineName);
                response.put("available", false);
                response.put("status", "not_found");
                response.put("message", "Engine not available");
            }

        } catch (Exception e) {
            log.error("SlicingController", "Failed to get engine info: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/engines/test")
    public ResponseEntity<Map<String, Object>> testEngine(
            @RequestParam("engineName") String engineName,
            @RequestParam("modelId") UUID modelId) {

        log.info("SlicingController", "Testing engine: " + engineName + " with model: " + modelId);

        Map<String, Object> response = new HashMap<>();

        try {
            SlicingEngine engine = engineSelector.getEngine(engineName);

            // This would require a method to get Model by ID
            // For now, just test engine availability
            boolean available = engineSelector.isEngineAvailable(engineName);

            response.put("engine", engine.getName());
            response.put("version", engine.getVersion());
            response.put("available", available);
            response.put("test_result", available ? "passed" : "failed");
            response.put("model_id", modelId);
            response.put("status", "success");

        } catch (Exception e) {
            log.error("SlicingController", "Engine test failed: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ======================================
    // QUEUE STATISTICS ENDPOINTS
    // ======================================

    @GetMapping("/queue/stats")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        log.info("SlicingController", "Getting queue statistics");

        Map<String, Object> response = new HashMap<>();

        try {
            // This would require additional repository methods to get queue stats
            // For now, return basic structure
            response.put("total_queued", 0);
            response.put("total_processing", 0);
            response.put("total_completed", 0);
            response.put("total_failed", 0);
            response.put("average_wait_time_minutes", 0);
            response.put("average_processing_time_minutes", 0);
            response.put("status", "success");

        } catch (Exception e) {
            log.error("SlicingController", "Failed to get queue stats: " + e.getMessage());
            response.put("status", "error");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ======================================
    // HEALTH CHECK ENDPOINTS
    // ======================================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSlicingHealth() {
        log.info("SlicingController", "Getting slicing system health");

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
            log.error("SlicingController", "Health check failed: " + e.getMessage());
            response.put("status", "unhealthy");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    // ======================================
    // UTILITY ENDPOINTS
    // ======================================

    @PostMapping("/cancel/{queueId}")
    public ResponseEntity<Map<String, Object>> cancelSlicing(@PathVariable UUID queueId) {
        log.info("SlicingController", "Cancelling slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Slicing cancellation not yet implemented");
        response.put("queueId", queueId);

        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/retry/{queueId}")
    public ResponseEntity<Map<String, Object>> retrySlicing(@PathVariable UUID queueId) {
        log.info("SlicingController", "Retrying slicing for queue: " + queueId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "not_implemented");
        response.put("message", "Slicing retry not yet implemented");
        response.put("queueId", queueId);

        return ResponseEntity.accepted().body(response);
    }
}