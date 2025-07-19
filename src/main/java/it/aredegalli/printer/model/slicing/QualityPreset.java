package it.aredegalli.printer.model.slicing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "quality_preset")
public class QualityPreset {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(max = 64)
    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "layer_height_mm", precision = 4, scale = 3)
    private BigDecimal layerHeightMm;

    @Column(name = "print_speed_factor", precision = 3, scale = 2)
    private BigDecimal printSpeedFactor;

    @Column(name = "quality_level")
    private Integer qualityLevel;

    @Column(name = "estimated_time_factor", precision = 3, scale = 2)
    private BigDecimal estimatedTimeFactor;

}