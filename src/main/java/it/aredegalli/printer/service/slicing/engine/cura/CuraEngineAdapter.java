package it.aredegalli.printer.service.slicing.engine.cura;

import it.aredegalli.printer.dto.storage.UploadResult;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.resource.FileResource;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.result.SlicingResult;
import it.aredegalli.printer.repository.resource.FileResourceRepository;
import it.aredegalli.printer.repository.slicing.result.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngine;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Complete CuraEngine adapter with improved error handling, timeouts, and WebSocket integration
 */
@Component("curaEngineAdapter")
@RequiredArgsConstructor
public class CuraEngineAdapter implements SlicingEngine {

    private final FileResourceService fileResourceService;
    private final StorageService storageService;
    private final FileResourceRepository fileResourceRepository;
    private final SlicingResultRepository slicingResultRepository;
    private final LogService logService;

    @Qualifier("slicingRestTemplate")
    private final RestTemplate restTemplate;

    @Qualifier("healthCheckRestTemplate")
    private final RestTemplate healthCheckRestTemplate;

    @Value("${slicing.engines.external.service-url}")
    private String curaServiceUrl;

    @Value("${slicing.engines.external.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${slicing.engines.external.connection-timeout-seconds:30}")
    private int connectionTimeoutSeconds;

    // Generic build volume - can be overridden by printer at print time
    @Value("${slicing.default.build-volume.width:200}")
    private int defaultBuildVolumeWidth;

    @Value("${slicing.default.build-volume.depth:200}")
    private int defaultBuildVolumeDepth;

    @Value("${slicing.default.build-volume.height:200}")
    private int defaultBuildVolumeHeight;

    // Quality profiles for consistent layer height mapping
    private static final Map<String, QualityProfile> QUALITY_PROFILES = Map.of(
            "draft", new QualityProfile("Draft", 0.3f, 80f, 15f, 2),
            "standard", new QualityProfile("Standard", 0.2f, 50f, 20f, 2),
            "high", new QualityProfile("High", 0.15f, 40f, 25f, 3),
            "ultra", new QualityProfile("Ultra", 0.1f, 30f, 30f, 4)
    );

    @Override
    public SlicingResult slice(Model model, SlicingProperty properties) {
        logService.info("CuraEngineAdapter",
                String.format("Starting slicing for model: %s (size: %.2f MB)",
                        model.getName(), model.getFileResource().getFileSize() / 1024.0 / 1024.0));

        Instant startTime = Instant.now();

        try {
            // 1. Validate model before processing
            if (!validateModel(model)) {
                throw new SlicingException("Model validation failed");
            }

            // 2. Check service health before starting
            if (!checkServiceHealth()) {
                throw new SlicingException("CuraEngine service is not available");
            }

            // 3. Download STL file
            byte[] stlBytes = downloadSTLBytes(model);
            logService.debug("CuraEngineAdapter",
                    String.format("Downloaded STL: %.2f MB", stlBytes.length / 1024.0 / 1024.0));

            // 4. Build slicing parameters
            SlicingParameters params = buildSlicingParameters(properties);

            // 5. Call CuraEngine API with retry logic
            String gcode = callCuraEngineAPIWithRetry(stlBytes, model.getId().toString(), params);

            // 6. Validate G-code output
            validateGcode(gcode);

            // 7. Save and return result
            SlicingResult result = createSlicingResult(gcode, model, properties);

            Duration processingTime = Duration.between(startTime, Instant.now());
            logService.info("CuraEngineAdapter",
                    String.format("Slicing completed successfully in %d seconds. G-code: %d lines",
                            processingTime.getSeconds(), gcode.lines().count()));

            return result;

        } catch (SlicingException e) {
            logService.error("CuraEngineAdapter", "Slicing failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "Unexpected slicing error: " + e.getMessage());
            throw new SlicingException("CuraEngine slicing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Enhanced API call with retry logic and better error handling
     */
    private String callCuraEngineAPIWithRetry(byte[] stlBytes, String modelName, SlicingParameters params) {
        int maxRetries = 3;
        int baseDelayMs = 5000; // 5 seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logService.info("CuraEngineAdapter",
                        String.format("Calling CuraEngine API (attempt %d/%d): %s/slice",
                                attempt, maxRetries, curaServiceUrl));

                return callCuraEngineAPI(stlBytes, modelName, params);

            } catch (ResourceAccessException e) {
                // Network/timeout errors
                if (attempt == maxRetries) {
                    throw new SlicingException("CuraEngine API timeout after " + maxRetries + " attempts: " + e.getMessage());
                }

                int delayMs = baseDelayMs * attempt;
                logService.warn("CuraEngineAdapter",
                        String.format("API call failed (attempt %d/%d), retrying in %d seconds: %s",
                                attempt, maxRetries, delayMs / 1000, e.getMessage()));

                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SlicingException("Slicing interrupted");
                }

            } catch (RestClientException e) {
                // Other REST errors
                throw new SlicingException("CuraEngine API error: " + e.getMessage());
            }
        }

        throw new SlicingException("CuraEngine API failed after all retry attempts");
    }

