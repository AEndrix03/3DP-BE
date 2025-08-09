package it.aredegalli.printer.model.material;

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
@Table(name = "material",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_material_brand_name_diameter",
                        columnNames = {"brand_id", "name", "diameter_mm"})
        },
        indexes = {
                @Index(name = "idx_material_name", columnList = "name"),
                @Index(name = "idx_material_type_id", columnList = "type_id"),
                @Index(name = "idx_material_brand_id", columnList = "brand_id"),
                @Index(name = "idx_material_type_brand", columnList = "type_id, brand_id")
        })
public class Material {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", referencedColumnName = "id")
    private MaterialType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", referencedColumnName = "id")
    private MaterialBrand brand;

    @Column(name = "density_g_cm3", length = 16)
    private String densityGCm3;

    @Column(name = "diameter_mm", length = 16)
    private String diameterMm;

    @Column(name = "cost_per_kg", length = 16)
    private String costPerKg;

    @Column(name = "recommended_extruder_temp_min_c")
    private Long recommendedExtruderTempMinC;

    @Column(name = "recommended_extruder_temp_max_c")
    private Long recommendedExtruderTempMaxC;

    @Column(name = "recommended_bed_temp_c")
    private Long recommendedBedTempC;

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