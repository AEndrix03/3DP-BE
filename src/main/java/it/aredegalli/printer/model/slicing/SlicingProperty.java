package it.aredegalli.printer.model.slicing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "slicing_property")
public class SlicingProperty {

    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 64)
    private String id;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", length = 512)
    private String description;

    @Column(name = "material_id", nullable = false, length = 64)
    private String materialId;

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

}
