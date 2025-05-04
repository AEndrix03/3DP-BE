package it.aredegalli.printer.dto.slicing;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class SlicingResultDto {

    private UUID id;
    private UUID sourceId;
    private Map<String, Object> parameters;
    private String logs;
    private Instant createdAt;

}
