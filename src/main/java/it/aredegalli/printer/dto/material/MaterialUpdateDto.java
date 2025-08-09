package it.aredegalli.printer.dto.material;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialUpdateDto {
    private String id;
    private String name;

    private String type;
    private String brand;

    private String densityGCm3;
    private String diameterMm;
    private String costPerKg;
    private Long recommendedExtruderTempMinC;
    private Long recommendedExtruderTempMaxC;
    private Long recommendedBedTempC;
    private String requiresHeatedBed;
    private String requiresChamberHeating;
    private String supportsSoluble;
    private String shrinkageFactor;
    private String image;
}