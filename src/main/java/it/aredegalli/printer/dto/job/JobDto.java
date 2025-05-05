package it.aredegalli.printer.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {

    private UUID id;
    private UUID printerId;
    private UUID resourceId;
    private String statusCode;
    private Long progress;
    private Long totalLines;
    private Long startOffsetLine;

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

}
