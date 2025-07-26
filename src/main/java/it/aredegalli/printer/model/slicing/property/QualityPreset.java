package it.aredegalli.printer.model.slicing.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quality_preset")
public class QualityPreset {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 64)
    @Column(name = "name", length = 64)
    private String name;

    @Digits(integer = 1, fraction = 3)
    @Column(name = "layer_height_mm", columnDefinition = "DECIMAL(4,3)")
    private BigDecimal layerHeightMm;

    @Digits(integer = 1, fraction = 2)
    @Column(name = "print_speed_factor", columnDefinition = "DECIMAL(3,2)")
    private BigDecimal printSpeedFactor;

    @Column(name = "quality_level")
    private Integer qualityLevel;

    @Digits(integer = 1, fraction = 2)
    @Column(name = "estimated_time_factor", columnDefinition = "DECIMAL(3,2)")
    private BigDecimal estimatedTimeFactor;
}