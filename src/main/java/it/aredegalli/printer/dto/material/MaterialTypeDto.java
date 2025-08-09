package it.aredegalli.printer.dto.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialTypeDto {

    private String id;
    private String name;
    private String description;
    private Integer typicalTempMinC;
    private Integer typicalTempMaxC;
    private Integer typicalBedTempC;
    private Boolean isFlexible;
    private Boolean isSoluble;
    private Boolean requiresHeatedBed;
    private Boolean requiresEnclosure;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

}