package it.aredegalli.printer.service.slicing.metrics;

import it.aredegalli.printer.model.slicing.SlicingMetric;
import it.aredegalli.printer.model.slicing.SlicingResult;
import it.aredegalli.printer.repository.slicing.SlicingMetricsRepository;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlicingMetricsServiceImpl implements SlicingMetricsService {

    private final SlicingMetricsRepository metricsRepository;
    private final LogService logService;

    @Override
    public SlicingMetric calculateMetrics(SlicingResult result) {
        logService.info("SlicingMetricsService", "Calculating metrics for result: " + result.getId());

        // Basic metrics calculation - extend as needed
        SlicingMetric metrics = SlicingMetric.builder()
                .slicingResult(result)
                .sliceTimeSeconds(60) // Dummy - track actual time
                .estimatedPrintTimeMinutes(calculatePrintTime(result))
                .layerCount(calculateLayerCount(result))
                .materialVolumeMm3(calculateMaterialVolume(result))
                .materialWeightG(calculateMaterialWeight(result))
                .estimatedCost(calculateCost(result))
                .build();

        return metricsRepository.save(metrics);
    }

    @Override
    public SlicingMetric getMetricsBySlicingResultId(UUID slicingResultId) {
        return metricsRepository.findBySlicingResultId(slicingResultId).orElse(null);
    }

    @Override
    public void saveMetrics(SlicingMetric metrics) {
        metricsRepository.save(metrics);
    }

    private Integer calculatePrintTime(SlicingResult result) {
        // Parse GCODE and estimate time - simplified
        return (int) (result.getLines() * 0.1); // 0.1 minutes per line
    }

    private Integer calculateLayerCount(SlicingResult result) {
        // Count layer comments in GCODE or calculate from properties
        return 100; // Dummy
    }

    private BigDecimal calculateMaterialVolume(SlicingResult result) {
        // Calculate from extrusion moves in GCODE
        return BigDecimal.valueOf(1500.0); // mm³
    }

    private BigDecimal calculateMaterialWeight(SlicingResult result) {
        // volume * density from material properties
        BigDecimal volume = calculateMaterialVolume(result);
        BigDecimal density = BigDecimal.valueOf(1.25); // PLA density g/cm³
        return volume.divide(BigDecimal.valueOf(1000)).multiply(density);
    }

    private BigDecimal calculateCost(SlicingResult result) {
        // weight * cost per kg from material
        BigDecimal weight = calculateMaterialWeight(result);
        BigDecimal costPerKg = BigDecimal.valueOf(25.0); // €25/kg
        return weight.divide(BigDecimal.valueOf(1000)).multiply(costPerKg);
    }
}
