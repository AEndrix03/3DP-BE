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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * CuraEngine adapter that integrates with existing external slicing system
 * Works with the external engine configuration in application.yml
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

    // Uses existing external engine configuration
    @Value("${slicing.engines.external.service-url}")
    private String curaServiceUrl;

    @Value("${slicing.engines.external.timeout-seconds}")
    private int timeoutSeconds;

    @Override
    public SlicingResult slice(Model model, SlicingProperty properties) {
        logService.info("CuraEngineAdapter", "Starting slicing for model: " + model.getName());

        try {
            // 1. Download STL and encode to Base64
            String stlBase64 = downloadAndEncodeSTL(model);

            // 2. Prepare request using existing SlicingProperty format
            CuraRequest request = buildCuraRequest(model, properties, stlBase64);

            // 3. Call CuraEngine API
            CuraResponse response = callCuraAPI(request);

            // 4. Save G-code and create result
            return createSlicingResult(response, model, properties);

        } catch (Exception e) {
            logService.error("CuraEngineAdapter", "Slicing failed: " + e.getMessage());
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
                fileType.toLowerCase().contains("stl") &&
                fileSize > 0 &&
                fileSize <= 50 * 1024 * 1024; // 50MB limit
    }

    @Override
    public String getName() {
        return "CuraEngine";
    }

    @Override
    public String getVersion() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    curaServiceUrl + "/health", Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object version = response.getBody().get("cura_version");
                return version != null ? version.toString() : "5.x.x";
            }
        } catch (Exception e) {
            logService.debug("CuraEngineAdapter", "Could not get version: " + e.getMessage());
        }
        return "5.x.x";
    }

    // ======================================
    // PRIVATE METHODS
    // ======================================

    private String downloadAndEncodeSTL(Model model) throws Exception {
        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId())) {
            byte[] stlBytes = stlStream.readAllBytes();
            return Base64.getEncoder().encodeToString(stlBytes);
        }
    }

    private CuraRequest buildCuraRequest(Model model, SlicingProperty properties, String stlBase64) {
        CuraRequest request = new CuraRequest();
        request.setJobId(java.util.UUID.randomUUID().toString());
        request.setStlData(stlBase64);
        request.setModelName(model.getName());

        // Convert SlicingProperty to CuraEngine settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("layer_height", parseFloat(properties.getLayerHeightMm(), 0.2f));
        settings.put("infill_sparse_density", parseFloat(properties.getInfillPercentage(), 20f));
        settings.put("speed_print", parseFloat(properties.getPrintSpeedMmS(), 60f));
        settings.put("speed_travel", parseFloat(properties.getTravelSpeedMmS(), 150f));
        settings.put("material_print_temperature", (int) properties.getExtruderTempC());
        settings.put("material_bed_temperature", (int) properties.getBedTempC());
        settings.put("wall_thickness", properties.getPerimeterCount() * 0.4f);
        settings.put("support_enable", parseBoolean(properties.getSupportsEnabled()));
        settings.put("adhesion_type", parseBoolean(properties.getBrimEnabled()) ? "brim" : "skirt");

        request.setSettings(settings);
        return request;
    }

    private CuraResponse callCuraAPI(CuraRequest request) throws Exception {
        String url = curaServiceUrl + "/slice";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<CuraRequest> entity = new HttpEntity<>(request, headers);

        logService.info("CuraEngineAdapter", "Calling CuraEngine: " + url);

        ResponseEntity<CuraResponse> response = restTemplate.postForEntity(url, entity, CuraResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            CuraResponse curaResponse = response.getBody();

            if (!curaResponse.isSuccess()) {
                throw new RuntimeException("CuraEngine error: " + curaResponse.getError());
            }

            return curaResponse;
        } else {
            throw new RuntimeException("CuraEngine HTTP error: " + response.getStatusCode());
        }
    }

    private SlicingResult createSlicingResult(CuraResponse response, Model model, SlicingProperty properties)
            throws Exception {

        // Decode G-code
        byte[] gcodeBytes = Base64.getDecoder().decode(response.getGcodeData());
        String gcode = new String(gcodeBytes);

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

        // Create and save SlicingResult
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
    // SIMPLE DTO CLASSES
    // ======================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuraRequest {
        private String jobId;
        private String stlData;        // Base64 STL
        private String modelName;
        private Map<String, Object> settings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CuraResponse {
        private boolean success;
        private String jobId;
        private String gcodeData;      // Base64 G-code
        private String error;
    }
}