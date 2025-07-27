package it.aredegalli.printer.service.websocket;

import it.aredegalli.printer.dto.websocket.SlicingQueueUpdateDto;
import it.aredegalli.printer.dto.websocket.SystemHealthUpdateDto;
import it.aredegalli.printer.dto.websocket.WebSocketMessage;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.websocket.SlicingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending WebSocket notifications about slicing queue changes
 */
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SlicingWebSocketHandler webSocketHandler;
    private final SlicingQueueRepository slicingQueueRepository;
    private final LogService logService;

    /**
     * Notify about queue status change
     */
    @Async("slicingExecutor")
    public void notifyQueueStatusChange(UUID queueId, SlicingStatus newStatus, String message) {
        try {
            SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
            if (queue == null) {
                logService.warn("WebSocketNotificationService",
                        "Queue not found for notification: " + queueId);
                return;
            }

            SlicingQueueUpdateDto update = buildQueueUpdate(queue, message);

            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type(getMessageTypeForStatus(newStatus))
                    .userId(queue.getCreatedByUserId())
                    .payload(update)
                    .timestamp(Instant.now())
                    .build();

            // Send through WebSocket handler
            webSocketHandler.notifyQueueStatusChange(queueId, newStatus.getCode(), message,
                    queue.getProgressPercentage() != null ? queue.getProgressPercentage() : 0);

            logService.debug("WebSocketNotificationService",
                    String.format("Sent status change notification for queue %s: %s",
                            queueId, newStatus));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending queue status notification: " + e.getMessage());
        }
    }

    /**
     * Notify about progress update
     */
    @Async("slicingExecutor")
    public void notifyProgressUpdate(UUID queueId, int progress, String message) {
        try {
            webSocketHandler.broadcastProgressUpdate(queueId, progress, message);

            logService.debug("WebSocketNotificationService",
                    String.format("Sent progress update for queue %s: %d%% - %s",
                            queueId, progress, message));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending progress notification: " + e.getMessage());
        }
    }

    /**
     * Notify about job completion
     */
    @Async("slicingExecutor")
    public void notifyJobCompleted(UUID queueId, long processingTimeSeconds, long gcodeLines) {
        try {
            SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
            if (queue == null) return;

            String completionMessage = String.format(
                    "Slicing completed successfully in %d seconds. G-code: %d lines",
                    processingTimeSeconds, gcodeLines);

            SlicingQueueUpdateDto update = buildQueueUpdate(queue, completionMessage);

            WebSocketMessage wsMessage = WebSocketMessage.builder()
                    .type(WebSocketMessage.MessageType.QUEUE_COMPLETED)
                    .userId(queue.getCreatedByUserId())
                    .payload(update)
                    .timestamp(Instant.now())
                    .build();

            webSocketHandler.notifyQueueStatusChange(queueId, SlicingStatus.COMPLETED.getCode(),
                    completionMessage, 100);

            logService.info("WebSocketNotificationService",
                    String.format("Sent completion notification for queue %s", queueId));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending completion notification: " + e.getMessage());
        }
    }

    /**
     * Notify about job failure
     */
    @Async("slicingExecutor")
    public void notifyJobFailed(UUID queueId, String errorMessage, long processingTimeSeconds) {
        try {
            String failureMessage = String.format(
                    "Slicing failed after %d seconds: %s", processingTimeSeconds, errorMessage);

            webSocketHandler.notifyQueueStatusChange(queueId, SlicingStatus.FAILED.getCode(),
                    failureMessage, 0);

            logService.info("WebSocketNotificationService",
                    String.format("Sent failure notification for queue %s: %s", queueId, errorMessage));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending failure notification: " + e.getMessage());
        }
    }

    /**
     * Notify about job cancellation
     */
    @Async("slicingExecutor")
    public void notifyJobCancelled(UUID queueId, String reason) {
        try {
            String cancellationMessage = "Job cancelled" + (reason != null ? ": " + reason : "");

            webSocketHandler.broadcastQueueUpdate(queueId, "CANCELLED", cancellationMessage);

            logService.info("WebSocketNotificationService",
                    String.format("Sent cancellation notification for queue %s", queueId));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending cancellation notification: " + e.getMessage());
        }
    }

    /**
     * Notify about system health changes
     */
    @Async("slicingExecutor")
    public void notifySystemHealth(String status, int queued, int processing, int engines, String message) {
        try {
            SystemHealthUpdateDto healthUpdate = SystemHealthUpdateDto.builder()
                    .status(status)
                    .totalQueued(queued)
                    .totalProcessing(processing)
                    .availableEngines(engines)
                    .message(message)
                    .timestamp(Instant.now())
                    .build();

            // This would broadcast to all connected clients
            // Implementation depends on if you want system-wide notifications

            logService.debug("WebSocketNotificationService",
                    String.format("System health notification: %s (queued: %d, processing: %d)",
                            status, queued, processing));

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending system health notification: " + e.getMessage());
        }
    }

    /**
     * Send batch update for multiple queues (e.g., for user dashboard)
     */
    @Async("slicingExecutor")
    public void notifyUserQueueUpdate(String userId) {
        try {
            // This could send a comprehensive update of all user's queues
            logService.debug("WebSocketNotificationService",
                    "Sending user queue update for: " + userId);

            // Implementation would fetch all user queues and send update
            // This is useful for dashboard refreshes

        } catch (Exception e) {
            logService.error("WebSocketNotificationService",
                    "Error sending user queue update: " + e.getMessage());
        }
    }

    // ======================================
    // UTILITY METHODS
    // ======================================

    private SlicingQueueUpdateDto buildQueueUpdate(SlicingQueue queue, String message) {
        return SlicingQueueUpdateDto.builder()
                .queueId(queue.getId())
                .modelId(queue.getModel().getId())
                .modelName(queue.getModel().getName())
                .status(queue.getStatus())
                .progressPercentage(queue.getProgressPercentage())
                .message(message)
                .errorMessage(queue.getErrorMessage())
                .createdAt(queue.getCreatedAt())
                .startedAt(queue.getStartedAt())
                .completedAt(queue.getCompletedAt())
                .priority(queue.getPriority())
                .estimatedTimeRemainingSeconds(calculateEstimatedTime(queue))
                .build();
    }

    private Long calculateEstimatedTime(SlicingQueue queue) {
        if (queue.getStartedAt() == null || queue.getProgressPercentage() == null) {
            return null;
        }

        try {
            int progress = queue.getProgressPercentage();
            if (progress <= 0 || progress >= 100) {
                return null;
            }

            Duration elapsed = Duration.between(queue.getStartedAt(), Instant.now());
            long elapsedSeconds = elapsed.getSeconds();

            // Estimate remaining time based on current progress
            long estimatedTotal = (elapsedSeconds * 100) / progress;
            return estimatedTotal - elapsedSeconds;

        } catch (Exception e) {
            logService.debug("WebSocketNotificationService",
                    "Error calculating estimated time: " + e.getMessage());
            return null;
        }
    }

    private WebSocketMessage.MessageType getMessageTypeForStatus(SlicingStatus status) {
        switch (status) {
            case COMPLETED:
                return WebSocketMessage.MessageType.QUEUE_COMPLETED;
            case FAILED:
                return WebSocketMessage.MessageType.QUEUE_FAILED;
            case PROCESSING:
            case QUEUED:
                return WebSocketMessage.MessageType.QUEUE_STATUS_CHANGE;
            default:
                return WebSocketMessage.MessageType.QUEUE_UPDATE;
        }
    }

    // ======================================
    // INTEGRATION METHODS (called by other services)
    // ======================================

    /**
     * Helper method to be called from SlicingService when updating queue status
     */
    public void onQueueStatusChanged(SlicingQueue queue, SlicingStatus oldStatus, SlicingStatus newStatus, String message) {
        notifyQueueStatusChange(queue.getId(), newStatus, message);

        // Send specific notifications based on status transitions
        if (newStatus == SlicingStatus.PROCESSING && oldStatus == SlicingStatus.QUEUED) {
            notifyProgressUpdate(queue.getId(), 0, "Starting slicing process");
        }
    }

    /**
     * Helper method to be called from SlicingService when updating progress
     */
    public void onProgressUpdated(UUID queueId, int progress, String message) {
        notifyProgressUpdate(queueId, progress, message);
    }

    /**
     * Helper method to be called when job completes
     */
    public void onJobCompleted(UUID queueId, Duration processingTime, long gcodeLines) {
        notifyJobCompleted(queueId, processingTime.getSeconds(), gcodeLines);
    }

    /**
     * Helper method to be called when job fails
     */
    public void onJobFailed(UUID queueId, String errorMessage, Duration processingTime) {
        notifyJobFailed(queueId, errorMessage, processingTime.getSeconds());
    }
}