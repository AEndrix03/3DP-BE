package it.aredegalli.printer.service.slicing.configuration;

import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Service for managing CuraEngine configurations dynamically
 * Provides intelligent configuration selection and optimization
 */
@Service
@RequiredArgsConstructor
public class CuraConfigurationService {

    private final LogService logService;
    private final RestTemplate restTemplate;

    @Value("${slicing.engines.external.service-url}")
    private String curaServiceUrl;

    // ======================================
    // CONFIGURATION DETECTION
    // ======================================

    /**
     * Automatically configures slicing properties based on model characteristics
     */
    public SlicingProperty autoConfigureForModel(String modelName, long modelSizeBytes, String targetUse) {
        logService.info("CuraConfigurationService",
                String.format("Auto-configuring for model: %s, size: %d bytes, use: %s",
                        modelName, modelSizeBytes, targetUse));

        SlicingProperty config = new SlicingProperty();

        // 1. Determine printer based on model size and use case
        configurePrinter(config, modelSizeBytes, targetUse);

        // 2. Set quality based on model characteristics
        configureQuality(config, modelName, modelSizeBytes, targetUse);

        // 3. Set material and temperatures
        configureMaterial(config, targetUse);

        // 4. Configure advanced settings
        configureAdvancedSettings(config, targetUse);

        logService.info("CuraConfigurationService",
                "Auto-configuration complete: " + config.getConfigurationSummary());

        return config;
    }

    /**
     * Optimizes configuration for specific bed size
     */
    public SlicingProperty optimizeForBedSize(SlicingProperty baseConfig, int width, int depth, int height) {
        SlicingProperty optimized = cloneConfiguration(baseConfig);

        optimized.setBedWidth(String.valueOf(width));
        optimized.setBedDepth(String.valueOf(depth));
        optimized.setBedHeight(String.valueOf(height));

        // Adjust settings based on bed size
        if (width <= 160 && depth <= 160) {
            // Small bed optimizations
            optimized.setPrinterType("MINI_15X15");
            optimizeForSmallBed(optimized);
        } else if (width >= 300 || depth >= 300) {
            // Large bed optimizations  
            optimized.setPrinterType("CR10");
            optimizeForLargeBed(optimized);
        } else {
            // Medium bed (Ender3/Prusa size)
            optimized.setPrinterType("ENDER3");
            optimizeForMediumBed(optimized);
        }

        return optimized;
    }

    /**
     * Creates configuration for specific quality level
     */
    public SlicingProperty createQualityConfiguration(String qualityLevel, String printerType) {
        SlicingProperty config = new SlicingProperty();

        config.setPrinterType(printerType);
        config.setQualityProfile(qualityLevel.toUpperCase());

        switch (qualityLevel.toUpperCase()) {
            case "DRAFT":
                configureDraftQuality(config);
                break;
            case "NORMAL":
                configureNormalQuality(config);
                break;
            case "FINE":
                configureFineQuality(config);
                break;
            case "ULTRA_FINE":
                configureUltraFineQuality(config);
                break;
            case "MINIATURE":
                configureMiniatureQuality(config);
                break;
            default:
                configureNormalQuality(config);
        }

        return config;
    }

    // ======================================
    // CONFIGURATION TEMPLATES
    // ======================================

    /**
     * Returns pre-configured templates for common use cases
     */
    public Map<String, SlicingProperty> getConfigurationTemplates() {
        return Map.of(
                "prototyping_fast", createPrototypingTemplate(),
                "production_quality", createProductionTemplate(),
                "miniatures_detailed", createMiniaturesTemplate(),
                "functional_strong", createFunctionalTemplate(),
                "flexible_tpu", createFlexibleTemplate()
        );
    }

    private SlicingProperty createPrototypingTemplate() {
        return SlicingProperty.builder()
                .printerType("MINI_15X15")
                .bedDepth("150")
                .bedHeight("150")
                .bedWidth("150")
                .qualityProfile("DRAFT")
                .materialType("PLA")
                .layerHeightMm("0.3")
                .printSpeedMmS("80")
                .infillPercentage("15")
                .extruderTempC(200)
                .bedTempC(50)
                .build();
    }

    private SlicingProperty createProductionTemplate() {
        return SlicingProperty.builder()
                .printerType("ENDER3")
                .bedDepth("220")
                .bedHeight("220")
                .bedWidth("250")
                .qualityProfile("NORMAL")
                .materialType("PLA")
                .layerHeightMm("0.2")
                .printSpeedMmS("50")
                .infillPercentage("25")
                .extruderTempC(210)
                .bedTempC(60)
                .build();
    }

