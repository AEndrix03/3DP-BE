package it.aredegalli.printer.service.slicing.metrics;

import it.aredegalli.printer.model.slicing.metric.SlicingMetric;
import it.aredegalli.printer.model.slicing.result.SlicingResult;
import it.aredegalli.printer.repository.slicing.metric.SlicingMetricsRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import it.aredegalli.printer.service.slicing.analysis.GcodeAnalyzer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlicingMetricsServiceImpl implements SlicingMetricsService {

    private final SlicingMetricsRepository metricsRepository;
    private final LogService logService;

    // Enhanced dependencies for real G-code analysis
    private final GcodeAnalyzer gcodeAnalyzer;
    private final FileResourceService fileResourceService;

    @Override
    public SlicingMetric calculateMetrics(SlicingResult result) {
        logService.info("SlicingMetricsServiceImpl", "Calculating real metrics for result: " + result.getId());

        try {
            // Use real G-code analysis if available
            return calculateRealMetrics(result);

        } catch (Exception e) {
            logService.error("SlicingMetricsServiceImpl",
                    "Failed to calculate real metrics, falling back to estimates: " + e.getMessage());

            // Fallback to original estimation logic
            return calculateEstimatedMetrics(result);
        }
    }

    @Override
    public SlicingMetric getMetricsBySlicingResultId(UUID slicingResultId) {
        return metricsRepository.findBySlicingResultId(slicingResultId).orElse(null);
    }

    @Override
    public void saveMetrics(SlicingMetric metrics) {
        metricsRepository.save(metrics);
    }

    // ======================================
    // NEW REAL METRICS CALCULATION
    // ======================================

    private SlicingMetric calculateRealMetrics(SlicingResult result) throws Exception {
        // Download and analyze the actual G-code
        try (InputStream gcodeStream = fileResourceService.download(result.getGeneratedFile().getId())) {
            GcodeAnalyzer.GcodeMetrics analysis = gcodeAnalyzer.analyzeGcode(gcodeStream);

            // Create SlicingMetric from real analysis
            SlicingMetric metrics = SlicingMetric.builder()
                    .slicingResult(result)
                    .sliceTimeSeconds(calculateSliceTime(result))
                    .estimatedPrintTimeMinutes(analysis.getEstimatedPrintTimeMinutes())
                    .materialVolumeMm3(analysis.getMaterialVolumeMm3())
                    .materialWeightG(analysis.getMaterialWeightG())
                    .estimatedCost(analysis.getEstimatedCost())
                    .layerCount(analysis.getLayerCount())
                    .supportVolumeMm3(analysis.getSupportVolumeMm3())
                    .build();

            // Save to database
            SlicingMetric savedMetrics = metricsRepository.save(metrics);

            logService.info("SlicingMetricsServiceImpl",
                    String.format("Real metrics calculated - Layers: %d, Print time: %d min, Weight: %.2f g, Cost: €%.2f",
                            analysis.getLayerCount(),
                            analysis.getEstimatedPrintTimeMinutes(),
                            analysis.getMaterialWeightG().doubleValue(),
                            analysis.getEstimatedCost().doubleValue()));

            return savedMetrics;
        }
    }

    // ======================================
    // ORIGINAL ESTIMATION LOGIC (Enhanced as fallback)
    // ======================================

    private SlicingMetric calculateEstimatedMetrics(SlicingResult result) {
        logService.info("SlicingMetricsServiceImpl", "Using estimated metrics for result: " + result.getId());

        SlicingMetric metrics = SlicingMetric.builder()
                .slicingResult(result)
                .sliceTimeSeconds(calculateSliceTime(result))
                .estimatedPrintTimeMinutes(calculatePrintTime(result))
                .materialVolumeMm3(calculateMaterialVolume(result))
                .materialWeightG(calculateMaterialWeight(result))
                .estimatedCost(calculateCost(result))
                .layerCount(calculateLayerCount(result))
                .supportVolumeMm3(calculateSupportVolume(result))
                .build();

        return metricsRepository.save(metrics);
    }

    // ======================================
    // ENHANCED CALCULATION METHODS
    // ======================================

    /**
     * Calculate slicing time based on creation timestamps or estimate from complexity
     */
    private Integer calculateSliceTime(SlicingResult result) {
        // This would ideally be tracked during the actual slicing process
        // For now, estimate based on model complexity
        long fileSize = result.getSourceFile().getFileSize();
        long lines = result.getLines();

        // Enhanced heuristic: consider both file size and G-code complexity
        int baseTime = 30; // minimum 30 seconds
        int fileSizeFactor = (int) (fileSize / 50000); // +1 second per 50KB
        int linesFactor = (int) (lines / 1000); // +1 second per 1000 lines

        int estimatedSeconds = baseTime + fileSizeFactor + linesFactor;

        return Math.min(estimatedSeconds, 3600); // Cap at 1 hour
    }

    private Integer calculatePrintTime(SlicingResult result) {
        // Enhanced estimation based on G-code lines and model complexity
        long lines = result.getLines();
        long modelSize = result.getSourceFile().getFileSize();

        // More sophisticated estimation
        double baseTimePerLine = 0.02; // minutes per line
        double complexityFactor = Math.log10(Math.max(modelSize, 1000)) / 10; // complexity based on size

        return (int) Math.max(15, lines * baseTimePerLine * (1 + complexityFactor));
    }

    private Integer calculateLayerCount(SlicingResult result) {
        // Estimate layers from G-code lines
        long lines = result.getLines();

        // Typical G-code has ~50-200 lines per layer depending on complexity
        int estimatedLayers = (int) Math.max(1, lines / 100);

        return Math.min(estimatedLayers, 10000); // Reasonable upper bound
    }

    private BigDecimal calculateMaterialVolume(SlicingResult result) {
        // Enhanced volume estimation
        long modelSize = result.getSourceFile().getFileSize();
        long lines = result.getLines();

        // More sophisticated heuristic based on both model size and G-code complexity
        double volumeEstimate = (modelSize * 0.008) + (lines * 0.01);

        return BigDecimal.valueOf(Math.max(100, volumeEstimate)) // minimum 100mm³
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMaterialWeight(SlicingResult result) {
        // Calculate weight from volume using standard PLA density
        BigDecimal volume = calculateMaterialVolume(result);
        BigDecimal densityGPerCm3 = BigDecimal.valueOf(1.25); // PLA density

        // Convert mm³ to cm³ and multiply by density
        return volume.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                .multiply(densityGPerCm3)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateCost(SlicingResult result) {
        // Calculate cost from weight using standard PLA price
        BigDecimal weight = calculateMaterialWeight(result);
        BigDecimal costPerKg = BigDecimal.valueOf(25.0); // €25/kg for PLA

        return weight.divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                .multiply(costPerKg)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSupportVolume(SlicingResult result) {
        // Estimate support volume (if any) - typically 5-15% of main volume
        BigDecimal mainVolume = calculateMaterialVolume(result);

        // Simple heuristic: assume 10% support volume for complex models
        if (result.getSourceFile().getFileSize() > 1_000_000) { // > 1MB = complex
            return mainVolume.multiply(BigDecimal.valueOf(0.1))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.ZERO;
    }

    // ======================================
    // ENHANCED CALCULATION WITH CUSTOM MATERIALS
    // ======================================

    /**
     * Calculate metrics with custom material properties
     */
    public SlicingMetric calculateMetricsWithMaterial(SlicingResult result, MaterialProperties material) {
        try {
            // Try real analysis first
            try (InputStream gcodeStream = fileResourceService.download(result.getGeneratedFile().getId())) {
                GcodeAnalyzer.GcodeMetrics analysis = gcodeAnalyzer.analyzeGcode(gcodeStream);

                // Recalculate weight and cost based on actual material properties
                BigDecimal volume = analysis.getMaterialVolumeMm3();
                BigDecimal weightG = volume
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                        .multiply(material.getDensityGPerCm3());

                BigDecimal cost = weightG
                        .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                        .multiply(material.getCostPerKg());

                return SlicingMetric.builder()
                        .slicingResult(result)
                        .sliceTimeSeconds(calculateSliceTime(result))
                        .estimatedPrintTimeMinutes(analysis.getEstimatedPrintTimeMinutes())
                        .materialVolumeMm3(volume)
                        .materialWeightG(weightG.setScale(2, RoundingMode.HALF_UP))
                        .estimatedCost(cost.setScale(2, RoundingMode.HALF_UP))
                        .layerCount(analysis.getLayerCount())
                        .supportVolumeMm3(analysis.getSupportVolumeMm3())
                        .build();
            }

        } catch (Exception e) {
            logService.error("SlicingMetricsServiceImpl",
                    "Failed to calculate metrics with material properties using real analysis: " + e.getMessage());

            // Fallback to estimation with material properties
            BigDecimal volume = calculateMaterialVolume(result);
            BigDecimal weightG = volume
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                    .multiply(material.getDensityGPerCm3());

            BigDecimal cost = weightG
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                    .multiply(material.getCostPerKg());

            return SlicingMetric.builder()
                    .slicingResult(result)
                    .sliceTimeSeconds(calculateSliceTime(result))
                    .estimatedPrintTimeMinutes(calculatePrintTime(result))
                    .materialVolumeMm3(volume)
                    .materialWeightG(weightG.setScale(2, RoundingMode.HALF_UP))
                    .estimatedCost(cost.setScale(2, RoundingMode.HALF_UP))
                    .layerCount(calculateLayerCount(result))
                    .supportVolumeMm3(calculateSupportVolume(result))
                    .build();
        }
    }

    /**
     * Material properties for accurate cost calculations
     */
    public static class MaterialProperties {
        private final BigDecimal densityGPerCm3;
        private final BigDecimal costPerKg;
        private final String materialType;

        public MaterialProperties(double densityGPerCm3, double costPerKg, String materialType) {
            this.densityGPerCm3 = BigDecimal.valueOf(densityGPerCm3);
            this.costPerKg = BigDecimal.valueOf(costPerKg);
            this.materialType = materialType;
        }

        // Predefined common materials
        public static final MaterialProperties PLA = new MaterialProperties(1.25, 25.0, "PLA");
        public static final MaterialProperties ABS = new MaterialProperties(1.04, 28.0, "ABS");
        public static final MaterialProperties PETG = new MaterialProperties(1.27, 32.0, "PETG");
        public static final MaterialProperties TPU = new MaterialProperties(1.20, 45.0, "TPU");

        public BigDecimal getDensityGPerCm3() {
            return densityGPerCm3;
        }

        public BigDecimal getCostPerKg() {
            return costPerKg;
        }

        public String getMaterialType() {
            return materialType;
        }
    }
}
