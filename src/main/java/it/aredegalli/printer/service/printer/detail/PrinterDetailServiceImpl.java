package it.aredegalli.printer.service.printer.detail;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailDto;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailSaveDto;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.printer.PrinterFirmware;
import it.aredegalli.printer.model.printer.PrinterSpecifications;
import it.aredegalli.printer.repository.printer.PrinterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrinterDetailServiceImpl implements PrinterDetailService {

    private final PrinterRepository printerRepository;

    public PrinterDetailDto getPrinterById(UUID printerId) {
        Printer printer = findPrinterById(printerId);
        return buildDetailDto(printer);
    }

    @Transactional
    public UUID savePrinter(PrinterDetailSaveDto saveDto) {
        Printer printer = saveDto.getId() != null ?
                findPrinterById(saveDto.getId()) :
                Printer.builder().build();

        printer.setName(saveDto.getName());
        printer.setDriverId(saveDto.getDriverId());

        if (saveDto.getId() == null) {
            printer.setLastSeen(Instant.now());
        }

        if (hasSpecificationData(saveDto)) {
            PrinterSpecifications specs = printer.getPrinterSpecifications();
            if (specs == null) {
                printer.setPrinterSpecifications(buildSpecifications(saveDto, printer));
            } else {
                updateSpecifications(specs, saveDto);
            }
        }

        if (saveDto.getFirmwareVersionId() != null) {
            PrinterFirmware currentFirmware = printer.getPrinterFirmware();
            if (currentFirmware == null || !saveDto.getFirmwareVersionId().equals(
                    currentFirmware.getFirmwareVersionId())) {
                printer.setPrinterFirmware(buildFirmware(saveDto.getFirmwareVersionId(), printer));
            }
        }

        return printerRepository.save(printer).getId();
    }

    private Printer findPrinterById(UUID id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Printer not found"));
    }

    private PrinterDetailDto buildDetailDto(Printer printer) {
        PrinterSpecifications specs = printer.getPrinterSpecifications();
        PrinterFirmware firmware = printer.getPrinterFirmware();

        return PrinterDetailDto.builder()
                .id(printer.getId())
                .name(printer.getName())
                .driverId(printer.getDriverId())
                .lastSeen(printer.getLastSeen())
                .firmwareVersionId(firmware != null ? firmware.getFirmwareVersionId() : null)
                .firmwareInstalledAt(firmware != null ? firmware.getInstalledAt() : null)
                .buildVolumeXMm(specs != null ? specs.getBuildVolumeXMm() : null)
                .buildVolumeYMm(specs != null ? specs.getBuildVolumeYMm() : null)
                .buildVolumeZMm(specs != null ? specs.getBuildVolumeZMm() : null)
                .buildPlateMaterial(specs != null ? specs.getBuildPlateMaterial() : null)
                .extruderCount(specs != null ? specs.getExtruderCount() : null)
                .nozzleDiameterMm(specs != null ? specs.getNozzleDiameterMm() : null)
                .maxNozzleTempC(specs != null ? specs.getMaxNozzleTempC() : null)
                .hotendType(specs != null ? specs.getHotendType() : null)
                .kinematicsType(specs != null ? specs.getKinematicsType() : null)
                .maxPrintSpeedMmS(specs != null ? specs.getMaxPrintSpeedMmS() : null)
                .maxTravelSpeedMmS(specs != null ? specs.getMaxTravelSpeedMmS() : null)
                .maxAccelerationMmS2(specs != null ? specs.getMaxAccelerationMmS2() : null)
                .maxJerkMmS(specs != null ? specs.getMaxJerkMmS() : null)
                .hasHeatedBed(specs != null ? specs.getHasHeatedBed() : null)
                .maxBedTempC(specs != null ? specs.getMaxBedTempC() : null)
                .bedSizeXMm(specs != null ? specs.getBedSizeXMm() : null)
                .bedSizeYMm(specs != null ? specs.getBedSizeYMm() : null)
                .hasHeatedChamber(specs != null ? specs.getHasHeatedChamber() : null)
                .maxChamberTempC(specs != null ? specs.getMaxChamberTempC() : null)
                .hasAutoBedLeveling(specs != null ? specs.getHasAutoBedLeveling() : null)
                .bedLevelingType(specs != null ? specs.getBedLevelingType() : null)
                .hasFilamentSensor(specs != null ? specs.getHasFilamentSensor() : null)
                .hasPowerRecovery(specs != null ? specs.getHasPowerRecovery() : null)
                .hasResumePrint(specs != null ? specs.getHasResumePrint() : null)
                .minLayerHeightMm(specs != null ? specs.getMinLayerHeightMm() : null)
                .maxLayerHeightMm(specs != null ? specs.getMaxLayerHeightMm() : null)
                .specificationsCreatedAt(specs != null ? specs.getCreatedAt() : null)
                .specificationsUpdatedAt(specs != null ? specs.getUpdatedAt() : null)
                .build();
    }

    private PrinterSpecifications buildSpecifications(PrinterDetailSaveDto dto, Printer printer) {
        Instant now = Instant.now();
        return PrinterSpecifications.builder()
                .printer(printer)
                .buildVolumeXMm(dto.getBuildVolumeXMm())
                .buildVolumeYMm(dto.getBuildVolumeYMm())
                .buildVolumeZMm(dto.getBuildVolumeZMm())
                .buildPlateMaterial(dto.getBuildPlateMaterial())
                .extruderCount(dto.getExtruderCount() != null ? dto.getExtruderCount() : 0L)
                .nozzleDiameterMm(dto.getNozzleDiameterMm())
                .maxNozzleTempC(dto.getMaxNozzleTempC() != null ? dto.getMaxNozzleTempC() : 0L)
                .hotendType(dto.getHotendType())
                .kinematicsType(dto.getKinematicsType())
                .maxPrintSpeedMmS(dto.getMaxPrintSpeedMmS())
                .maxTravelSpeedMmS(dto.getMaxTravelSpeedMmS())
                .maxAccelerationMmS2(dto.getMaxAccelerationMmS2())
                .maxJerkMmS(dto.getMaxJerkMmS())
                .hasHeatedBed(dto.getHasHeatedBed())
                .maxBedTempC(dto.getMaxBedTempC() != null ? dto.getMaxBedTempC() : 0L)
                .bedSizeXMm(dto.getBedSizeXMm())
                .bedSizeYMm(dto.getBedSizeYMm())
                .hasHeatedChamber(dto.getHasHeatedChamber())
                .maxChamberTempC(dto.getMaxChamberTempC() != null ? dto.getMaxChamberTempC() : 0L)
                .hasAutoBedLeveling(dto.getHasAutoBedLeveling())
                .bedLevelingType(dto.getBedLevelingType())
                .hasFilamentSensor(dto.getHasFilamentSensor())
                .hasPowerRecovery(dto.getHasPowerRecovery())
                .hasResumePrint(dto.getHasResumePrint())
                .minLayerHeightMm(dto.getMinLayerHeightMm())
                .maxLayerHeightMm(dto.getMaxLayerHeightMm())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void updateSpecifications(PrinterSpecifications specs, PrinterDetailSaveDto dto) {
        specs.setBuildVolumeXMm(dto.getBuildVolumeXMm());
        specs.setBuildVolumeYMm(dto.getBuildVolumeYMm());
        specs.setBuildVolumeZMm(dto.getBuildVolumeZMm());
        specs.setBuildPlateMaterial(dto.getBuildPlateMaterial());
        specs.setExtruderCount(dto.getExtruderCount() != null ? dto.getExtruderCount() : specs.getExtruderCount());
        specs.setNozzleDiameterMm(dto.getNozzleDiameterMm());
        specs.setMaxNozzleTempC(dto.getMaxNozzleTempC() != null ? dto.getMaxNozzleTempC() : specs.getMaxNozzleTempC());
        specs.setHotendType(dto.getHotendType());
        specs.setKinematicsType(dto.getKinematicsType());
        specs.setMaxPrintSpeedMmS(dto.getMaxPrintSpeedMmS());
        specs.setMaxTravelSpeedMmS(dto.getMaxTravelSpeedMmS());
        specs.setMaxAccelerationMmS2(dto.getMaxAccelerationMmS2());
        specs.setMaxJerkMmS(dto.getMaxJerkMmS());
        specs.setHasHeatedBed(dto.getHasHeatedBed());
        specs.setMaxBedTempC(dto.getMaxBedTempC() != null ? dto.getMaxBedTempC() : specs.getMaxBedTempC());
        specs.setBedSizeXMm(dto.getBedSizeXMm());
        specs.setBedSizeYMm(dto.getBedSizeYMm());
        specs.setHasHeatedChamber(dto.getHasHeatedChamber());
        specs.setMaxChamberTempC(dto.getMaxChamberTempC() != null ? dto.getMaxChamberTempC() : specs.getMaxChamberTempC());
        specs.setHasAutoBedLeveling(dto.getHasAutoBedLeveling());
        specs.setBedLevelingType(dto.getBedLevelingType());
        specs.setHasFilamentSensor(dto.getHasFilamentSensor());
        specs.setHasPowerRecovery(dto.getHasPowerRecovery());
        specs.setHasResumePrint(dto.getHasResumePrint());
        specs.setMinLayerHeightMm(dto.getMinLayerHeightMm());
        specs.setMaxLayerHeightMm(dto.getMaxLayerHeightMm());
        specs.setUpdatedAt(Instant.now());
    }

    private PrinterFirmware buildFirmware(UUID firmwareVersionId, Printer printer) {
        return PrinterFirmware.builder()
                .printer(printer)
                .firmwareVersionId(firmwareVersionId)
                .installedAt(Instant.now())
                .build();
    }

    private boolean hasSpecificationData(PrinterDetailSaveDto dto) {
        return dto.getBuildVolumeXMm() != null || dto.getBuildVolumeYMm() != null ||
                dto.getBuildVolumeZMm() != null || dto.getBuildPlateMaterial() != null ||
                dto.getExtruderCount() != null || dto.getNozzleDiameterMm() != null ||
                dto.getMaxNozzleTempC() != null || dto.getHotendType() != null ||
                dto.getKinematicsType() != null || dto.getMaxPrintSpeedMmS() != null ||
                dto.getMaxTravelSpeedMmS() != null || dto.getMaxAccelerationMmS2() != null ||
                dto.getMaxJerkMmS() != null || dto.getHasHeatedBed() != null ||
                dto.getMaxBedTempC() != null || dto.getBedSizeXMm() != null ||
                dto.getBedSizeYMm() != null || dto.getHasHeatedChamber() != null ||
                dto.getMaxChamberTempC() != null || dto.getHasAutoBedLeveling() != null ||
                dto.getBedLevelingType() != null || dto.getHasFilamentSensor() != null ||
                dto.getHasPowerRecovery() != null || dto.getHasResumePrint() != null ||
                dto.getMinLayerHeightMm() != null || dto.getMaxLayerHeightMm() != null;
    }
}