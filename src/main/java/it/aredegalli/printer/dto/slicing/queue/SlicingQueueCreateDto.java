package it.aredegalli.printer.dto.slicing.queue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SlicingQueueCreateDto {

    @NotNull(message = "Model ID is required")
    private UUID modelId;

    @NotNull(message = "Slicing property ID is required")
    private UUID slicingPropertyId;

    @NotNull(message = "User ID is required")
    private String userId;

    @Min(value = 1, message = "Priority must be between 1 and 10")
    @Max(value = 10, message = "Priority must be between 1 and 10")
    private Integer priority = 5;
}