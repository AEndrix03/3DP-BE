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
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
 * Enhanced CuraEngine adapter with comprehensive debug logging
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
        logService.info("CuraEngineAdapter", "=== STARTING CURA ENGINE SLICING ===");
        logService.info("CuraEngineAdapter",
                String.format("Model: %s (size: %.2f MB), Properties: %s",
                        model.getName(),
                        model.getFileResource().getFileSize() / 1024.0 / 1024.0,
                        properties.getName()));

        Instant startTime = Instant.now();

        try {
            // 1. Validate model before processing
            logService.info("CuraEngineAdapter", "STEP 1: Validating model");
            if (!validateModel(model)) {
                throw new SlicingException("Model validation failed");
            }
            logService.info("CuraEngineAdapter", "STEP 1 COMPLETED: Model validation successful");

            // 2. Check service health before starting
            logService.info("CuraEngineAdapter", "STEP 2: Checking service health");
            if (!checkServiceHealth()) {
                throw new SlicingException("CuraEngine service is not available at: " + curaServiceUrl);
            }
            logService.info("CuraEngineAdapter", "STEP 2 COMPLETED: Service health check OK");

            // 3. Download STL file
            logService.info("CuraEngineAdapter", "STEP 3: Downloading STL file");
            byte[] stlBytes = downloadSTLBytes(model);
            logService.info("CuraEngineAdapter",
                    String.format("STEP 3 COMPLETED: Downloaded STL: %.2f MB (%d bytes)",
                            stlBytes.length / 1024.0 / 1024.0, stlBytes.length));

            // 4. Build slicing parameters
            logService.info("CuraEngineAdapter", "STEP 4: Building slicing parameters");
            SlicingParameters params = buildSlicingParameters(properties);
            logService.info("CuraEngineAdapter", "STEP 4 COMPLETED: Parameters built - " +
                    String.format("Layer: %.2fmm, Speed: %.0fmm/s, Infill: %.0f%%",
                            params.layerHeight, params.speedPrint, params.infillDensity));

            // 5. Call CuraEngine API with retry logic
            logService.info("CuraEngineAdapter", "STEP 5: Calling CuraEngine API");
            String gcode = callCuraEngineAPIWithRetry(stlBytes, model.getId().toString(), params);

            if (gcode == null || gcode.trim().isEmpty()) {
                throw new SlicingException("Received empty G-code from CuraEngine");
            }

            logService.info("CuraEngineAdapter",
                    String.format("STEP 5 COMPLETED: Received G-code: %d lines, %.2f MB",
                            gcode.lines().count(), gcode.length() / 1024.0 / 1024.0));

            // 6. Validate G-code output
            logService.info("CuraEngineAdapter", "STEP 6: Validating G-code");
            validateGcode(gcode);
            logService.info("CuraEngineAdapter", "STEP 6 COMPLETED: G-code validation successful");

            // 7. Save and return result
            logService.info("CuraEngineAdapter", "STEP 7: Creating slicing result");
            SlicingResult result = createSlicingResult(gcode, model, properties);

            Duration processingTime = Duration.between(startTime, Instant.now());
            logService.info("CuraEngineAdapter",
                    String.format("=== CURA ENGINE SLICING COMPLETED SUCCESSFULLY ===\n" +
                                    "Processing time: %d seconds\n" +
                                    "G-code lines: %d\n" +
                                    "Result ID: %s",
                            processingTime.getSeconds(), result.getLines(), result.getId()));

            return result;

        } catch (SlicingException e) {
            logService.error("CuraEngineAdapter", "SLICING FAILED: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "UNEXPECTED SLICING ERROR: " + e.getMessage());
            logService.error("CuraEngineAdapter", "STACK TRACE: ", java.util.Map.of("exception", e));
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
                        String.format("API CALL ATTEMPT %d/%d: %s/slice (timeout: %ds)",
                                attempt, maxRetries, curaServiceUrl, timeoutSeconds));

                String result = callCuraEngineAPI(stlBytes, modelName, params);

                logService.info("CuraEngineAdapter", "API CALL SUCCESSFUL on attempt " + attempt);
                return result;

            } catch (ResourceAccessException e) {
                // Network/timeout errors
                logService.error("CuraEngineAdapter",
                        String.format("API CALL FAILED (attempt %d/%d): %s", attempt, maxRetries, e.getMessage()));

                if (attempt == maxRetries) {
                    throw new SlicingException("CuraEngine API timeout after " + maxRetries + " attempts: " + e.getMessage());
                }

                int delayMs = baseDelayMs * attempt;
                logService.warn("CuraEngineAdapter",
                        String.format("Retrying in %d seconds...", delayMs / 1000));

                try {
                    TimeUnit.MILLISECONDS.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SlicingException("Slicing interrupted");
                }

            } catch (RestClientException e) {
                // Other REST errors
                logService.error("CuraEngineAdapter", "REST CLIENT ERROR: " + e.getMessage());
                throw new SlicingException("CuraEngine API error: " + e.getMessage());
            }
        }

        throw new SlicingException("CuraEngine API failed after all retry attempts");
    }

    private String callCuraEngineAPI(byte[] stlBytes, String modelName, SlicingParameters params) {
        String url = curaServiceUrl + "/slice";

        // Configura RestTemplate con timeout di 5 minuti
        RestTemplate customRestTemplate = createRestTemplateWithTimeout(300); // 300 secondi = 5 minuti

        logService.info("CuraEngineAdapter", "PREPARING API CALL to: " + url);

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

        logService.info("CuraEngineAdapter", String.format(
                "SENDING REQUEST: URL=%s, Parameters=%d, STL_Size=%.2fMB, Timeout=%ds",
                url, body.size(), stlBytes.length / 1024.0 / 1024.0, 300));

        try {
            Instant callStart = Instant.now();
            ResponseEntity<String> response = customRestTemplate.postForEntity(url, requestEntity, String.class);
            Duration callTime = Duration.between(callStart, Instant.now());

            logService.info("CuraEngineAdapter", String.format(
                    "API RESPONSE: Status=%s, Time=%ds, Content-Length=%d",
                    response.getStatusCode(), callTime.getSeconds(),
                    response.getBody() != null ? response.getBody().length() : 0));

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String gcode = response.getBody();
                logService.info("CuraEngineAdapter",
                        String.format("SUCCESS: Received G-code: %d lines, %.2f MB",
                                gcode.lines().count(), gcode.length() / 1024.0 / 1024.0));
                return gcode;
            } else {
                throw new SlicingException("CuraEngine HTTP error: " + response.getStatusCode());
            }

        } catch (ResourceAccessException e) {
            // This includes timeout and connection errors
            if (e.getMessage().contains("Read timed out")) {
                logService.error("CuraEngineAdapter", "API READ TIMEOUT after 5 minutes");
                throw new SlicingException("CuraEngine API read timeout after 5 minutes");
            } else if (e.getMessage().contains("Connection reset")) {
                logService.error("CuraEngineAdapter", "API CONNECTION RESET - service may be overloaded");
                throw new SlicingException("CuraEngine API connection reset - service may be overloaded");
            } else {
                logService.error("CuraEngineAdapter", "API CONNECTION ERROR: " + e.getMessage());
                throw new SlicingException("CuraEngine API connection error: " + e.getMessage());
            }
        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "UNEXPECTED API ERROR: " + e.getMessage());
            throw new SlicingException("Unexpected CuraEngine API error: " + e.getMessage());
        }
    }

    /**
     * Check if CuraEngine service is available
     */
    private boolean checkServiceHealth() {
        try {
            String healthUrl = curaServiceUrl + "/config";
            logService.info("CuraEngineAdapter", "HEALTH CHECK: " + healthUrl);

            ResponseEntity<Map> response = healthCheckRestTemplate.getForEntity(healthUrl, Map.class);

            boolean isHealthy = response.getStatusCode() == HttpStatus.OK;
            logService.info("CuraEngineAdapter", "HEALTH CHECK RESULT: " +
                    (isHealthy ? "HEALTHY" : "UNHEALTHY") + " - Status: " + response.getStatusCode());

            if (isHealthy && response.getBody() != null) {
                logService.info("CuraEngineAdapter", "SERVICE INFO: " + response.getBody());
            }

            return isHealthy;

        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "HEALTH CHECK FAILED: " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate G-code output
     */
    private void validateGcode(String gcode) {
        logService.info("CuraEngineAdapter", "VALIDATING G-CODE...");

        if (gcode == null || gcode.trim().isEmpty()) {
            throw new SlicingException("Received empty G-code from CuraEngine");
        }

        long lineCount = gcode.lines().count();
        if (lineCount < 10) {
            throw new SlicingException("G-code too short, only " + lineCount + " lines");
        }

        // Check for basic G-code structure
        boolean hasMovement = gcode.contains("G1") || gcode.contains("G0");
        boolean hasExtrusion = gcode.contains("E");

        if (!hasMovement) {
            throw new SlicingException("Invalid G-code: no movement commands found");
        }

        logService.info("CuraEngineAdapter", String.format(
                "G-CODE VALIDATION PASSED: %d lines, Movement: %s, Extrusion: %s",
                lineCount, hasMovement, hasExtrusion));
    }

    @Override
    public boolean validateModel(Model model) {
        logService.info("CuraEngineAdapter", "VALIDATING MODEL: " + model.getName());

        if (model == null || model.getFileResource() == null) {
            logService.error("CuraEngineAdapter", "VALIDATION FAILED: Model or file resource is null");
            return false;
        }

        FileResource fileResource = model.getFileResource();
        String fileType = fileResource.getFileType();
        long fileSize = fileResource.getFileSize();

        logService.info("CuraEngineAdapter", String.format(
                "MODEL INFO: Type=%s, Size=%d bytes (%.2f MB)",
                fileType, fileSize, fileSize / 1024.0 / 1024.0));

        if (fileSize > 100 * 1024 * 1024) {
            logService.error("CuraEngineAdapter", "VALIDATION FAILED: Invalid file size: " + fileSize + " bytes");
            return false;
        }

        logService.info("CuraEngineAdapter", "MODEL VALIDATION PASSED");
        return true;
    }

    @Override
    public String getName() {
        return "CuraEngine";
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
    // CONFIGURATION METHODS
    // ======================================

    private SlicingParameters buildSlicingParameters(SlicingProperty properties) {
        logService.info("CuraEngineAdapter", "BUILDING SLICING PARAMETERS for: " + properties.getName());

        SlicingParameters params = new SlicingParameters();

        // Use generic build volume - printer compatibility checked at print time
        params.machineWidth = defaultBuildVolumeWidth;
        params.machineDepth = defaultBuildVolumeDepth;
        params.machineHeight = defaultBuildVolumeHeight;

        // Apply quality profile defaults if available
        QualityProfile qualityProfile = getQualityProfile(properties.getQualityProfile());
        if (qualityProfile != null) {
            logService.info("CuraEngineAdapter", "APPLYING QUALITY PROFILE: " + qualityProfile.name);
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
                String.format("FINAL PARAMETERS: Layer: %.2fmm, Speed: %.0fmm/s, Infill: %.0f%%, Temp: %d°C",
                        params.layerHeight, params.speedPrint, params.infillDensity, params.printTemperature));

        return params;
    }

    private QualityProfile getQualityProfile(String qualityProfileName) {
        if (qualityProfileName == null) {
            return QUALITY_PROFILES.get("standard");
        }
        return QUALITY_PROFILES.get(qualityProfileName.toLowerCase());
    }

    private void applySlicingPropertyOverrides(SlicingParameters params, SlicingProperty properties) {
        logService.info("CuraEngineAdapter", "APPLYING PROPERTY OVERRIDES...");

        int overrideCount = 0;

        // Layer settings
        if (properties.getLayerHeightMm() != null) {
            params.layerHeight = bigDecimalToFloat(properties.getLayerHeightMm());
            overrideCount++;
        }
        if (properties.getLineWidthMm() != null) {
            params.lineWidth = bigDecimalToFloat(properties.getLineWidthMm());
            overrideCount++;
        }

        // Speed settings (mm/s)
        if (properties.getPrintSpeedMmS() != null) {
            params.speedPrint = bigDecimalToFloat(properties.getPrintSpeedMmS());
            overrideCount++;
        }
        if (properties.getTravelSpeedMmS() != null) {
            params.speedTravel = bigDecimalToFloat(properties.getTravelSpeedMmS());
            overrideCount++;
        }
        if (properties.getFirstLayerSpeedMmS() != null) {
            params.speedFirstLayer = bigDecimalToFloat(properties.getFirstLayerSpeedMmS());
            overrideCount++;
        }

        // Infill settings
        if (properties.getInfillPercentage() != null) {
            params.infillDensity = bigDecimalToFloat(properties.getInfillPercentage());
            overrideCount++;
        }
        if (properties.getInfillPattern() != null) {
            params.infillPattern = properties.getInfillPattern();
            overrideCount++;
        }

        // Shell settings
        if (properties.getPerimeterCount() != null) {
            params.wallLineCount = properties.getPerimeterCount();
            overrideCount++;
        }

        // Support settings
        if (properties.getSupportsEnabled() != null) {
            params.supportEnabled = properties.getSupportsEnabled();
            overrideCount++;
        }

        // Temperature settings
        if (properties.getExtruderTempC() != null) {
            params.printTemperature = properties.getExtruderTempC();
            overrideCount++;
        }
        if (properties.getBedTempC() != null) {
            params.bedTemperature = properties.getBedTempC();
            overrideCount++;
        }

        logService.info("CuraEngineAdapter", "APPLIED " + overrideCount + " PROPERTY OVERRIDES");
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
        logService.info("CuraEngineAdapter", "DOWNLOADING STL for model: " + model.getName());

        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
            byte[] bytes = stlStream.readAllBytes();
            logService.info("CuraEngineAdapter", "STL DOWNLOAD SUCCESS: " + bytes.length + " bytes");
            return bytes;
        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "STL DOWNLOAD FAILED: " + e.getMessage());
            throw e;
        }
    }

    private void addSlicingParameters(MultiValueMap<String, Object> body, SlicingParameters params) {
        logService.info("CuraEngineAdapter", "ADDING SLICING PARAMETERS TO REQUEST...");

        // Generic machine settings (printer-independent)
        body.add("machine_width", params.machineWidth);
        body.add("machine_depth", params.machineDepth);
        body.add("machine_height", params.machineHeight);

        // Quality settings
        body.add("layer_height", params.layerHeight);
        body.add("line_width", params.lineWidth);
        body.add("wall_line_count", params.wallLineCount);
        body.add("wall_thickness", params.wallThickness);

        // Infill settings
        body.add("infill_sparse_density", params.infillDensity);
        body.add("infill_pattern", params.infillPattern);

        // Speed settings
        body.add("speed_print", params.speedPrint);
        body.add("speed_travel", params.speedTravel);

        // Temperature settings (generic - can be overridden by printer)
        body.add("material_print_temperature", params.printTemperature);
        body.add("material_bed_temperature", params.bedTemperature);

        // Support settings
        body.add("support_enable", params.supportEnabled);

        // Adhesion settings
        body.add("adhesion_type", determineAdhesionType(params));

        // Cooling settings
        body.add("cool_fan_enabled", params.fanEnabled);

        // Retraction settings
        body.add("retraction_enable", params.retractionEnabled);

        logService.info("CuraEngineAdapter", String.format(
                "PARAMETERS ADDED: %d total parameters, Key settings: Layer=%.2f, Speed=%.0f, Infill=%.0f, Temp=%d",
                body.size(), params.layerHeight, params.speedPrint, params.infillDensity, params.printTemperature));
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
        logService.info("CuraEngineAdapter", "CREATING SLICING RESULT...");

        byte[] gcodeBytes = gcode.getBytes();

        // Save G-code to storage
        logService.info("CuraEngineAdapter", "UPLOADING G-CODE TO STORAGE...");
        UploadResult uploadResult = storageService.upload(
                new ByteArrayInputStream(gcodeBytes),
                gcodeBytes.length,
                "text/plain",
                PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME
        );
        logService.info("CuraEngineAdapter", "G-CODE UPLOADED: Object key = " + uploadResult.getObjectKey());

        // Create file resource for G-code
        FileResource gcodeFile = fileResourceRepository.save(FileResource.builder()
                .fileName(model.getName() + ".gcode")
                .fileType("text/plain")
                .fileSize(gcodeBytes.length)
                .fileHash(uploadResult.getHashBytes())
                .objectKey(uploadResult.getObjectKey())
                .bucketName(PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME)
                .uploadedAt(Instant.now())
                .build());
        logService.info("CuraEngineAdapter", "G-CODE FILE RESOURCE CREATED: ID = " + gcodeFile.getId());

        // Create SlicingResult
        SlicingResult result = SlicingResult.builder()
                .sourceFile(model.getFileResource())
                .generatedFile(gcodeFile)
                .slicingProperty(properties)
                .lines(gcode.lines().count())
                .createdAt(Instant.now())
                .build();

        SlicingResult savedResult = slicingResultRepository.save(result);
        logService.info("CuraEngineAdapter", "SLICING RESULT CREATED: ID = " + savedResult.getId() +
                ", Lines = " + savedResult.getLines());

        return savedResult;
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

    private RestTemplate createRestTemplateWithTimeout(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30 * 1000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        return new RestTemplate(factory);
    }
}