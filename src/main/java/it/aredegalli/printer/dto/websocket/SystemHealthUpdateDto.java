package it.aredegalli.printer.dto.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * System health update
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthUpdateDto {
    private String status; // healthy, degraded, unhealthy
    private int totalQueued;
    private int totalProcessing;
    private int availableEngines;
    private String message;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
}