package it.aredegalli.printer.dto.slicing;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Slicing Property DTO - Generic slicing profile configuration")
public class SlicingPropertyDto {

    @Schema(description = "Unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @NotBlank(message = "Name is required")
    @Size(max = 128, message = "Name must not exceed 128 characters")
    @Schema(description = "Profile name", example = "High Quality PLA", required = true)
    private String name;

    @Size(max = 512, message = "Description must not exceed 512 characters")
    @Schema(description = "Profile description", example = "High quality profile optimized for PLA printing")
    private String description;

    // === LAYER SETTINGS ===
    @NotNull(message = "Layer height is required")
    @DecimalMin(value = "0.001", message = "Layer height must be greater than 0")
    @DecimalMax(value = "2.0", message = "Layer height must not exceed 2.0mm")
    @Schema(description = "Layer height in mm", example = "0.2", required = true)
    private BigDecimal layerHeightMm;

    @DecimalMin(value = "0.001", message = "First layer height must be greater than 0")
    @DecimalMax(value = "2.0", message = "First layer height must not exceed 2.0mm")
    @Schema(description = "First layer height in mm", example = "0.3")
    private BigDecimal firstLayerHeightMm;

    @DecimalMin(value = "0.001", message = "Line width must be greater than 0")
    @DecimalMax(value = "5.0", message = "Line width must not exceed 5.0mm")
    @Schema(description = "Line width in mm", example = "0.4")
    private BigDecimal lineWidthMm;

    // === SPEED SETTINGS (mm/s) ===
    @NotNull(message = "Print speed is required")
    @DecimalMin(value = "0.1", message = "Print speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Print speed must not exceed 1000 mm/s")
    @Schema(description = "Print speed in mm/s", example = "50.0", required = true)
    private BigDecimal printSpeedMmS;

    @DecimalMin(value = "0.1", message = "First layer speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "First layer speed must not exceed 1000 mm/s")
    @Schema(description = "First layer speed in mm/s", example = "20.0")
    private BigDecimal firstLayerSpeedMmS;

    @DecimalMin(value = "0.1", message = "Travel speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Travel speed must not exceed 1000 mm/s")
    @Schema(description = "Travel speed in mm/s", example = "150.0")
    private BigDecimal travelSpeedMmS;

    @DecimalMin(value = "0.1", message = "Infill speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Infill speed must not exceed 1000 mm/s")
    @Schema(description = "Infill speed in mm/s", example = "50.0")
    private BigDecimal infillSpeedMmS;

    @DecimalMin(value = "0.1", message = "Outer wall speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Outer wall speed must not exceed 1000 mm/s")
    @Schema(description = "Outer wall speed in mm/s", example = "45.0")
    private BigDecimal outerWallSpeedMmS;

    @DecimalMin(value = "0.1", message = "Inner wall speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Inner wall speed must not exceed 1000 mm/s")
    @Schema(description = "Inner wall speed in mm/s", example = "55.0")
    private BigDecimal innerWallSpeedMmS;

    @DecimalMin(value = "0.1", message = "Top/bottom speed must be greater than 0")
    @DecimalMax(value = "1000.0", message = "Top/bottom speed must not exceed 1000 mm/s")
    @Schema(description = "Top/bottom speed in mm/s", example = "40.0")
    private BigDecimal topBottomSpeedMmS;

    // === INFILL SETTINGS ===
    @NotNull(message = "Infill percentage is required")
    @DecimalMin(value = "0", message = "Infill percentage must be between 0 and 100")
    @DecimalMax(value = "100", message = "Infill percentage must be between 0 and 100")
    @Schema(description = "Infill percentage (0-100)", example = "20.0", required = true)
    private BigDecimal infillPercentage;

    @NotNull(message = "Infill pattern is required")
    @Pattern(regexp = "^(grid|lines|triangles|cubic|gyroid|honeycomb|concentric)$",
            message = "Invalid infill pattern")
    @Schema(description = "Infill pattern", example = "grid",
            allowableValues = {"grid", "lines", "triangles", "cubic", "gyroid", "honeycomb", "concentric"})
    private String infillPattern;

    // === SHELL SETTINGS ===
    @Min(value = 0, message = "Perimeter count must be non-negative")
    @Max(value = 10, message = "Perimeter count must not exceed 10")
    @Schema(description = "Number of perimeter walls", example = "2")
    private Integer perimeterCount;

    @Min(value = 0, message = "Top solid layers must be non-negative")
    @Max(value = 20, message = "Top solid layers must not exceed 20")
    @Schema(description = "Number of top solid layers", example = "3")
    private Integer topSolidLayers;

    @Min(value = 0, message = "Bottom solid layers must be non-negative")
    @Max(value = 20, message = "Bottom solid layers must not exceed 20")
    @Schema(description = "Number of bottom solid layers", example = "3")
    private Integer bottomSolidLayers;

    @DecimalMin(value = "0", message = "Top/bottom thickness must be non-negative")
    @DecimalMax(value = "10.0", message = "Top/bottom thickness must not exceed 10mm")
    @Schema(description = "Top/bottom thickness in mm", example = "0.8")
    private BigDecimal topBottomThicknessMm;

    // === SUPPORT SETTINGS ===
    @NotNull(message = "Supports enabled flag is required")
    @Schema(description = "Enable supports", example = "false", required = true)
    private Boolean supportsEnabled;

    @DecimalMin(value = "0", message = "Support angle must be between 0 and 90")
    @DecimalMax(value = "90", message = "Support angle must be between 0 and 90")
    @Schema(description = "Support angle threshold in degrees", example = "45.0")
    private BigDecimal supportAngleThreshold;

    @DecimalMin(value = "0", message = "Support density must be between 0 and 100")
    @DecimalMax(value = "100", message = "Support density must be between 0 and 100")
    @Schema(description = "Support density percentage", example = "20.0")
    private BigDecimal supportDensityPercentage;

    @Pattern(regexp = "^(grid|lines|zigzag|triangles)$", message = "Invalid support pattern")
    @Schema(description = "Support pattern", example = "grid",
            allowableValues = {"grid", "lines", "zigzag", "triangles"})
    private String supportPattern;

    @DecimalMin(value = "0", message = "Support Z distance must be non-negative")
    @DecimalMax(value = "5.0", message = "Support Z distance must not exceed 5mm")
    @Schema(description = "Support Z distance in mm", example = "0.2")
    private BigDecimal supportZDistanceMm;

    // === ADHESION SETTINGS ===
    @Pattern(regexp = "^(none|brim|raft|skirt)$", message = "Invalid adhesion type")
    @Schema(description = "Adhesion type", example = "brim",
            allowableValues = {"none", "brim", "raft", "skirt"})
    private String adhesionType;

    @NotNull(message = "Brim enabled flag is required")
    @Schema(description = "Enable brim", example = "true", required = true)
    private Boolean brimEnabled;

    @DecimalMin(value = "0", message = "Brim width must be non-negative")
    @DecimalMax(value = "100.0", message = "Brim width must not exceed 100mm")
    @Schema(description = "Brim width in mm", example = "5.0")
    private BigDecimal brimWidthMm;

    // === COOLING SETTINGS ===
    @NotNull(message = "Fan enabled flag is required")
    @Schema(description = "Enable cooling fan", example = "true", required = true)
    private Boolean fanEnabled;

    @DecimalMin(value = "0", message = "Fan speed must be between 0 and 100")
    @DecimalMax(value = "100", message = "Fan speed must be between 0 and 100")
    @Schema(description = "Fan speed percentage", example = "100.0")
    private BigDecimal fanSpeedPercentage;

    // === RETRACTION SETTINGS ===
    @NotNull(message = "Retraction enabled flag is required")
    @Schema(description = "Enable retraction", example = "true", required = true)
    private Boolean retractionEnabled;

    @DecimalMin(value = "0", message = "Retraction distance must be non-negative")
    @DecimalMax(value = "10.0", message = "Retraction distance must not exceed 10mm")
    @Schema(description = "Retraction distance in mm", example = "1.0")
    private BigDecimal retractionDistanceMm;

    @NotNull(message = "Z-hop enabled flag is required")
    @Schema(description = "Enable Z-hop", example = "false", required = true)
    private Boolean zhopEnabled;

    @DecimalMin(value = "0", message = "Z-hop height must be non-negative")
    @DecimalMax(value = "5.0", message = "Z-hop height must not exceed 5mm")
    @Schema(description = "Z-hop height in mm", example = "0.2")
    private BigDecimal zhopHeightMm;

    // === TEMPERATURE SETTINGS (Optional - can be overridden by material) ===
    @Min(value = 150, message = "Extruder temperature must be at least 150°C")
    @Max(value = 350, message = "Extruder temperature must not exceed 350°C")
    @Schema(description = "Extruder temperature in Celsius", example = "210")
    private Integer extruderTempC;

    @Min(value = 0, message = "Bed temperature must be non-negative")
    @Max(value = 150, message = "Bed temperature must not exceed 150°C")
    @Schema(description = "Bed temperature in Celsius", example = "60")
    private Integer bedTempC;

    // === QUALITY PROFILE ===
    @Pattern(regexp = "^(draft|standard|high|ultra)$", message = "Invalid quality profile")
    @Schema(description = "Quality profile level", example = "standard",
            allowableValues = {"draft", "standard", "high", "ultra"})
    private String qualityProfile;

    // === ADVANCED SETTINGS ===
    @Schema(description = "Advanced settings in JSON format", example = "{\"custom_gcode\": \"G28\"}")
    private String advancedSettings;

    // === METADATA ===
    @Schema(description = "Slicer software ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID slicerId;

    @NotNull(message = "Created by user ID is required")
    @Size(max = 64, message = "User ID must not exceed 64 characters")
    @Schema(description = "User who created this profile", example = "user123", required = true)
    private String createdByUserId;

    @NotNull(message = "Public flag is required")
    @Schema(description = "Is profile public", example = "false", required = true)
    private Boolean isPublic;

    @NotNull(message = "Active flag is required")
    @Schema(description = "Is profile active", example = "true", required = true)
    private Boolean isActive;

    @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Last update timestamp", example = "2024-01-15T10:30:00Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "List of compatible material IDs",
            example = "[\"550e8400-e29b-41d4-a716-446655440001\", \"550e8400-e29b-41d4-a716-446655440002\"]")
    private List<UUID> materialIds;

}