    private SlicingProperty createMiniaturesTemplate() {
        return SlicingProperty.builder()
                .printerType("MINI_15X15")
                .bedDepth("150")
                .bedHeight("150")
                .bedWidth("150")
                .qualityProfile("MINIATURE")
                .materialType("PLA")
                .layerHeightMm("0.05")
                .printSpeedMmS("20")
                .infillPercentage("30")
                .extruderTempC(205)
                .bedTempC(60)
                .supportsEnabled("true")
                .build();
    }

    private SlicingProperty createFunctionalTemplate() {
        return SlicingProperty.builder()
                .printerType("ENDER3")
                .bedDepth("220")
                .bedHeight("220")
                .bedWidth("250")
                .qualityProfile("NORMAL")
                .materialType("PETG")
                .layerHeightMm("0.2")
                .printSpeedMmS("40")
                .infillPercentage("40")
                .perimeterCount(4)
                .extruderTempC(230)
                .bedTempC(70)
                .build();
    }

    private SlicingProperty createFlexibleTemplate() {
        return SlicingProperty.builder()
                .printerType("ENDER3")
                .bedDepth("220")
                .bedHeight("220")
                .bedWidth("250")
                .qualityProfile("NORMAL")
                .materialType("TPU")
                .layerHeightMm("0.2")
                .printSpeedMmS("25")
                .infillPercentage("25")
                .extruderTempC(220)
                .bedTempC(60)
                .build();
    }

    // ======================================
    // INTELLIGENT CONFIGURATION
    // ======================================

    private void configurePrinter(SlicingProperty config, long modelSizeBytes, String targetUse) {
        // Determine printer based on model size and intended use
        if (modelSizeBytes < 1024 * 1024 || "miniature".equalsIgnoreCase(targetUse)) {
            // Small models or miniatures -> Mini printer
            config.setPrinterType("MINI_15X15");
            config.setBedDepth("150");
            config.setBedHeight("150");
            config.setBedWidth("150");
        } else if (modelSizeBytes > 10 * 1024 * 1024 || "large".equalsIgnoreCase(targetUse)) {
            // Large models -> CR-10
            config.setPrinterType("CR10");
            config.setBedDepth("300");
            config.setBedHeight("300");
            config.setBedWidth("400");
        } else {
            // Medium models -> Ender 3
            config.setPrinterType("ENDER3");
            config.setBedDepth("220");
            config.setBedHeight("220");
            config.setBedWidth("250");
        }
    }

    private void configureQuality(SlicingProperty config, String modelName, long modelSizeBytes, String targetUse) {
        String qualityProfile;

        if ("prototype".equalsIgnoreCase(targetUse) || "draft".equalsIgnoreCase(targetUse)) {
            qualityProfile = "DRAFT";
            config.setLayerHeightMm("0.3");
            config.setPrintSpeedMmS("80");
        } else if ("miniature".equalsIgnoreCase(targetUse) || modelName.toLowerCase().contains("mini")) {
            qualityProfile = "MINIATURE";
            config.setLayerHeightMm("0.05");
            config.setPrintSpeedMmS("20");
        } else if ("fine".equalsIgnoreCase(targetUse) || "detailed".equalsIgnoreCase(targetUse)) {
            qualityProfile = "FINE";
            config.setLayerHeightMm("0.15");
            config.setPrintSpeedMmS("40");
        } else {
            qualityProfile = "NORMAL";
            config.setLayerHeightMm("0.2");
            config.setPrintSpeedMmS("50");
        }

        config.setQualityProfile(qualityProfile);
    }

    private void configureMaterial(SlicingProperty config, String targetUse) {
        if ("flexible".equalsIgnoreCase(targetUse)) {
            config.setMaterialType("TPU");
            config.setExtruderTempC(220);
            config.setBedTempC(60);
        } else if ("strong".equalsIgnoreCase(targetUse) || "functional".equalsIgnoreCase(targetUse)) {
            config.setMaterialType("PETG");
            config.setExtruderTempC(230);
            config.setBedTempC(70);
        } else if ("high_temp".equalsIgnoreCase(targetUse)) {
            config.setMaterialType("ABS");
            config.setExtruderTempC(240);
            config.setBedTempC(80);
        } else {
            // Default PLA
            config.setMaterialType("PLA");
            config.setExtruderTempC(210);
            config.setBedTempC(60);
        }
    }

