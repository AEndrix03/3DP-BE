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
@Table(name = "material_brand", indexes = {
        @Index(name = "idx_material_brand_name", columnList = "name"),
        @Index(name = "idx_material_brand_active", columnList = "is_active"),
        @Index(name = "idx_material_brand_rating", columnList = "quality_rating")
})
public class MaterialBrand {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", nullable = false, updatable = false)
    private java.util.UUID id;

    @Column(name = "name", nullable = false, length = 128, unique = true)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "website")
    private String website;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "quality_rating")
    private Integer qualityRating;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Relazione bidirezionale con Material
    @OneToMany(mappedBy = "brand", fetch = FetchType.LAZY)
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