    private String callCuraEngineAPI(byte[] stlBytes, String modelName, SlicingParameters params) {
        String url = curaServiceUrl + "/slice";

        // Prepare form-data request
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        // Add STL file
        ByteArrayResource stlResource = new ByteArrayResource(stlBytes) {
            @Override
            public String getFilename() {
                return modelName.endsWith(".stl") ? modelName : modelName + ".stl";
            }
        };
        body.add("uploaded_file", stlResource);

        // Add all slicing parameters
        addSlicingParameters(body, params);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("User-Agent", "PrinterApplication-SlicingService/1.0");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        logService.info("CuraEngineAdapter",
                String.format("Calling CuraEngine API: %s with %d parameters (timeout: %ds)",
                        url, body.size(), timeoutSeconds));

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String gcode = response.getBody();
                logService.info("CuraEngineAdapter",
                        String.format("Received G-code: %d lines, %.2f MB",
                                gcode.lines().count(), gcode.length() / 1024.0 / 1024.0));
                return gcode;
            } else {
                throw new SlicingException("CuraEngine HTTP error: " + response.getStatusCode());
            }
        } catch (ResourceAccessException e) {
            // This includes timeout and connection errors
            if (e.getMessage().contains("Read timed out")) {
                throw new SlicingException("CuraEngine API read timeout after " + timeoutSeconds + " seconds");
            } else if (e.getMessage().contains("Connection reset")) {
                throw new SlicingException("CuraEngine API connection reset - service may be overloaded");
            } else {
                throw new SlicingException("CuraEngine API connection error: " + e.getMessage());
            }
        }
    }

    /**
     * Check if CuraEngine service is available
     */
    private boolean checkServiceHealth() {
        try {
            ResponseEntity<Map> response = healthCheckRestTemplate.getForEntity(
                    curaServiceUrl + "/config", Map.class);

            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            logService.debug("CuraEngineAdapter", "Service health check: " + (isHealthy ? "OK" : "FAILED"));
            return isHealthy;

        } catch (Exception e) {
            logService.warn("CuraEngineAdapter", "Service health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate G-code output
     */
    private void validateGcode(String gcode) {
        if (gcode == null || gcode.trim().isEmpty()) {
            throw new SlicingException("Received empty G-code from CuraEngine");
        }

        long lineCount = gcode.lines().count();
        if (lineCount < 10) {
            throw new SlicingException("G-code too short, only " + lineCount + " lines");
        }

        // Check for basic G-code structure
        if (!gcode.contains("G1") && !gcode.contains("G0")) {
            throw new SlicingException("Invalid G-code: no movement commands found");
        }

        logService.debug("CuraEngineAdapter", "G-code validation passed: " + lineCount + " lines");
    }

    @Override
    public boolean validateModel(Model model) {
        if (model == null || model.getFileResource() == null) {
            logService.warn("CuraEngineAdapter", "Model or file resource is null");
            return false;
        }

        FileResource fileResource = model.getFileResource();
        String fileType = fileResource.getFileType();
        long fileSize = fileResource.getFileSize();

        // File type validation
        if (fileType == null || (!fileType.toLowerCase().contains("stl") &&
                !fileType.toLowerCase().contains("model"))) {
            logService.warn("CuraEngineAdapter", "Unsupported file type: " + fileType);
            return false;
        }

        // File size validation (min 1KB, max 100MB)
        if (fileSize <= 1024 || fileSize > 100 * 1024 * 1024) {
            logService.warn("CuraEngineAdapter", "Invalid file size: " + fileSize + " bytes");
            return false;
        }

        logService.debug("CuraEngineAdapter", "Model validation passed");
        return true;
    }

    @Override
    public String getName() {
        return "CuraEngine Generic";
    }

    @Override
    public String getVersion() {
        try {
            ResponseEntity<Map> response = healthCheckRestTemplate.getForEntity(
                    curaServiceUrl + "/config", Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> config = response.getBody();
                return config.getOrDefault("version", "5.6.0-Generic").toString();
            }
        } catch (Exception e) {
            logService.debug("CuraEngineAdapter", "Could not get version: " + e.getMessage());
        }
        return "5.6.0-Generic";
    }

    // ======================================
    // GENERIC CONFIGURATION METHODS
    // ======================================

    /**
     * Builds slicing parameters using only SlicingProperty data
     * No printer-specific configurations - generic G-code generation
     */
    private SlicingParameters buildSlicingParameters(SlicingProperty properties) {
        SlicingParameters params = new SlicingParameters();

        // Use generic build volume - printer compatibility checked at print time
        params.machineWidth = defaultBuildVolumeWidth;
        params.machineDepth = defaultBuildVolumeDepth;
        params.machineHeight = defaultBuildVolumeHeight;

        // Apply quality profile defaults if available
        QualityProfile qualityProfile = getQualityProfile(properties.getQualityProfile());
        if (qualityProfile != null) {
            params.layerHeight = qualityProfile.layerHeight;
            params.speedPrint = qualityProfile.printSpeed;
            params.infillDensity = qualityProfile.infillDensity;
            params.wallLineCount = qualityProfile.wallCount;
        }

        // Override with specific properties
        applySlicingPropertyOverrides(params, properties);

        // Calculate derived parameters
        calculateDerivedParameters(params);

        logService.info("CuraEngineAdapter",
                String.format("Generic slicing config: Layer: %.2fmm, Speed: %.0fmm/s, Infill: %.0f%%",
                        params.layerHeight, params.speedPrint, params.infillDensity));

        return params;
    }

    private QualityProfile getQualityProfile(String qualityProfileName) {
        if (qualityProfileName == null) {
            return QUALITY_PROFILES.get("standard");
        }
        return QUALITY_PROFILES.get(qualityProfileName.toLowerCase());
    }

    /**
     * Applies all SlicingProperty values to parameters
     * Handles BigDecimal and Boolean conversions properly
     */
    private void applySlicingPropertyOverrides(SlicingParameters params, SlicingProperty properties) {
        // Layer settings
        if (properties.getLayerHeightMm() != null) {
            params.layerHeight = bigDecimalToFloat(properties.getLayerHeightMm());
        }
        if (properties.getLineWidthMm() != null) {
            params.lineWidth = bigDecimalToFloat(properties.getLineWidthMm());
        }

        // Speed settings (mm/s)
        if (properties.getPrintSpeedMmS() != null) {
            params.speedPrint = bigDecimalToFloat(properties.getPrintSpeedMmS());
        }
        if (properties.getTravelSpeedMmS() != null) {
            params.speedTravel = bigDecimalToFloat(properties.getTravelSpeedMmS());
        }
        if (properties.getFirstLayerSpeedMmS() != null) {
            params.speedFirstLayer = bigDecimalToFloat(properties.getFirstLayerSpeedMmS());
        }
        if (properties.getInfillSpeedMmS() != null) {
            params.speedInfill = bigDecimalToFloat(properties.getInfillSpeedMmS());
        }
        if (properties.getOuterWallSpeedMmS() != null) {
            params.speedWallOuter = bigDecimalToFloat(properties.getOuterWallSpeedMmS());
        }
        if (properties.getInnerWallSpeedMmS() != null) {
            params.speedWallInner = bigDecimalToFloat(properties.getInnerWallSpeedMmS());
        }
        if (properties.getTopBottomSpeedMmS() != null) {
            params.speedTopBottom = bigDecimalToFloat(properties.getTopBottomSpeedMmS());
        }

        // Infill settings
        if (properties.getInfillPercentage() != null) {
            params.infillDensity = bigDecimalToFloat(properties.getInfillPercentage());
        }
        if (properties.getInfillPattern() != null) {
            params.infillPattern = properties.getInfillPattern();
        }

        // Shell settings
        if (properties.getPerimeterCount() != null) {
            params.wallLineCount = properties.getPerimeterCount();
        }
        if (properties.getTopSolidLayers() != null) {
            params.topLayers = properties.getTopSolidLayers();
        }
        if (properties.getBottomSolidLayers() != null) {
            params.bottomLayers = properties.getBottomSolidLayers();
        }
        if (properties.getTopBottomThicknessMm() != null) {
            params.topBottomThickness = bigDecimalToFloat(properties.getTopBottomThicknessMm());
        }

        // Support settings
        if (properties.getSupportsEnabled() != null) {
            params.supportEnabled = properties.getSupportsEnabled();
        }
        if (properties.getSupportAngleThreshold() != null) {
            params.supportAngle = bigDecimalToFloat(properties.getSupportAngleThreshold());
        }
        if (properties.getSupportDensityPercentage() != null) {
            params.supportDensity = bigDecimalToFloat(properties.getSupportDensityPercentage());
        }
        if (properties.getSupportPattern() != null) {
            params.supportPattern = properties.getSupportPattern();
        }
        if (properties.getSupportZDistanceMm() != null) {
            params.supportZDistance = bigDecimalToFloat(properties.getSupportZDistanceMm());
        }

        // Adhesion settings
        if (properties.getAdhesionType() != null) {
            params.adhesionType = properties.getAdhesionType();
        }
        if (properties.getBrimEnabled() != null) {
            params.brimEnabled = properties.getBrimEnabled();
        }
        if (properties.getBrimWidthMm() != null) {
            params.brimWidth = bigDecimalToFloat(properties.getBrimWidthMm());
        }

        // Cooling settings
        if (properties.getFanEnabled() != null) {
            params.fanEnabled = properties.getFanEnabled();
        }
        if (properties.getFanSpeedPercentage() != null) {
            params.fanSpeed = bigDecimalToFloat(properties.getFanSpeedPercentage());
        }

        // Retraction settings
        if (properties.getRetractionEnabled() != null) {
            params.retractionEnabled = properties.getRetractionEnabled();
        }
        if (properties.getRetractionDistanceMm() != null) {
            params.retractionDistance = bigDecimalToFloat(properties.getRetractionDistanceMm());
        }
        if (properties.getZhopEnabled() != null) {
            params.zhopEnabled = properties.getZhopEnabled();
        }
        if (properties.getZhopHeightMm() != null) {
            params.zhopHeight = bigDecimalToFloat(properties.getZhopHeightMm());
        }

        // Temperature settings (optional - can be overridden by material/printer)
        if (properties.getExtruderTempC() != null) {
            params.printTemperature = properties.getExtruderTempC();
        }
        if (properties.getBedTempC() != null) {
            params.bedTemperature = properties.getBedTempC();
        }
    }

    private void calculateDerivedParameters(SlicingParameters params) {
        // Calculate wall thickness from line count and width
        params.wallThickness = params.wallLineCount * params.lineWidth;

        // Auto-calculate top/bottom thickness if not specified
        if (params.topBottomThickness <= 0) {
            params.topBottomThickness = Math.max(0.8f, params.layerHeight * 4f);
        }

        // Adjust speeds for very fine layers
        if (params.layerHeight <= 0.1f) {
            params.speedPrint *= 0.7f; // Slower for fine layers
            params.speedTravel *= 0.8f;
        }

        // Default temperature fallbacks if not set
        if (params.printTemperature <= 0) {
            params.printTemperature = 210; // Default PLA temperature
        }
        if (params.bedTemperature < 0) {
            params.bedTemperature = 60; // Default heated bed
        }
    }

    // ======================================
    // API CALL METHODS
    // ======================================

    private byte[] downloadSTLBytes(Model model) throws Exception {
        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
            return stlStream.readAllBytes();
        }
    }

    private void addSlicingParameters(MultiValueMap<String, Object> body, SlicingParameters params) {
        // Generic machine settings (printer-independent)
        body.add("machine_width", params.machineWidth);
        body.add("machine_depth", params.machineDepth);
        body.add("machine_height", params.machineHeight);

        // Quality settings
        body.add("layer_height", params.layerHeight);
        body.add("line_width", params.lineWidth);
        body.add("wall_line_count", params.wallLineCount);
        body.add("wall_thickness", params.wallThickness);
        body.add("top_bottom_thickness", params.topBottomThickness);
        body.add("top_layers", params.topLayers);
        body.add("bottom_layers", params.bottomLayers);

        // Infill settings
        body.add("infill_sparse_density", params.infillDensity);
        body.add("infill_pattern", params.infillPattern);

        // Speed settings
        body.add("speed_print", params.speedPrint);
        body.add("speed_travel", params.speedTravel);
        body.add("speed_layer_0", params.speedFirstLayer);
        body.add("speed_infill", params.speedInfill);
        body.add("speed_wall_0", params.speedWallOuter);
        body.add("speed_wall_x", params.speedWallInner);
        body.add("speed_topbottom", params.speedTopBottom);

        // Temperature settings (generic - can be overridden by printer)
        body.add("material_print_temperature", params.printTemperature);
        body.add("material_bed_temperature", params.bedTemperature);

        // Support settings
        body.add("support_enable", params.supportEnabled);
        if (params.supportEnabled) {
            body.add("support_density", params.supportDensity);
            body.add("support_angle", params.supportAngle);
            body.add("support_z_distance", params.supportZDistance);
            body.add("support_pattern", params.supportPattern);
        }

        // Adhesion settings
        body.add("adhesion_type", determineAdhesionType(params));
        if (params.brimEnabled) {
            body.add("brim_width", params.brimWidth);
        }

        // Cooling settings
        body.add("cool_fan_enabled", params.fanEnabled);
        body.add("cool_fan_speed", params.fanSpeed);

        // Retraction settings
        body.add("retraction_enable", params.retractionEnabled);
        if (params.retractionEnabled) {
            body.add("retraction_amount", params.retractionDistance);
            body.add("retraction_hop_enabled", params.zhopEnabled);
            if (params.zhopEnabled) {
                body.add("retraction_hop", params.zhopHeight);
            }
        }

        logService.debug("CuraEngineAdapter",
                String.format("Generic parameters: Layer: %.2fmm, Infill: %.0f%%, Speed: %.0fmm/s",
                        params.layerHeight, params.infillDensity, params.speedPrint));
    }

    private String determineAdhesionType(SlicingParameters params) {
        if (params.brimEnabled) {
            return "brim";
        }

        switch (params.adhesionType.toLowerCase()) {
            case "brim":
                return "brim";
            case "raft":
                return "raft";
            case "skirt":
                return "skirt";
            default:
                return "skirt"; // Safe default
        }
    }

    private SlicingResult createSlicingResult(String gcode, Model model, SlicingProperty properties)
            throws Exception {
        byte[] gcodeBytes = gcode.getBytes();

        // Save G-code to storage
        UploadResult uploadResult = storageService.upload(
                new ByteArrayInputStream(gcodeBytes),
                gcodeBytes.length,
                "text/plain",
                PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME
        );

        FileResource gcodeFile = fileResourceRepository.save(FileResource.builder()
                .fileName(model.getName() + ".gcode")
                .fileType("text/plain")
                .fileSize(gcodeBytes.length)
                .fileHash(uploadResult.getHashBytes())
                .objectKey(uploadResult.getObjectKey())
                .bucketName(PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME)
                .uploadedAt(Instant.now())
                .build());

        // Create SlicingResult
        SlicingResult result = SlicingResult.builder()
                .sourceFile(model.getFileResource())
                .generatedFile(gcodeFile)
                .slicingProperty(properties)
                .lines(gcode.lines().count())
                .createdAt(Instant.now())
                .build();

        return slicingResultRepository.save(result);
    }

    // ======================================
    // UTILITY METHODS
    // ======================================

    /**
     * Safely converts BigDecimal to float with null check
     */
    private float bigDecimalToFloat(BigDecimal value) {
        return value != null ? value.floatValue() : 0.0f;
    }

    // ======================================
    // CONFIGURATION CLASSES
    // ======================================

    private static class SlicingParameters {
        // Generic machine (build volume independent)
        int machineWidth = 200;
        int machineDepth = 200;
        int machineHeight = 200;

        // Quality settings
        float layerHeight = 0.2f;
        float lineWidth = 0.4f;
        int wallLineCount = 2;
        float wallThickness = 0.8f;
        float topBottomThickness = 0.8f;
        int topLayers = 3;
        int bottomLayers = 3;

        // Infill settings
        float infillDensity = 20f;
        String infillPattern = "grid";

        // Speed settings (mm/s)
        float speedPrint = 50f;
        float speedTravel = 150f;
        float speedFirstLayer = 20f;
        float speedInfill = 50f;
        float speedWallOuter = 50f;
        float speedWallInner = 50f;
        float speedTopBottom = 50f;

        // Temperature settings (generic defaults)
        int printTemperature = 210;
        int bedTemperature = 60;

        // Support settings
        boolean supportEnabled = false;
        float supportDensity = 20f;
        float supportAngle = 45f;
        float supportZDistance = 0.2f;
        String supportPattern = "grid";

        // Adhesion settings
        String adhesionType = "skirt";
        boolean brimEnabled = false;
        float brimWidth = 5f;

        // Cooling settings
        boolean fanEnabled = true;
        float fanSpeed = 100f;

        // Retraction settings
        boolean retractionEnabled = true;
        float retractionDistance = 1f;
        boolean zhopEnabled = false;
        float zhopHeight = 0.2f;
    }

    private static class QualityProfile {
        final String name;
        final float layerHeight, printSpeed, infillDensity;
        final int wallCount;

        QualityProfile(String name, float layerHeight, float printSpeed, float infillDensity, int wallCount) {
            this.name = name;
            this.layerHeight = layerHeight;
            this.printSpeed = printSpeed;
            this.infillDensity = infillDensity;
            this.wallCount = wallCount;
        }
    }

    /**
     * Custom exception for slicing errors
     */
    public static class SlicingException extends RuntimeException {
        public SlicingException(String message) {
            super(message);
        }

        public SlicingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}