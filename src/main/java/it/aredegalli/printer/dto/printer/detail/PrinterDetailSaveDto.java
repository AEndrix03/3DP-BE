package it.aredegalli.printer.dto.printer.detail;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PrinterDetailSaveDto {

    private UUID id;

    @NotBlank(message = "Printer name is required")
    private String name;
    private UUID driverId;
    private UUID firmwareVersionId;
    private String buildVolumeXMm;
    private String buildVolumeYMm;
    private String buildVolumeZMm;
    private String buildPlateMaterial;

    @PositiveOrZero(message = "Extruder count must be positive or zero")
    private Long extruderCount;
    private String nozzleDiameterMm;

    @PositiveOrZero(message = "Max nozzle temperature must be positive or zero")
    private Long maxNozzleTempC;
    private String hotendType;
    private String kinematicsType;
    private String maxPrintSpeedMmS;
    private String maxTravelSpeedMmS;
    private String maxAccelerationMmS2;
    private String maxJerkMmS;
    private String hasHeatedBed;

    @PositiveOrZero(message = "Max bed temperature must be positive or zero")
    private Long maxBedTempC;
    private String bedSizeXMm;
    private String bedSizeYMm;
    private String hasHeatedChamber;

    @PositiveOrZero(message = "Max chamber temperature must be positive or zero")
    private Long maxChamberTempC;
    private String hasAutoBedLeveling;
    private String bedLevelingType;
    private String hasFilamentSensor;
    private String hasPowerRecovery;
    private String hasResumePrint;
    private String minLayerHeightMm;
    private String maxLayerHeightMm;
}