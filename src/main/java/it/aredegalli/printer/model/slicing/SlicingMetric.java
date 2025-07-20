package it.aredegalli.printer.model.slicing;

import jakarta.persistence.*;
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

    @Column(name = "material_volume_mm3", precision = 10, scale = 2)
    private BigDecimal materialVolumeMm3;

    @Column(name = "material_weight_g", precision = 8, scale = 2)
    private BigDecimal materialWeightG;

    @Column(name = "estimated_cost", precision = 8, scale = 2)
    private BigDecimal estimatedCost;

    @Column(name = "layer_count")
    private Integer layerCount;

    @Column(name = "support_volume_mm3", precision = 10, scale = 2)
    private BigDecimal supportVolumeMm3;

}