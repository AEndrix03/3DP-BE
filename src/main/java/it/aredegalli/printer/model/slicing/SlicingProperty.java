package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
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
@Table(name = "slicing_property", indexes = {
        @Index(name = "idx_slicing_property_name", columnList = "name"),
        @Index(name = "idx_slicing_property_material", columnList = "material_id"),
        @Index(name = "idx_slicing_property_user", columnList = "created_by_user_id")
})
public class SlicingProperty {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    // Fixed: Changed to UUID to match Material entity
    @Column(name = "material_id", nullable = false)
    private UUID materialId;

    // Optional: Add direct relationship if needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", insertable = false, updatable = false)
    private Material material;

    @Column(name = "layer_height_mm", length = 16)
    private String layerHeightMm;

    @Column(name = "first_layer_height_mm", length = 16)
    private String firstLayerHeightMm;

    @Column(name = "print_speed_mm_s", length = 16)
    private String printSpeedMmS;

    @Column(name = "travel_speed_mm_s", length = 16)
    private String travelSpeedMmS;

    @Column(name = "first_layer_speed_mm_s", length = 16)
    private String firstLayerSpeedMmS;

    @Column(name = "infill_percentage", length = 8)
    private String infillPercentage;

    @Column(name = "infill_pattern", length = 32)
    private String infillPattern;

    @Column(name = "perimeter_count")
    private long perimeterCount;

    @Column(name = "top_solid_layers")
    private long topSolidLayers;

    @Column(name = "bottom_solid_layers")
    private long bottomSolidLayers;

    @Column(name = "supports_enabled", length = 8)
    private String supportsEnabled;

    @Column(name = "support_angle_threshold", length = 8)
    private String supportAngleThreshold;

    @Column(name = "brim_enabled", length = 8)
    private String brimEnabled;

    @Column(name = "brim_width_mm", length = 16)
    private String brimWidthMm;

    @Column(name = "extruder_temp_c")
    private long extruderTempC;

    @Column(name = "bed_temp_c")
    private long bedTempC;

    @Column(name = "advanced_settings", length = 2048)
    private String advancedSettings;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by_user_id", length = 64)
    private String createdByUserId;

    @Column(name = "is_public", length = 8)
    private String isPublic;

    @Column(name = "printer_type")
    private String printerType; // MINI_15X15, ENDER3, PRUSA_I3, etc.

    @Column(name = "bed_width")
    private String bedWidth; // mm

    @Column(name = "bed_depth")
    private String bedDepth; // mm

    @Column(name = "bed_height")
    private String bedHeight; // mm

    // Quality Profile
    @Column(name = "quality_profile")
    private String qualityProfile; // DRAFT, NORMAL, FINE, ULTRA_FINE, MINIATURE

    @Column(name = "line_width_mm")
    private String lineWidthMm; // 0.4 typically

    @Column(name = "top_bottom_thickness_mm")
    private String topBottomThicknessMm;

    // Material Profile
    @Column(name = "material_type")
    private String materialType; // PLA, ABS, PETG, TPU, WOOD

    @Column(name = "retraction_enabled")
    private String retractionEnabled;

    @Column(name = "retraction_distance_mm")
    private String retractionDistanceMm;

    // Advanced Support Settings
    @Column(name = "support_density_percentage")
    private String supportDensityPercentage;

    @Column(name = "support_z_distance_mm")
    private String supportZDistanceMm;

    @Column(name = "support_pattern")
    private String supportPattern; // lines, grid, triangles

    // Adhesion Settings
    @Column(name = "adhesion_type")
    private String adhesionType; // skirt, brim, raft

    // Cooling Settings
    @Column(name = "fan_enabled")
    private String fanEnabled;

    @Column(name = "fan_speed_percentage")
    private String fanSpeedPercentage;

    // Advanced Speed Settings
    @Column(name = "outer_wall_speed_mms")
    private String outerWallSpeedMmS;

    @Column(name = "inner_wall_speed_mms")
    private String innerWallSpeedMmS;

    @Column(name = "infill_speed_mms")
    private String infillSpeedMmS;

    @Column(name = "top_bottom_speed_mms")
    private String topBottomSpeedMmS;

    // Layer-specific Settings
    @Column(name = "initial_layer_height_mm")
    private String initialLayerHeightMm;

    @Column(name = "initial_layer_speed_mms")
    private String initialLayerSpeedMmS;

    // Z-hop Settings
    @Column(name = "zhop_enabled")
    private String zhopEnabled;

    @Column(name = "zhop_height_mm")
    private String zhopHeightMm;

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

    // ======================================
    // UTILITY METHODS
    // ======================================

    /**
     * Creates a SlicingProperty with optimal settings for 15x15cm bed
     */
    public static SlicingProperty createMini15x15Profile() {
        return SlicingProperty.builder()
                .printerType("MINI_15X15")
                .bedDepth("150")
                .bedHeight("150")
                .bedWidth("150")
                .qualityProfile("NORMAL")
                .materialType("PLA")
                .build();
    }

    /**
     * Creates a high-quality profile for miniatures
     */
    public static SlicingProperty createMiniatureProfile() {
        return SlicingProperty.builder()
                .printerType("MINI_15X15")
                .bedDepth("150")
                .bedHeight("150")
                .bedWidth("150")
                .qualityProfile("MINIATURE")
                .materialType("PLA")
                .build();
    }

    /**
     * Creates a fast prototyping profile
     */
    public static SlicingProperty createDraftProfile() {
        return SlicingProperty.builder()
                .printerType("MINI_15X15")
                .bedDepth("150")
                .bedHeight("150")
                .bedWidth("150")
                .qualityProfile("DRAFT")
                .materialType("PLA")
                .build();
    }

    /**
     * Validates that all required parameters are set
     */
    public boolean isValid() {
        return layerHeightMm != null &&
                extruderTempC > 0 &&
                bedTempC >= 0 &&
                printSpeedMmS != null &&
                infillPercentage != null;
    }

    /**
     * Returns a summary string of the configuration
     */
    public String getConfigurationSummary() {
        return String.format("%s | %s | %.2fmm | %d°C/%d°C | %s%% infill",
                printerType != null ? printerType : "Generic",
                qualityProfile != null ? qualityProfile : "Custom",
                parseFloat(layerHeightMm, 0.2f),
                (int) extruderTempC,
                (int) bedTempC,
                infillPercentage != null ? infillPercentage : "20");
    }

    private float parseFloat(String value, float defaultValue) {
        try {
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}