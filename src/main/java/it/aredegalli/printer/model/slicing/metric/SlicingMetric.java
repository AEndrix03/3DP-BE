package it.aredegalli.printer.model.slicing.metric;

import it.aredegalli.printer.model.slicing.result.SlicingResult;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "slicing_metrics")
public class SlicingMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slicing_result_id")
    private SlicingResult slicingResult;

    @Column(name = "slice_time_seconds")
    private Integer sliceTimeSeconds;

    @Column(name = "estimated_print_time_minutes")
    private Integer estimatedPrintTimeMinutes;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "material_volume_mm3", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal materialVolumeMm3;

    @Digits(integer = 6, fraction = 2)
    @Column(name = "material_weight_g", columnDefinition = "DECIMAL(8,2)")
    private BigDecimal materialWeightG;

    @Digits(integer = 6, fraction = 2)
    @Column(name = "estimated_cost", columnDefinition = "DECIMAL(8,2)")
    private BigDecimal estimatedCost;

    @Column(name = "layer_count")
    private Integer layerCount;

    @Digits(integer = 8, fraction = 2)
    @Column(name = "support_volume_mm3", columnDefinition = "DECIMAL(10,2)")
    private BigDecimal supportVolumeMm3;
}