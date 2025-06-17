package it.aredegalli.printer.dto.slicing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlicingResultDto {

    private UUID id;
    private UUID sourceId;
    private long lines;
    private SlicingProfileDto slicingProfile;
    private MaterialDto material;
    private Instant createdAt;

}