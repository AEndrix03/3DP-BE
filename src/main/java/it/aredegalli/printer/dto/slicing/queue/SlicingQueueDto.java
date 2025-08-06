package it.aredegalli.printer.dto.slicing.queue;

import it.aredegalli.printer.enums.slicing.SlicingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlicingQueueDto {
    private UUID id;
    private UUID modelId;
    private UUID slicingPropertyId;
    private Integer priority;
    private SlicingStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private String createdByUserId;

    private Long estimatedTimeRemaining;
    private String currentStep;
}