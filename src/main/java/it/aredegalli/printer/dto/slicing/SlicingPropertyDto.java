package it.aredegalli.printer.dto.slicing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlicingPropertyDto {

    private String id;
    private String name;
    private String description;
    private String layerHeightMm;
    private String firstLayerHeightMm;
    private String printSpeedMmS;
    private String travelSpeedMmS;
    private String firstLayerSpeedMmS;
    private String infillPercentage;
    private String infillPattern;
    private long perimeterCount;
    private long topSolidLayers;
    private long bottomSolidLayers;
    private String supportsEnabled;
    private String supportAngleThreshold;
    private String brimEnabled;
    private String brimWidthMm;
    private long extruderTempC;
    private long bedTempC;
    private String advancedSettings;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdByUserId;
    private String isPublic;

}