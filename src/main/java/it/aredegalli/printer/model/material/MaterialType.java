package it.aredegalli.printer.model.material;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "material_type", indexes = {
        @Index(name = "idx_material_type_name", columnList = "name"),
        @Index(name = "idx_material_type_active", columnList = "is_active")
})
public class MaterialType {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @Column(name = "name", nullable = false, length = 64, unique = true)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "typical_temp_min_c")
    private Integer typicalTempMinC;

    @Column(name = "typical_temp_max_c")
    private Integer typicalTempMaxC;

    @Column(name = "typical_bed_temp_c")
    private Integer typicalBedTempC;

    @Column(name = "is_flexible", nullable = false)
    @Builder.Default
    private Boolean isFlexible = false;

    @Column(name = "is_soluble", nullable = false)
    @Builder.Default
    private Boolean isSoluble = false;

    @Column(name = "requires_heated_bed", nullable = false)
    @Builder.Default
    private Boolean requiresHeatedBed = false;

    @Column(name = "requires_enclosure", nullable = false)
    @Builder.Default
    private Boolean requiresEnclosure = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relazione bidirezionale con Material
    @OneToMany(mappedBy = "type", fetch = FetchType.LAZY)
    private List<Material> materials;

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