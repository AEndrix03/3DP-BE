package it.aredegalli.printer.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Job cancellation request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobCancellationRequestDto {
    private UUID queueId;
    private String reason;
}