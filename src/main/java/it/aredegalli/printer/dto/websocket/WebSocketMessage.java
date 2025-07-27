package it.aredegalli.printer.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Base WebSocket message structure
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private MessageType type;
    private String userId;
    private Object payload;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    public enum MessageType {
        // Slicing queue updates
        QUEUE_UPDATE,
        QUEUE_STATUS_CHANGE,
        QUEUE_PROGRESS_UPDATE,
        QUEUE_COMPLETED,
        QUEUE_FAILED,

        // System messages  
        HEARTBEAT,
        CONNECT_CONFIRM,
        ERROR,

        // Control messages (from client)
        SUBSCRIBE_QUEUE,
        UNSUBSCRIBE_QUEUE,
        CANCEL_JOB,

        // Responses
        OPERATION_SUCCESS,
        OPERATION_FAILED
    }
}










