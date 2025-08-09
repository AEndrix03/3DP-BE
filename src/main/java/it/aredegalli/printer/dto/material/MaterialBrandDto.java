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
public class MaterialBrandDto {

    private String id;
    private String name;
    private String description;
    private String website;
    private String country;
    private Integer qualityRating;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

}