package it.aredegalli.printer.service.slicing.engine;

import it.aredegalli.printer.dto.storage.UploadResult;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.resource.FileResource;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.model.slicing.SlicingResult;
import it.aredegalli.printer.repository.resource.FileResourceRepository;
import it.aredegalli.printer.repository.slicing.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.resource.FileResourceService;
import it.aredegalli.printer.service.storage.StorageService;
import it.aredegalli.printer.util.PrinterCostants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;

/**
 * Advanced CuraEngine adapter with full configuration support
 * Supports all printer configurations and slicing parameters
 * Optimized for form-data API integration
 */
@Component("curaEngineAdapter")
@RequiredArgsConstructor
public class CuraEngineAdapter implements SlicingEngine {

    private final FileResourceService fileResourceService;
    private final StorageService storageService;
    private final FileResourceRepository fileResourceRepository;
    private final SlicingResultRepository slicingResultRepository;
    private final LogService logService;
    private final RestTemplate restTemplate;

    @Value("${slicing.engines.external.service-url}")
    private String curaServiceUrl;

    @Value("${slicing.engines.external.timeout-seconds:120}")
    private int timeoutSeconds;

    // Printer configurations mapping
    private static final Map<String, PrinterConfig> PRINTER_CONFIGS = Map.of(
            "MINI_15X15", new PrinterConfig("Mini Printer 15x15cm", 150, 150, 150),
            "ENDER3", new PrinterConfig("Creality Ender 3", 220, 220, 250),
            "PRUSA_I3", new PrinterConfig("Prusa i3", 250, 210, 200),
            "ULTIMAKER3", new PrinterConfig("Ultimaker 3", 215, 215, 200),
            "CR10", new PrinterConfig("Creality CR-10", 300, 300, 400),
            "GENERIC", new PrinterConfig("Generic FDM", 200, 200, 200)
    );

    // Quality profiles
    private static final Map<String, QualityProfile> QUALITY_PROFILES = Map.of(
            "DRAFT", new QualityProfile("Draft", 0.3f, 80f, 15f, 2),
            "NORMAL", new QualityProfile("Normal", 0.2f, 50f, 20f, 2),
            "FINE", new QualityProfile("Fine", 0.15f, 40f, 25f, 3),
            "ULTRA_FINE", new QualityProfile("Ultra Fine", 0.1f, 30f, 30f, 4),
            "MINIATURE", new QualityProfile("Miniature", 0.05f, 20f, 30f, 4)
    );

    // Material profiles
    private static final Map<String, MaterialProfile> MATERIAL_PROFILES = Map.of(
            "PLA", new MaterialProfile("PLA", 200, 50, 60f),
            "ABS", new MaterialProfile("ABS", 240, 80, 80f),
            "PETG", new MaterialProfile("PETG", 230, 70, 70f),
            "TPU", new MaterialProfile("TPU", 220, 60, 30f),
            "WOOD", new MaterialProfile("Wood PLA", 190, 50, 45f)
    );

