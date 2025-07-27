package it.aredegalli.printer.model.slicing.property;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "slicing_property",
        indexes = {
                @Index(name = "idx_slicing_property_name", columnList = "name"),
                @Index(name = "idx_slicing_property_user", columnList = "created_by_user_id, is_active"),
                @Index(name = "idx_slicing_property_public", columnList = "is_public, is_active"),
                @Index(name = "idx_slicing_property_slicer", columnList = "slicer_id, is_active"),
                @Index(name = "idx_slicing_property_quality", columnList = "quality_profile, is_active")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_slicing_property_name_user",
                        columnNames = {"name", "created_by_user_id", "is_active"})
        })
public class SlicingProperty {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 128, message = "Name must not exceed 128 characters")
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Size(max = 512, message = "Description must not exceed 512 characters")
    @Column(name = "description", length = 512)
    private String description;

    // === LAYER SETTINGS ===
    @NotNull(message = "Layer height is required")
    @DecimalMin(value = "0.001", message = "Layer height must be greater than 0")
    @DecimalMax(value = "2.0", message = "Layer height must not exceed 2.0mm")
    @Column(name = "layer_height_mm", nullable = false, precision = 6, scale = 3)
    private BigDecimal layerHeightMm;

    @DecimalMin(value = "0.001", message = "First layer height must be greater than 0")
    @DecimalMax(value = "2.0", message = "First layer height must not exceed 2.0mm")
    @Column(name = "first_layer_height_mm", precision = 6, scale = 3)
    private BigDecimal firstLayerHeightMm;

    @DecimalMin(value = "0.001", message = "Line width must be greater than 0")
    @DecimalMax(value = "5.0", message = "Line width must not exceed 5.0mm")
    @Column(name = "line_width_mm", precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal lineWidthMm = BigDecimal.valueOf(0.400);

    // === SPEED SETTINGS (mm/s) ===
    @NotNull(message = "Print speed is required")
    @DecimalMin(value = "0.1", message = "Print speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Print speed must not exceed 1000 mm/s")
    @Column(name = "print_speed_mm_s", nullable = false, precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal printSpeedMmS = BigDecimal.valueOf(50.0);

    @DecimalMin(value = "0.1", message = "First layer speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "First layer speed must not exceed 1000 mm/s")
    @Column(name = "first_layer_speed_mm_s", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal firstLayerSpeedMmS = BigDecimal.valueOf(20.0);

    @DecimalMin(value = "0.1", message = "Travel speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Travel speed must not exceed 1000 mm/s")
    @Column(name = "travel_speed_mm_s", precision = 7, scale = 2)
    @Builder.Default
    private BigDecimal travelSpeedMmS = BigDecimal.valueOf(150.0);

    @DecimalMin(value = "0.1", message = "Infill speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Infill speed must not exceed 1000 mm/s")
    @Column(name = "infill_speed_mm_s", precision = 7, scale = 2)
    private BigDecimal infillSpeedMmS;

    @DecimalMin(value = "0.1", message = "Outer wall speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Outer wall speed must not exceed 1000 mm/s")
    @Column(name = "outer_wall_speed_mm_s", precision = 7, scale = 2)
    private BigDecimal outerWallSpeedMmS;

    @DecimalMin(value = "0.1", message = "Inner wall speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Inner wall speed must not exceed 1000 mm/s")
    @Column(name = "inner_wall_speed_mm_s", precision = 7, scale = 2)
    private BigDecimal innerWallSpeedMmS;

    @DecimalMin(value = "0.1", message = "Top/bottom speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Top/bottom speed must not exceed 1000 mm/s")
    @Column(name = "top_bottom_speed_mm_s", precision = 7, scale = 2)
    private BigDecimal topBottomSpeedMmS;

    // === INFILL SETTINGS ===
    @NotNull(message = "Infill percentage is required")
    @DecimalMin(value = "0", message = "Infill percentage must be between 0 and 100")
    @DecimalMax(value = "100", message = "Infill percentage must be between 0 and 100")
    @Column(name = "infill_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal infillPercentage = BigDecimal.valueOf(20.0);

    @NotNull(message = "Infill pattern is required")
    @Pattern(regexp = "^(grid|lines|triangles|cubic|gyroid|honeycomb|concentric)$",
            message = "Invalid infill pattern")
    @Column(name = "infill_pattern", nullable = false, length = 32)
    @Builder.Default
    private String infillPattern = "grid";

    // === SHELL SETTINGS ===
    @Min(value = 0, message = "Perimeter count must be non-negative")
    @Max(value = 10, message = "Perimeter count must not exceed 10")
    @Column(name = "perimeter_count", nullable = false)
    @Builder.Default
    private Integer perimeterCount = 2;

    @Min(value = 0, message = "Top solid layers must be non-negative")
    @Max(value = 20, message = "Top solid layers must not exceed 20")
    @Column(name = "top_solid_layers", nullable = false)
    @Builder.Default
    private Integer topSolidLayers = 3;

    @Min(value = 0, message = "Bottom solid layers must be non-negative")
    @Max(value = 20, message = "Bottom solid layers must not exceed 20")
    @Column(name = "bottom_solid_layers", nullable = false)
    @Builder.Default
    private Integer bottomSolidLayers = 3;

    @DecimalMin(value = "0", message = "Top/bottom thickness must be non-negative")
    @DecimalMax(value = "10.0", message = "Top/bottom thickness must not exceed 10mm")
    @Column(name = "top_bottom_thickness_mm", precision = 6, scale = 3)
    private BigDecimal topBottomThicknessMm;

    // === SUPPORT SETTINGS ===
    @NotNull(message = "Supports enabled flag is required")
    @Column(name = "supports_enabled", nullable = false)
    @Builder.Default
    private Boolean supportsEnabled = false;

    @DecimalMin(value = "0", message = "Support angle must be between 0 and 90")
    @DecimalMax(value = "90", message = "Support angle must be between 0 and 90")
    @Column(name = "support_angle_threshold", precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal supportAngleThreshold = BigDecimal.valueOf(45.0);

    @DecimalMin(value = "0", message = "Support density must be between 0 and 100")
    @DecimalMax(value = "100", message = "Support density must be between 0 and 100")
    @Column(name = "support_density_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal supportDensityPercentage = BigDecimal.valueOf(20.0);

    @Pattern(regexp = "^(grid|lines|zigzag|triangles)$", message = "Invalid support pattern")
    @Column(name = "support_pattern", length = 32)
    @Builder.Default
    private String supportPattern = "grid";

    @DecimalMin(value = "0", message = "Support Z distance must be non-negative")
    @DecimalMax(value = "5.0", message = "Support Z distance must not exceed 5mm")
    @Column(name = "support_z_distance_mm", precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal supportZDistanceMm = BigDecimal.valueOf(0.2);

    // === ADHESION SETTINGS ===
    @Pattern(regexp = "^(none|brim|raft|skirt)$", message = "Invalid adhesion type")
    @Column(name = "adhesion_type", length = 32)
    @Builder.Default
    private String adhesionType = "none";

    @NotNull(message = "Brim enabled flag is required")
    @Column(name = "brim_enabled", nullable = false)
    @Builder.Default
    private Boolean brimEnabled = false;

    @DecimalMin(value = "0", message = "Brim width must be non-negative")
    @DecimalMax(value = "100.0", message = "Brim width must not exceed 100mm")
    @Column(name = "brim_width_mm", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal brimWidthMm = BigDecimal.valueOf(5.0);

    // === COOLING SETTINGS ===
    @NotNull(message = "Fan enabled flag is required")
    @Column(name = "fan_enabled", nullable = false)
    @Builder.Default
    private Boolean fanEnabled = true;

    @DecimalMin(value = "0", message = "Fan speed must be between 0 and 100")
    @DecimalMax(value = "100", message = "Fan speed must be between 0 and 100")
    @Column(name = "fan_speed_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal fanSpeedPercentage = BigDecimal.valueOf(100.0);

    // === RETRACTION SETTINGS ===
    @NotNull(message = "Retraction enabled flag is required")
    @Column(name = "retraction_enabled", nullable = false)
    @Builder.Default
    private Boolean retractionEnabled = true;

    @DecimalMin(value = "0", message = "Retraction distance must be non-negative")
    @DecimalMax(value = "10.0", message = "Retraction distance must not exceed 10mm")
    @Column(name = "retraction_distance_mm", precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal retractionDistanceMm = BigDecimal.valueOf(1.0);

    @NotNull(message = "Z-hop enabled flag is required")
    @Column(name = "zhop_enabled", nullable = false)
    @Builder.Default
    private Boolean zhopEnabled = false;

    @DecimalMin(value = "0", message = "Z-hop height must be non-negative")
    @DecimalMax(value = "5.0", message = "Z-hop height must not exceed 5mm")
    @Column(name = "zhop_height_mm", precision = 6, scale = 3)
    @Builder.Default
    private BigDecimal zhopHeightMm = BigDecimal.valueOf(0.2);

    // === TEMPERATURE SETTINGS (Optional - can be overridden by material) ===
    @Min(value = 150, message = "Extruder temperature must be at least 150°C")
    @Max(value = 350, message = "Extruder temperature must not exceed 350°C")
    @Column(name = "extruder_temp_c")
    private Integer extruderTempC;

    @Min(value = 0, message = "Bed temperature must be non-negative")
    @Max(value = 150, message = "Bed temperature must not exceed 150°C")
    @Column(name = "bed_temp_c")
    private Integer bedTempC;

    // === QUALITY PROFILE ===
    @Pattern(regexp = "^(draft|standard|high|ultra)$", message = "Invalid quality profile")
    @Column(name = "quality_profile", length = 32)
    @Builder.Default
    private String qualityProfile = "standard";

    // === ADVANCED SETTINGS ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "advanced_settings", columnDefinition = "jsonb")
    @Builder.Default
    private String advancedSettings = "{}";

    @Column(name = "slicer_id")
    private UUID slicerId;

    @NotNull(message = "Created by user ID is required")
    @Size(max = 64, message = "User ID must not exceed 64 characters")
    @Column(name = "created_by_user_id", nullable = false, length = 64)
    private String createdByUserId;

    @NotNull(message = "Public flag is required")
    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @NotNull(message = "Active flag is required")
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "slicingProperty", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SlicingPropertyMaterial> materialAssociations;

    // === LIFECYCLE METHODS ===
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

    // === BUSINESS METHODS ===

    /**
     * Validates that all required parameters for slicing are set
     */
    public boolean isValidForSlicing() {
        return layerHeightMm != null &&
                printSpeedMmS != null &&
                infillPercentage != null &&
                perimeterCount != null &&
                topSolidLayers != null &&
                bottomSolidLayers != null &&
                supportsEnabled != null &&
                slicerId != null &&
                isActive;
    }

    /**
     * Returns a human-readable summary of the slicing configuration
     */
    public String getConfigurationSummary() {
        return String.format("%s Profile | %.2fmm layer | %.0f mm/s | %.0f%% infill | %s pattern",
                qualityProfile != null ? qualityProfile.toUpperCase() : "CUSTOM",
                layerHeightMm != null ? layerHeightMm.doubleValue() : 0.2,
                printSpeedMmS != null ? printSpeedMmS.doubleValue() : 50.0,
                infillPercentage != null ? infillPercentage.doubleValue() : 20.0,
                infillPattern != null ? infillPattern : "grid");
    }
}