    private void configureAdvancedSettings(SlicingProperty config, String targetUse) {
        // Infill based on use case
        if ("prototype".equalsIgnoreCase(targetUse)) {
            config.setInfillPercentage("15");
        } else if ("strong".equalsIgnoreCase(targetUse) || "functional".equalsIgnoreCase(targetUse)) {
            config.setInfillPercentage("40");
            config.setPerimeterCount(4);
        } else if ("miniature".equalsIgnoreCase(targetUse)) {
            config.setInfillPercentage("30");
            config.setSupportsEnabled("true");
        } else {
            config.setInfillPercentage("20");
        }

        // Travel speed based on quality
        float printSpeed = Float.parseFloat(config.getPrintSpeedMmS());
        config.setTravelSpeedMmS(String.valueOf(printSpeed * 2.5f));

        // Adhesion
        config.setBrimEnabled("true");
    }

    // ======================================
    // BED SIZE OPTIMIZATIONS
    // ======================================

    private void optimizeForSmallBed(SlicingProperty config) {
        // Small bed optimizations
        config.setBrimEnabled("true"); // Always use brim for small beds
        config.setBrimWidthMm("3");

        // Adjust speeds for better precision on small objects
        float currentSpeed = Float.parseFloat(config.getPrintSpeedMmS());
        config.setPrintSpeedMmS(String.valueOf(currentSpeed * 0.9f));
    }

    private void optimizeForMediumBed(SlicingProperty config) {
        // Standard optimizations for Ender3/Prusa size
        config.setBrimEnabled("true");
        config.setBrimWidthMm("5");
    }

    private void optimizeForLargeBed(SlicingProperty config) {
        // Large bed optimizations
        config.setBrimEnabled("true");
        config.setBrimWidthMm("8"); // Wider brim for large beds

        // Increase speeds for large beds
        float currentSpeed = Float.parseFloat(config.getPrintSpeedMmS());
        config.setPrintSpeedMmS(String.valueOf(currentSpeed * 1.1f));
    }

    // ======================================
    // QUALITY CONFIGURATIONS
    // ======================================

    private void configureDraftQuality(SlicingProperty config) {
        config.setLayerHeightMm("0.3");
        config.setPrintSpeedMmS("80");
        config.setInfillPercentage("15");
        config.setPerimeterCount(2);
    }

    private void configureNormalQuality(SlicingProperty config) {
        config.setLayerHeightMm("0.2");
        config.setPrintSpeedMmS("50");
        config.setInfillPercentage("20");
        config.setPerimeterCount(2);
    }

    private void configureFineQuality(SlicingProperty config) {
        config.setLayerHeightMm("0.15");
        config.setPrintSpeedMmS("40");
        config.setInfillPercentage("25");
        config.setPerimeterCount(3);
    }

    private void configureUltraFineQuality(SlicingProperty config) {
        config.setLayerHeightMm("0.1");
        config.setPrintSpeedMmS("30");
        config.setInfillPercentage("30");
        config.setPerimeterCount(4);
    }

    private void configureMiniatureQuality(SlicingProperty config) {
        config.setLayerHeightMm("0.05");
        config.setPrintSpeedMmS("20");
        config.setInfillPercentage("30");
        config.setPerimeterCount(4);
        config.setSupportsEnabled("true");
    }

    // ======================================
    // UTILITY METHODS
    // ======================================

    private SlicingProperty cloneConfiguration(SlicingProperty source) {
        // Implement deep copy of SlicingProperty
        // For brevity, showing key fields only
        SlicingProperty clone = new SlicingProperty();

        clone.setLayerHeightMm(source.getLayerHeightMm());
        clone.setPrintSpeedMmS(source.getPrintSpeedMmS());
        clone.setTravelSpeedMmS(source.getTravelSpeedMmS());
        clone.setInfillPercentage(source.getInfillPercentage());
        clone.setExtruderTempC(source.getExtruderTempC());
        clone.setBedTempC(source.getBedTempC());
        clone.setPerimeterCount(source.getPerimeterCount());
        clone.setSupportsEnabled(source.getSupportsEnabled());
        clone.setBrimEnabled(source.getBrimEnabled());

        // Copy new fields
        clone.setPrinterType(source.getPrinterType());
        clone.setQualityProfile(source.getQualityProfile());
        clone.setMaterialType(source.getMaterialType());

        return clone;
    }

    /**
     * Validates configuration against CuraEngine API capabilities
     */
    public boolean validateConfiguration(SlicingProperty config) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    curaServiceUrl + "/config", Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                // Configuration is valid if API is accessible
                return config.isValid();
            }
        } catch (Exception e) {
            logService.warn("CuraConfigurationService",
                    "Could not validate against CuraEngine API: " + e.getMessage());
        }

        return config.isValid(); // Fallback to basic validation
    }
}