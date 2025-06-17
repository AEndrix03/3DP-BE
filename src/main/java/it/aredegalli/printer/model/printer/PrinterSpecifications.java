package it.aredegalli.printer.model.printer;

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
@Table(name = "printer_specifications")
public class PrinterSpecifications {
    @Id
    @GeneratedValue(generator = "UUID")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "printer_id")
    private Printer printer;

    @Column(name = "build_volume_x_mm")
    private String buildVolumeXMm;

    @Column(name = "build_volume_y_mm")
    private String buildVolumeYMm;

    @Column(name = "build_volume_z_mm")
    private String buildVolumeZMm;

    @Column(name = "build_plate_material")
    private String buildPlateMaterial;

    @Column(name = "extruder_count")
    private long extruderCount;

    @Column(name = "nozzle_diameter_mm")
    private String nozzleDiameterMm;

    @Column(name = "max_nozzle_temp_c")
    private long maxNozzleTempC;

    @Column(name = "hotend_type")
    private String hotendType;

    @Column(name = "kinematics_type")
    private String kinematicsType;

    @Column(name = "max_print_speed_mm_s")
    private String maxPrintSpeedMmS;

    @Column(name = "max_travel_speed_mm_s")
    private String maxTravelSpeedMmS;

    @Column(name = "max_acceleration_mm_s2")
    private String maxAccelerationMmS2;

    @Column(name = "max_jerk_mm_s")
    private String maxJerkMmS;

    @Column(name = "has_heated_bed")
    private String hasHeatedBed;

    @Column(name = "max_bed_temp_c")
    private long maxBedTempC;

    @Column(name = "bed_size_x_mm")
    private String bedSizeXMm;

    @Column(name = "bed_size_y_mm")
    private String bedSizeYMm;

    @Column(name = "has_heated_chamber")
    private String hasHeatedChamber;

    @Column(name = "max_chamber_temp_c")
    private long maxChamberTempC;

    @Column(name = "has_auto_bed_leveling")
    private String hasAutoBedLeveling;

    @Column(name = "bed_leveling_type")
    private String bedLevelingType;

    @Column(name = "has_filament_sensor")
    private String hasFilamentSensor;

    @Column(name = "has_power_recovery")
    private String hasPowerRecovery;

    @Column(name = "has_resume_print")
    private String hasResumePrint;

    @Column(name = "min_layer_height_mm")
    private String minLayerHeightMm;

    @Column(name = "max_layer_height_mm")
    private String maxLayerHeightMm;

    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private Instant createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ DEFAULT now()")
    private Instant updatedAt;
}
