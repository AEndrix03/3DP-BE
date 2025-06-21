package it.aredegalli.printer.dto.slicing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlicingResultDto {

    private UUID id;
    private UUID sourceId;
    private UUID generatedId;
    private long lines;
    private List<MaterialDto> materials;
    private SlicingPropertyDto slicingProperty;
    private Instant createdAt;

}