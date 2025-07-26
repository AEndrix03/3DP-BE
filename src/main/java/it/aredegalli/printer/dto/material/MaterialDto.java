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
public class MaterialDto {

    private String id;
    private String name;
    private String type;
    private String brand;
    private String densityGCm3;
    private String diameterMm;
    private String costPerKg;
    private long recommendedExtruderTempMinC;
    private long recommendedExtruderTempMaxC;
    private long recommendedBedTempC;
    private String requiresHeatedBed;
    private String requiresChamberHeating;
    private String supportsSoluble;
    private String shrinkageFactor;
    private Instant createdAt;
    private Instant updatedAt;
    private String image;

}