    @Override
    public SlicingResult slice(Model model, SlicingProperty properties) {
        logService.info("CuraEngineAdapter", "Starting advanced slicing for model: " + model.getName());

        try {
            // 1. Download STL file
            byte[] stlBytes = downloadSTLBytes(model);

            // 2. Build advanced slicing parameters
            SlicingParameters params = buildAdvancedSlicingParameters(model, properties);

            // 3. Call CuraEngine API with form-data
            String gcode = callCuraEngineAPI(stlBytes, model.getId().toString(), params);

            // 4. Save and return result
            return createSlicingResult(gcode, model, properties);

        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "Advanced slicing failed: " + e.getMessage());
            throw new RuntimeException("CuraEngine slicing failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean validateModel(Model model) {
        if (model == null || model.getFileResource() == null) {
            return false;
        }

        String fileType = model.getFileResource().getFileType();
        long fileSize = model.getFileResource().getFileSize();

        return fileType != null &&
                (fileType.toLowerCase().contains("stl") ||
                        fileType.toLowerCase().contains("model")) &&
                fileSize > 0 &&
                fileSize <= 50 * 1024 * 1024; // 50MB limit
    }

    @Override
    public String getName() {
        return "CuraEngine Advanced";
    }

    @Override
    public String getVersion() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    curaServiceUrl + "/config", Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return "5.6.0-Extended"; // Our extended version
            }
        } catch (Exception e) {
            logService.debug("CuraEngineAdapter", "Could not get version: " + e.getMessage());
        }
        return "5.6.0-Extended";
    }

    // ======================================
    // ADVANCED CONFIGURATION METHODS
    // ======================================

    private SlicingParameters buildAdvancedSlicingParameters(Model model, SlicingProperty properties) {
        SlicingParameters params = new SlicingParameters();

        // 1. Determine printer configuration
        PrinterConfig printer = determinePrinterConfig(properties);
        params.machineWidth = printer.width;
        params.machineDepth = printer.depth;
        params.machineHeight = printer.height;

        // 2. Apply quality profile
        QualityProfile quality = determineQualityProfile(properties);
        params.layerHeight = quality.layerHeight;
        params.speedPrint = quality.printSpeed;
        params.infillDensity = quality.infillDensity;
        params.wallLineCount = quality.wallCount;

        // 3. Apply material profile
        MaterialProfile material = determineMaterialProfile(properties);
        params.printTemperature = material.printTemp;
        params.bedTemperature = material.bedTemp;
        params.speedTravel = material.travelSpeed;

        // 4. Override with specific properties if provided
        applySpecificOverrides(params, properties);

        // 5. Calculate derived parameters
        calculateDerivedParameters(params);

        logService.info("CuraEngineAdapter",
                String.format("Using config: %dx%dx%d, Layer: %.2f, Speed: %.0f",
                        params.machineWidth, params.machineDepth, params.machineHeight,
                        params.layerHeight, params.speedPrint));

        return params;
    }

    private PrinterConfig determinePrinterConfig(SlicingProperty properties) {
        // Check if specific printer is configured
        String printerType = properties.getPrinterType(); // Assuming this exists or add it

        if (printerType != null && PRINTER_CONFIGS.containsKey(printerType.toUpperCase())) {
            return PRINTER_CONFIGS.get(printerType.toUpperCase());
        }

        // Try to determine from bed size if available
        if (properties.getBedWidth() != null && properties.getBedDepth() != null) {
            int width = Integer.parseInt(properties.getBedWidth());
            int depth = Integer.parseInt(properties.getBedDepth());

            if (width <= 160 && depth <= 160) return PRINTER_CONFIGS.get("MINI_15X15");
            if (width <= 230 && depth <= 230) return PRINTER_CONFIGS.get("ENDER3");
            if (width <= 260 && depth <= 220) return PRINTER_CONFIGS.get("PRUSA_I3");
            if (width <= 220 && depth <= 220) return PRINTER_CONFIGS.get("ULTIMAKER3");
            if (width >= 300) return PRINTER_CONFIGS.get("CR10");
        }

        // Default to mini printer for small objects
        return PRINTER_CONFIGS.get("MINI_15X15");
    }

    private QualityProfile determineQualityProfile(SlicingProperty properties) {
        // Check layer height to determine quality
        float layerHeight = parseFloat(properties.getLayerHeightMm(), 0.2f);

        if (layerHeight >= 0.3f) return QUALITY_PROFILES.get("DRAFT");
        if (layerHeight >= 0.2f) return QUALITY_PROFILES.get("NORMAL");
        if (layerHeight >= 0.15f) return QUALITY_PROFILES.get("FINE");
        if (layerHeight >= 0.1f) return QUALITY_PROFILES.get("ULTRA_FINE");
        return QUALITY_PROFILES.get("MINIATURE");
    }

    private MaterialProfile determineMaterialProfile(SlicingProperty properties) {
        float extruderTemp = properties.getExtruderTempC();

        if (extruderTemp <= 205) return MATERIAL_PROFILES.get("PLA");
        if (extruderTemp <= 225) return MATERIAL_PROFILES.get("PETG");
        if (extruderTemp <= 235) return MATERIAL_PROFILES.get("TPU");
        if (extruderTemp >= 235) return MATERIAL_PROFILES.get("ABS");

        return MATERIAL_PROFILES.get("PLA"); // Default
    }

    private void applySpecificOverrides(SlicingParameters params, SlicingProperty properties) {
        // Override with specific values if provided
        if (properties.getLayerHeightMm() != null) {
            params.layerHeight = parseFloat(properties.getLayerHeightMm(), params.layerHeight);
        }
        if (properties.getPrintSpeedMmS() != null) {
            params.speedPrint = parseFloat(properties.getPrintSpeedMmS(), params.speedPrint);
        }
        if (properties.getTravelSpeedMmS() != null) {
            params.speedTravel = parseFloat(properties.getTravelSpeedMmS(), params.speedTravel);
        }
        if (properties.getInfillPercentage() != null) {
            params.infillDensity = parseFloat(properties.getInfillPercentage(), params.infillDensity);
        }
        if (properties.getExtruderTempC() > 0) {
            params.printTemperature = (int) properties.getExtruderTempC();
        }
        if (properties.getBedTempC() > 0) {
            params.bedTemperature = (int) properties.getBedTempC();
        }

        // Advanced parameters
        params.supportEnabled = parseBoolean(properties.getSupportsEnabled());
        params.brimEnabled = parseBoolean(properties.getBrimEnabled());
        params.wallThickness = params.wallLineCount * 0.4f; // Standard nozzle
    }

    private void calculateDerivedParameters(SlicingParameters params) {
        // Calculate optimal parameters based on layer height
        params.lineWidth = Math.max(0.3f, params.layerHeight * 2f);
        params.topBottomThickness = Math.max(0.8f, params.layerHeight * 4f);

        // Adjust speeds for layer height
        if (params.layerHeight <= 0.1f) {
            params.speedPrint *= 0.7f; // Slower for fine layers
            params.speedTravel *= 0.8f;
        } else if (params.layerHeight >= 0.3f) {
            params.speedPrint *= 1.2f; // Faster for thick layers
        }

        // Support density based on infill
        params.supportDensity = Math.min(25f, params.infillDensity + 5f);
    }

    // ======================================
    // API CALL METHODS
    // ======================================

    private byte[] downloadSTLBytes(Model model) throws Exception {
        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
            return stlStream.readAllBytes();
        }
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

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        logService.info("CuraEngineAdapter",
                String.format("Calling CuraEngine API: %s with %d parameters", url, body.size()));

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String gcode = response.getBody();
                logService.info("CuraEngineAdapter",
                        String.format("Received G-code: %d lines, %d bytes",
                                gcode.lines().count(), gcode.length()));
                return gcode;
            } else {
                throw new RuntimeException("CuraEngine HTTP error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "API call failed: " + e.getMessage());
            throw new RuntimeException("CuraEngine API call failed", e);
        }
    }

    private void addSlicingParameters(MultiValueMap<String, Object> body, SlicingParameters params) {
        // Machine settings
        body.add("machine_width", params.machineWidth);
        body.add("machine_depth", params.machineDepth);
        body.add("machine_height", params.machineHeight);

        // Quality settings
        body.add("layer_height", params.layerHeight);
        body.add("line_width", params.lineWidth);
        body.add("wall_line_count", params.wallLineCount);
        body.add("wall_thickness", params.wallThickness);
        body.add("top_bottom_thickness", params.topBottomThickness);

        // Infill settings
        body.add("infill_sparse_density", params.infillDensity);

        // Speed settings
        body.add("speed_print", params.speedPrint);
        body.add("speed_travel", params.speedTravel);

        // Temperature settings
        body.add("material_print_temperature", params.printTemperature);
        body.add("material_bed_temperature", params.bedTemperature);

        // Support settings
        body.add("support_enable", params.supportEnabled);
        if (params.supportEnabled) {
            body.add("support_density", params.supportDensity);
            body.add("support_z_distance", 0.2f);
        }

        // Adhesion settings
        body.add("adhesion_type", params.brimEnabled ? "brim" : "skirt");

        logService.debug("CuraEngineAdapter",
                String.format("Parameters: %sx%sx%s, Layer: %.2f, Infill: %.0f%%, Speed: %.0f/%.0f",
                        params.machineWidth, params.machineDepth, params.machineHeight,
                        params.layerHeight, params.infillDensity, params.speedPrint, params.speedTravel));
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

    private float parseFloat(String value, float defaultValue) {
        try {
            return value != null ? Float.parseFloat(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    // ======================================
    // CONFIGURATION CLASSES
    // ======================================

    private static class SlicingParameters {
        // Machine
        int machineWidth = 150;
        int machineDepth = 150;
        int machineHeight = 150;

        // Quality
        float layerHeight = 0.2f;
        float lineWidth = 0.4f;
        int wallLineCount = 2;
        float wallThickness = 0.8f;
        float topBottomThickness = 0.8f;

        // Infill
        float infillDensity = 20f;

        // Speed
        float speedPrint = 50f;
        float speedTravel = 120f;

        // Temperature
        int printTemperature = 210;
        int bedTemperature = 60;

        // Support
        boolean supportEnabled = false;
        float supportDensity = 15f;

        // Adhesion
        boolean brimEnabled = true;
    }

    private static class PrinterConfig {
        final String name;
        final int width, depth, height;

        PrinterConfig(String name, int width, int depth, int height) {
            this.name = name;
            this.width = width;
            this.depth = depth;
            this.height = height;
        }
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

    private static class MaterialProfile {
        final String name;
        final int printTemp, bedTemp;
        final float travelSpeed;

        MaterialProfile(String name, int printTemp, int bedTemp, float travelSpeed) {
            this.name = name;
            this.printTemp = printTemp;
            this.bedTemp = bedTemp;
            this.travelSpeed = travelSpeed;
        }
    }
}