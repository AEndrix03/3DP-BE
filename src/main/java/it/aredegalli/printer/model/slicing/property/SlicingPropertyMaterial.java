package it.aredegalli.printer.model.slicing.property;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "slicing_property_material",
        indexes = {
                @Index(name = "idx_slicing_property_material_slicing_property", columnList = "slicing_property_id"),
                @Index(name = "idx_slicing_property_material_material", columnList = "material_id"),
                @Index(name = "idx_slicing_property_material_created_at", columnList = "created_at"),
                @Index(name = "idx_slicing_property_material_composite", columnList = "slicing_property_id, material_id"),
                @Index(name = "idx_slicing_property_material_recent", columnList = "created_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_slicing_property_material",
                        columnNames = {"slicing_property_id", "material_id"})
        })
public class SlicingPropertyMaterial {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "Slicing property ID is required")
    @Column(name = "slicing_property_id", nullable = false)
    private UUID slicingPropertyId;

    @NotNull(message = "Material ID is required")
    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    @NotNull(message = "Created at is required")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // === RELATIONSHIPS ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slicing_property_id", insertable = false, updatable = false)
    private SlicingProperty slicingProperty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private Material material;

    // === LIFECYCLE METHODS ===

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    // === STATIC FACTORY METHODS ===

    /**
     * Creates a new association between a slicing property and a material
     *
     * @param slicingPropertyId the UUID of the slicing property
     * @param materialId        the UUID of the material
     * @return a new SlicingPropertyMaterial instance
     */
    public static SlicingPropertyMaterial create(UUID slicingPropertyId, UUID materialId) {
        return SlicingPropertyMaterial.builder()
                .slicingPropertyId(slicingPropertyId)
                .materialId(materialId)
                .createdAt(Instant.now())
                .build();
    }

    /**
     * Creates a new association between a slicing property and a material with entities
     *
     * @param slicingProperty the slicing property entity
     * @param material        the material entity
     * @return a new SlicingPropertyMaterial instance
     */
    public static SlicingPropertyMaterial create(SlicingProperty slicingProperty, Material material) {
        return SlicingPropertyMaterial.builder()
                .slicingPropertyId(slicingProperty.getId())
                .materialId(material.getId())
                .slicingProperty(slicingProperty)
                .material(material)
                .createdAt(Instant.now())
                .build();
    }

    // === BUSINESS METHODS ===

    /**
     * Checks if this association is valid
     *
     * @return true if both IDs are not null
     */
    public boolean isValid() {
        return slicingPropertyId != null && materialId != null;
    }

    /**
     * Returns a string representation of this association
     *
     * @return formatted string with property and material info
     */
    public String getAssociationSummary() {
        String propertyName = slicingProperty != null ? slicingProperty.getName() : slicingPropertyId.toString();
        String materialName = material != null ? material.getName() : materialId.toString();

        return String.format("Property: %s -> Material: %s (created: %s)",
                propertyName, materialName, createdAt);
    }

    // === UTILITY METHODS ===

    /**
     * Compares two associations for equality based on property and material IDs
     *
     * @param other the other association to compare
     * @return true if both have the same property and material IDs
     */
    public boolean hasSameAssociation(SlicingPropertyMaterial other) {
        if (other == null) {
            return false;
        }

        return this.slicingPropertyId.equals(other.slicingPropertyId) &&
                this.materialId.equals(other.materialId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlicingPropertyMaterial that)) return false;

        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return String.format("SlicingPropertyMaterial{id=%s, slicingPropertyId=%s, materialId=%s, createdAt=%s}",
                id, slicingPropertyId, materialId, createdAt);
    }
}