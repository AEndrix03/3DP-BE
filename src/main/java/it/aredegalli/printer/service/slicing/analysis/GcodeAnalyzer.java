package it.aredegalli.printer.service.slicing.analysis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes G-code files to extract real slicing metrics
 */
@Component
@Slf4j
public class GcodeAnalyzer {

    // G-code patterns for parsing
    private static final Pattern LAYER_PATTERN = Pattern.compile(";\\s*LAYER\\s*[:\\s]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LAYER_HEIGHT_PATTERN = Pattern.compile(";\\s*layer_height\\s*=\\s*([\\d.]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRINT_TIME_PATTERN = Pattern.compile(";\\s*estimated\\s+printing\\s+time.*?(\\d+)h\\s*(\\d+)m\\s*(\\d+)s", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRINT_TIME_PATTERN_2 = Pattern.compile(";\\s*TIME\\s*[:\\s]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILAMENT_USED_PATTERN = Pattern.compile(";\\s*filament\\s+used.*?([\\d.]+)m", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILAMENT_WEIGHT_PATTERN = Pattern.compile(";\\s*filament\\s+used.*?([\\d.]+)g", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTRUSION_PATTERN = Pattern.compile("G1.*?E([\\d.-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SUPPORT_PATTERN = Pattern.compile(";\\s*TYPE:.*SUPPORT", Pattern.CASE_INSENSITIVE);

    private final GcodeMetrics metrics = new GcodeMetrics();

    public GcodeMetrics analyzeGcode(InputStream gcodeStream) throws IOException {
        reset();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(gcodeStream))) {
            String line;
            boolean inSupportSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Parse header comments for metadata
                parseMetadata(line);

                // Track layers
                parseLayers(line);

                // Track extrusion
                if (line.startsWith("G1") && line.contains("E")) {
                    parseExtrusion(line, inSupportSection);
                }

                // Track support sections
                if (SUPPORT_PATTERN.matcher(line).find()) {
                    inSupportSection = true;
                } else if (line.startsWith(";TYPE:") && !line.contains("SUPPORT")) {
                    inSupportSection = false;
                }
            }

            calculateDerivedMetrics();
        }

        return metrics;
    }

    private void parseMetadata(String line) {
        // Parse layer height
        Matcher layerHeightMatcher = LAYER_HEIGHT_PATTERN.matcher(line);
        if (layerHeightMatcher.find()) {
            metrics.layerHeight = Double.parseDouble(layerHeightMatcher.group(1));
        }

        // Parse print time (multiple formats)
        Matcher printTimeMatcher = PRINT_TIME_PATTERN.matcher(line);
        if (printTimeMatcher.find()) {
            int hours = Integer.parseInt(printTimeMatcher.group(1));
            int minutes = Integer.parseInt(printTimeMatcher.group(2));
            int seconds = Integer.parseInt(printTimeMatcher.group(3));
            metrics.estimatedPrintTimeMinutes = hours * 60 + minutes + (seconds > 30 ? 1 : 0);
        }

        // Alternative time format (seconds)
        Matcher timeMatcher2 = PRINT_TIME_PATTERN_2.matcher(line);
        if (timeMatcher2.find() && metrics.estimatedPrintTimeMinutes == 0) {
            int totalSeconds = Integer.parseInt(timeMatcher2.group(1));
            metrics.estimatedPrintTimeMinutes = (totalSeconds + 30) / 60; // Round to nearest minute
        }

        // Parse filament length
        Matcher filamentMatcher = FILAMENT_USED_PATTERN.matcher(line);
        if (filamentMatcher.find()) {
            metrics.filamentLengthMm = (int) (Double.parseDouble(filamentMatcher.group(1)) * 1000);
        }

        // Parse filament weight
        Matcher weightMatcher = FILAMENT_WEIGHT_PATTERN.matcher(line);
        if (weightMatcher.find()) {
            metrics.materialWeightG = BigDecimal.valueOf(Double.parseDouble(weightMatcher.group(1)))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }

    private void parseLayers(String line) {
        Matcher layerMatcher = LAYER_PATTERN.matcher(line);
        if (layerMatcher.find()) {
            int layerNum = Integer.parseInt(layerMatcher.group(1));
            metrics.layerCount = Math.max(metrics.layerCount, layerNum + 1);
        }
    }

    private void parseExtrusion(String line, boolean isSupport) {
        Matcher extrusionMatcher = EXTRUSION_PATTERN.matcher(line);
        if (extrusionMatcher.find()) {
            double extrusionAmount = Double.parseDouble(extrusionMatcher.group(1));

            if (extrusionAmount > metrics.lastExtrusionValue) {
                double extrusionDiff = extrusionAmount - metrics.lastExtrusionValue;
                metrics.totalExtrusion += extrusionDiff;

                if (isSupport) {
                    metrics.supportExtrusion += extrusionDiff;
                }
            }

            metrics.lastExtrusionValue = extrusionAmount;
        }
    }

    private void calculateDerivedMetrics() {
        // Calculate material volume from extrusion
        // Formula: volume = extrusion_length * (π * (nozzle_diameter/2)²) * layer_height
        double nozzleDiameter = 0.4; // mm - should come from printer config
        double nozzleArea = Math.PI * Math.pow(nozzleDiameter / 2, 2);

        if (metrics.layerHeight > 0) {
            double volumeMm3 = metrics.totalExtrusion * nozzleArea * metrics.layerHeight;
            metrics.materialVolumeMm3 = BigDecimal.valueOf(volumeMm3)
                    .setScale(2, RoundingMode.HALF_UP);

            double supportVolumeMm3 = metrics.supportExtrusion * nozzleArea * metrics.layerHeight;
            metrics.supportVolumeMm3 = BigDecimal.valueOf(supportVolumeMm3)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Calculate weight if not found in comments
        if (metrics.materialWeightG.compareTo(BigDecimal.ZERO) == 0 &&
                metrics.materialVolumeMm3.compareTo(BigDecimal.ZERO) > 0) {

            // Default PLA density: 1.25 g/cm³
            double densityGPerCm3 = 1.25;
            double volumeCm3 = metrics.materialVolumeMm3.doubleValue() / 1000;
            metrics.materialWeightG = BigDecimal.valueOf(volumeCm3 * densityGPerCm3)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Estimate cost (default: €25/kg for PLA)
        if (metrics.materialWeightG.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costPerKg = BigDecimal.valueOf(25.0);
            metrics.estimatedCost = metrics.materialWeightG
                    .divide(BigDecimal.valueOf(1000), 4, RoundingMode.HALF_UP)
                    .multiply(costPerKg)
                    .setScale(2, RoundingMode.HALF_UP);
        }

        log.debug("GcodeAnalyzer", "Analysis complete: {} layers, {} min print time, {} g material",
                metrics.layerCount, metrics.estimatedPrintTimeMinutes, metrics.materialWeightG);
    }

    private void reset() {
        metrics.reset();
    }

    public static class GcodeMetrics {
        public int layerCount = 0;
        public int estimatedPrintTimeMinutes = 0;
        public BigDecimal materialVolumeMm3 = BigDecimal.ZERO;
        public BigDecimal materialWeightG = BigDecimal.ZERO;
        public BigDecimal estimatedCost = BigDecimal.ZERO;
        public BigDecimal supportVolumeMm3 = BigDecimal.ZERO;
        public int filamentLengthMm = 0;

        // Internal tracking
        public double layerHeight = 0.2;
        public double totalExtrusion = 0;
        public double supportExtrusion = 0;
        public double lastExtrusionValue = 0;

        public void reset() {
            layerCount = 0;
            estimatedPrintTimeMinutes = 0;
            materialVolumeMm3 = BigDecimal.ZERO;
            materialWeightG = BigDecimal.ZERO;
            estimatedCost = BigDecimal.ZERO;
            supportVolumeMm3 = BigDecimal.ZERO;
            filamentLengthMm = 0;
            layerHeight = 0.2;
            totalExtrusion = 0;
            supportExtrusion = 0;
            lastExtrusionValue = 0;
        }

        public int getLayerCount() {
            return layerCount;
        }

        public int getEstimatedPrintTimeMinutes() {
            return estimatedPrintTimeMinutes;
        }

        public BigDecimal getMaterialVolumeMm3() {
            return materialVolumeMm3;
        }

        public BigDecimal getMaterialWeightG() {
            return materialWeightG;
        }

        public BigDecimal getEstimatedCost() {
            return estimatedCost;
        }

        public BigDecimal getSupportVolumeMm3() {
            return supportVolumeMm3;
        }

        public int getFilamentLengthMm() {
            return filamentLengthMm;
        }
    }
}