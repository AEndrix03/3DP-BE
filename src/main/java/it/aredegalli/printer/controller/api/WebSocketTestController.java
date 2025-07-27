package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.SlicingService;
import it.aredegalli.printer.service.websocket.WebSocketNotificationService;
import it.aredegalli.printer.websocket.SlicingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for WebSocket testing and integration
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
public class WebSocketTestController {

    private final SlicingWebSocketHandler webSocketHandler;
    private final WebSocketNotificationService notificationService;
    private final SlicingService slicingService;
    private final LogService logService;

    /**
     * Get WebSocket connection info and statistics
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getWebSocketInfo() {
        logService.info("WebSocketTestController", "Getting WebSocket info");

        Map<String, Object> info = new HashMap<>();
        info.put("endpoint", "/ws/slicing");
        info.put("status", "active");
        info.put("timestamp", Instant.now());
        info.put("features", Map.of(
                "real_time_updates", true,
                "job_cancellation", true,
                "progress_tracking", true,
                "heartbeat", true
        ));

        return ResponseEntity.ok(info);
    }

    /**
     * Test WebSocket notification (for development/testing)
     */
    @PostMapping("/test/notify/{queueId}")
    public ResponseEntity<Map<String, Object>> testNotification(
            @PathVariable UUID queueId,
            @RequestParam(required = false, defaultValue = "Test notification") String message,
            @RequestParam(required = false, defaultValue = "50") int progress) {

        logService.info("WebSocketTestController",
                String.format("Sending test notification for queue: %s", queueId));

        try {
            // Send test progress notification
            notificationService.notifyProgressUpdate(queueId, progress, message);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Test notification sent");
            response.put("queueId", queueId);
            response.put("progress", progress);
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logService.error("WebSocketTestController", "Test notification failed: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger job cancellation (for testing)
     */
    @PostMapping("/test/cancel/{queueId}")
    public ResponseEntity<Map<String, Object>> testJobCancellation(
            @PathVariable UUID queueId,
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "Test cancellation") String reason) {

        logService.info("WebSocketTestController",
                String.format("Testing job cancellation for queue: %s, user: %s", queueId, userId));

        try {
            boolean cancelled = slicingService.cancelSlicingJob(queueId, userId, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("status", cancelled ? "success" : "failed");
            response.put("message", cancelled ? "Job cancelled successfully" : "Job cancellation failed");
            response.put("queueId", queueId);
            response.put("userId", userId);
            response.put("reason", reason);
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logService.error("WebSocketTestController", "Test cancellation failed: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get WebSocket connection statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getWebSocketStats() {
        logService.debug("WebSocketTestController", "Getting WebSocket statistics");

        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("endpoint", "/ws/slicing");
            stats.put("active_connections", "Not implemented"); // Would need to track in handler
            stats.put("supported_message_types", new String[]{
                    "QUEUE_UPDATE", "QUEUE_STATUS_CHANGE", "QUEUE_PROGRESS_UPDATE",
                    "QUEUE_COMPLETED", "QUEUE_FAILED", "SUBSCRIBE_QUEUE",
                    "UNSUBSCRIBE_QUEUE", "CANCEL_JOB", "HEARTBEAT"
            });
            stats.put("timestamp", Instant.now());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logService.error("WebSocketTestController", "Stats retrieval failed: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Broadcast system health update (admin function)
     */
    @PostMapping("/admin/broadcast-health")
    public ResponseEntity<Map<String, Object>> broadcastSystemHealth(
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "System health update") String message) {

        logService.info("WebSocketTestController",
                String.format("Broadcasting system health: %s", status));

        try {
            // This would typically be called automatically by a monitoring service
            notificationService.notifySystemHealth(status, 0, 0, 1, message);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Health update broadcasted");
            response.put("health_status", status);
            response.put("timestamp", Instant.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logService.error("WebSocketTestController", "Health broadcast failed: " + e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check for WebSocket system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getWebSocketHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            health.put("status", "healthy");
            health.put("websocket_endpoint", "/ws/slicing");
            health.put("features_enabled", true);
            health.put("timestamp", Instant.now());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("timestamp", Instant.now());

            return ResponseEntity.internalServerError().body(health);
        }
    }
}