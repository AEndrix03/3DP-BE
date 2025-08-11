package it.aredegalli.printer.dto.printer.detail;

import it.aredegalli.printer.dto.printer.PrinterDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class PrinterDetailDto extends PrinterDto {

    private UUID firmwareVersionId;
    private Instant firmwareInstalledAt;
    private String buildVolumeXMm;
    private String buildVolumeYMm;
    private String buildVolumeZMm;
    private String buildPlateMaterial;
    private Long extruderCount;
    private String nozzleDiameterMm;
    private Long maxNozzleTempC;
    private String hotendType;
    private String kinematicsType;
    private String maxPrintSpeedMmS;
    private String maxTravelSpeedMmS;
    private String maxAccelerationMmS2;
    private String maxJerkMmS;
    private String hasHeatedBed;
    private Long maxBedTempC;
    private String bedSizeXMm;
    private String bedSizeYMm;
    private String hasHeatedChamber;
    private Long maxChamberTempC;
    private String hasAutoBedLeveling;
    private String bedLevelingType;
    private String hasFilamentSensor;
    private String hasPowerRecovery;
    private String hasResumePrint;
    private String minLayerHeightMm;
    private String maxLayerHeightMm;
    private Instant specificationsCreatedAt;
    private Instant specificationsUpdatedAt;
}