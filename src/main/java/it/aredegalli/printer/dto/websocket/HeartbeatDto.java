package it.aredegalli.printer.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Heartbeat message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatDto {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant serverTime;
    private String status;
    private long connectionId;
}