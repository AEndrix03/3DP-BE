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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Component("prusaSlicerEngine")
@ConditionalOnProperty(name = "slicing.engines.prusa.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class PrusaSlicerEngine implements SlicingEngine {

    private final FileResourceService fileResourceService;
    private final StorageService storageService;
    private final FileResourceRepository fileResourceRepository;
    private final SlicingResultRepository slicingResultRepository;
    private final LogService logService;

    @Value("${slicing.engines.prusa.binary-path:/usr/bin/prusa-slicer}")
    private String prusaSlicerPath;

    @Value("${slicing.engines.prusa.timeout-seconds:300}")
    private int timeoutSeconds;

    @Value("${slicing.temp-directory:/tmp/slicing}")
    private String tempDirectory;

    @Override
    public SlicingResult slice(Model model, SlicingProperty properties) {
        logService.info("PrusaSlicerEngine", "Starting PrusaSlicer for model: " + model.getId());

        Path workingDir = null;
        try {
            // 1. Create temporary working directory
            workingDir = createWorkingDirectory();

            // 2. Validate model before processing
            if (!validateModel(model)) {
                throw new SlicingException("Model validation failed");
            }

            // 3. Download STL to temporary file
            Path stlFile = downloadModelToFile(model, workingDir);

            // 4. Generate PrusaSlicer configuration
            Path configFile = generatePrusaConfig(properties, workingDir);

            // 5. Execute PrusaSlicer
            SlicingExecutionResult executionResult = executePrusaSlicer(stlFile, configFile, workingDir);

            // 6. Upload G-code to storage
            FileResource gcodeFile = uploadGcodeToStorage(executionResult.getGcodeFile(), model.getName());

            // 7. Create SlicingResult with real metrics
            SlicingResult result = SlicingResult.builder()
                    .sourceFile(model.getFileResource())
                    .generatedFile(gcodeFile)
                    .slicingProperty(properties)
                    .lines(executionResult.getLineCount())
                    .createdAt(Instant.now())
                    .build();

            logService.info("PrusaSlicerEngine", "Slicing completed successfully for model: " + model.getId());
            return slicingResultRepository.save(result);

        } catch (Exception e) {
            logService.error("PrusaSlicerEngine", "Slicing failed for model: " + model.getId() + " - " + e.getMessage());
            throw new SlicingException("PrusaSlicer execution failed: " + e.getMessage(), e);
        } finally {
            // Cleanup temporary files
            cleanupWorkingDirectory(workingDir);
        }
    }

    private Path createWorkingDirectory() throws IOException {
        Path baseDir = Paths.get(tempDirectory);
        Files.createDirectories(baseDir);
        return Files.createTempDirectory(baseDir, "slicing_");
    }

    private Path downloadModelToFile(Model model, Path workingDir) throws IOException {
        Path stlFile = workingDir.resolve(model.getName() + ".stl");

        try (InputStream stlStream = fileResourceService.download(model.getFileResource().getId());
             OutputStream fileOut = Files.newOutputStream(stlFile)) {

            stlStream.transferTo(fileOut);
        }

        logService.debug("PrusaSlicerEngine", "Downloaded STL to: " + stlFile);
        return stlFile;
    }

    private Path generatePrusaConfig(SlicingProperty properties, Path workingDir) throws IOException {
        Path configFile = workingDir.resolve("config.ini");

        // Print settings

        String config = "[print]\n" +
                "layer_height = " + properties.getLayerHeightMm() + "\n" +
                "first_layer_height = " + (properties.getFirstLayerHeightMm() != null ?
                properties.getFirstLayerHeightMm() : properties.getLayerHeightMm()) +
                "\n" +
                "perimeters = " + properties.getPerimeterCount() + "\n" +
                "top_solid_layers = " + properties.getTopSolidLayers() + "\n" +
                "bottom_solid_layers = " + properties.getBottomSolidLayers() + "\n" +
                "fill_density = " + parsePercentage(properties.getInfillPercentage()) + "%\n" +
                "fill_pattern = " + properties.getInfillPattern() + "\n" +
                "support_material = " + (parseBoolean(properties.getSupportsEnabled()) ? "1" : "0") + "\n" +
                "support_material_threshold = " + properties.getSupportAngleThreshold() + "\n" +
                "brim_width = " + (parseBoolean(properties.getBrimEnabled()) ? properties.getBrimWidthMm() : "0") + "\n" +

                // Speed settings
                "perimeter_speed = " + properties.getPrintSpeedMmS() + "\n" +
                "infill_speed = " + properties.getPrintSpeedMmS() + "\n" +
                "travel_speed = " + properties.getTravelSpeedMmS() + "\n" +
                "first_layer_speed = " + (properties.getFirstLayerSpeedMmS() != null ?
                properties.getFirstLayerSpeedMmS() : properties.getPrintSpeedMmS()) +
                "\n" +

                // Temperature settings
                "\n[filament]\n" +
                "temperature = " + properties.getExtruderTempC() + "\n" +
                "bed_temperature = " + properties.getBedTempC() + "\n" +

                // Basic printer settings
                "\n[printer]\n" +
                "printer_technology = FFF\n" +
                "bed_shape = 0x0,200x0,200x200,0x200\n" +
                "max_layer_height = 0.3\n" +
                "min_layer_height = 0.1\n" +
                "nozzle_diameter = 0.4\n";

        Files.writeString(configFile, config);
        logService.debug("PrusaSlicerEngine", "Generated config file: " + configFile);
        return configFile;
    }

    private SlicingExecutionResult executePrusaSlicer(Path stlFile, Path configFile, Path workingDir) throws IOException, InterruptedException {
        Path outputFile = workingDir.resolve("output.gcode");

        ProcessBuilder processBuilder = new ProcessBuilder(
                prusaSlicerPath,
                "--load", configFile.toString(),
                "--export-gcode",
                "--output", outputFile.toString(),
                stlFile.toString()
        );

        processBuilder.directory(workingDir.toFile());
        processBuilder.redirectErrorStream(true);

        logService.info("PrusaSlicerEngine", "Executing: " + String.join(" ", processBuilder.command()));

        Process process = processBuilder.start();

        // Capture output for debugging
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                logService.debug("PrusaSlicerEngine", "Slicer output: " + line);
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new SlicingException("PrusaSlicer execution timed out after " + timeoutSeconds + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new SlicingException("PrusaSlicer failed with exit code " + exitCode + ". Output: " + output);
        }

        if (!Files.exists(outputFile)) {
            throw new SlicingException("PrusaSlicer did not generate output file");
        }

        // Count lines in generated G-code
        long lineCount = Files.lines(outputFile).count();

        return new SlicingExecutionResult(outputFile, lineCount, output.toString());
    }

    private FileResource uploadGcodeToStorage(Path gcodeFile, String modelName) throws IOException {
        String filename = modelName.replaceAll("\\.[^.]*$", "") + ".gcode";

        try (InputStream gcodeStream = Files.newInputStream(gcodeFile)) {
            UploadResult result = storageService.upload(
                    gcodeStream,
                    Files.size(gcodeFile),
                    "text/plain",
                    PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME
            );

            return fileResourceRepository.save(FileResource.builder()
                    .fileName(filename)
                    .fileType("text/plain")
                    .fileSize(Files.size(gcodeFile))
                    .fileHash(result.getHashBytes())
                    .objectKey(result.getObjectKey())
                    .bucketName(PrinterCostants.PRINTER_SLICING_STORAGE_BUCKET_NAME)
                    .uploadedAt(Instant.now())
                    .build());
        }
    }

    private void cleanupWorkingDirectory(Path workingDir) {
        if (workingDir != null && Files.exists(workingDir)) {
            try {
                Files.walk(workingDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logService.warn("PrusaSlicerEngine", "Failed to delete temporary file: " + path);
                            }
                        });
            } catch (IOException e) {
                logService.warn("PrusaSlicerEngine", "Failed to cleanup working directory: " + workingDir);
            }
        }
    }

    @Override
    public boolean validateModel(Model model) {
        if (model == null || model.getFileResource() == null) {
            return false;
        }

        // Check file type
        String fileType = model.getFileResource().getFileType();
        if (fileType == null || !fileType.toLowerCase().contains("stl")) {
            logService.warn("PrusaSlicerEngine", "Unsupported file type: " + fileType);
            return false;
        }

        // Check file size (reasonable limits)
        long fileSize = model.getFileResource().getFileSize();
        if (fileSize <= 0 || fileSize > 100 * 1024 * 1024) { // 100MB limit
            logService.warn("PrusaSlicerEngine", "Invalid file size: " + fileSize);
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "PrusaSlicer";
    }

    @Override
    public String getVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(prusaSlicerPath, "--version");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String version = reader.readLine();
                return version != null ? version.trim() : "unknown";
            }
        } catch (Exception e) {
            logService.warn("PrusaSlicerEngine", "Could not determine PrusaSlicer version");
            return "unknown";
        }
    }

    // Utility methods
    private double parsePercentage(String percentage) {
        try {
            return Double.parseDouble(percentage);
        } catch (NumberFormatException e) {
            return 20.0; // default
        }
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    // Inner classes
    private static class SlicingExecutionResult {
        private final Path gcodeFile;
        private final long lineCount;
        private final String output;

        public SlicingExecutionResult(Path gcodeFile, long lineCount, String output) {
            this.gcodeFile = gcodeFile;
            this.lineCount = lineCount;
            this.output = output;
        }

        public Path getGcodeFile() {
            return gcodeFile;
        }

        public long getLineCount() {
            return lineCount;
        }

        public String getOutput() {
            return output;
        }
    }

    public static class SlicingException extends RuntimeException {
        public SlicingException(String message) {
            super(message);
        }

        public SlicingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}