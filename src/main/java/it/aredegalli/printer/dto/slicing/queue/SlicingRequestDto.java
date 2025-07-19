package it.aredegalli.printer.dto.slicing.queue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SlicingRequestDto {

    @NotNull
    private UUID modelId;

    @NotNull
    private String slicingPropertyId;
}