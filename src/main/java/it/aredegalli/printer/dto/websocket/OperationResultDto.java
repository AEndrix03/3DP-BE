package it.aredegalli.printer.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Operation result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResultDto {
    private boolean success;
    private String message;
    private String operationType;
    private UUID targetId;
    private Map<String, Object> metadata;
}