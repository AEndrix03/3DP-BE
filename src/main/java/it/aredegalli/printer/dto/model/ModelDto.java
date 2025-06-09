package it.aredegalli.printer.dto.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelDto {

    private UUID id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    private UUID resourceId;

}
