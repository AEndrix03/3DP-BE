package it.aredegalli.printer.model.slicing.property;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "material", indexes = {
        @Index(name = "idx_material_name", columnList = "name"),
        @Index(name = "idx_material_type", columnList = "type"),
        @Index(name = "idx_material_brand", columnList = "brand")
})
public class Material {

    // Changed to UUID for consistency with other entities
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "brand", length = 64)
    private String brand;

    @Column(name = "density_g_cm3", length = 16)
    private String densityGCm3;

    @Column(name = "diameter_mm", length = 16)
    private String diameterMm;

    @Column(name = "cost_per_kg", length = 16)
    private String costPerKg;

    @Column(name = "recommended_extruder_temp_min_c")
    private long recommendedExtruderTempMinC;

    @Column(name = "recommended_extruder_temp_max_c")
    private long recommendedExtruderTempMaxC;

    @Column(name = "recommended_bed_temp_c")
    private long recommendedBedTempC;

    @Column(name = "requires_heated_bed", length = 8)
    private String requiresHeatedBed;

    @Column(name = "requires_chamber_heating", length = 8)
    private String requiresChamberHeating;

    @Column(name = "supports_soluble", length = 8)
    private String supportsSoluble;

    @Column(name = "shrinkage_factor", length = 16)
    private String shrinkageFactor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "image")
    private String image;

    // Auto-set timestamps
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}