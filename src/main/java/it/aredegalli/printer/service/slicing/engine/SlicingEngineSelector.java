package it.aredegalli.printer.service.slicing.engine;

import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import it.aredegalli.printer.service.log.LogService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for selecting the optimal slicing engine based on job requirements
 */
@Service
@RequiredArgsConstructor
public class SlicingEngineSelector {

    private final List<SlicingEngine> availableEngines;
    private final LogService logService;

    @Value("${slicing.default-engine:default}")
    private String defaultEngineName;

    @Value("${slicing.engine-selection.enabled:true}")
    private boolean smartSelectionEnabled;

    private Map<String, SlicingEngine> engineMap;

    @PostConstruct
    public void initializeEngines() {
        engineMap = new HashMap<>();

        for (SlicingEngine engine : availableEngines) {
            engineMap.put(engine.getName().toLowerCase(), engine);
            logService.info("SlicingEngineSelector",
                    String.format("Registered engine: %s v%s", engine.getName(), engine.getVersion()));
        }

        logService.info("SlicingEngineSelector",
                String.format("Initialized with %d engines. Default: %s", engineMap.size(), defaultEngineName));
    }

    /**
     * Select the optimal engine for the given model and properties
     */
    public SlicingEngine selectEngine(SlicingProperty properties, Model model) {
        if (!smartSelectionEnabled) {
            return getDefaultEngine();
        }

        try {
            // Analyze job requirements
            JobRequirements requirements = analyzeRequirements(properties, model);

            // Select engine based on requirements
            SlicingEngine selectedEngine = selectBasedOnRequirements(requirements);

            logService.info("SlicingEngineSelector",
                    String.format("Selected engine: %s for model: %s (complexity: %s, quality: %s)",
                            selectedEngine.getName(), model.getName(),
                            requirements.getComplexity(), requirements.getQualityLevel()));

            return selectedEngine;

        } catch (Exception e) {
            logService.warn("SlicingEngineSelector",
                    "Engine selection failed, using default: " + e.getMessage());
            return getDefaultEngine();
        }
    }

    /**
     * Get engine by name
     */
    public SlicingEngine getEngine(String engineName) {
        SlicingEngine engine = engineMap.get(engineName.toLowerCase());
        if (engine == null) {
            logService.warn("SlicingEngineSelector", "Engine not found: " + engineName + ", using default");
            return getDefaultEngine();
        }
        return engine;
    }

    /**
     * Get default engine
     */
    public SlicingEngine getDefaultEngine() {
        SlicingEngine defaultEngine = engineMap.get(defaultEngineName.toLowerCase());

        if (defaultEngine == null && !engineMap.isEmpty()) {
            defaultEngine = engineMap.values().iterator().next();
            logService.warn("SlicingEngineSelector",
                    "Default engine not found, using: " + defaultEngine.getName());
        }

        if (defaultEngine == null) {
            throw new IllegalStateException("No slicing engines available");
        }

        return defaultEngine;
    }

    /**
     * Check if an engine is available
     */
    public boolean isEngineAvailable(String engineName) {
        return engineMap.containsKey(engineName.toLowerCase());
    }

    /**
     * Get all available engine names
     */
    public Map<String, String> getAvailableEngines() {
        Map<String, String> engines = new HashMap<>();
        for (SlicingEngine engine : engineMap.values()) {
            engines.put(engine.getName(), engine.getVersion());
        }
        return engines;
    }

    private JobRequirements analyzeRequirements(SlicingProperty properties, Model model) {
        JobRequirements requirements = new JobRequirements();

        // Analyze model complexity
        requirements.complexity = analyzeModelComplexity(model);

        // Analyze quality requirements
        requirements.qualityLevel = analyzeQualityRequirements(properties);

        // Analyze special features needed
        requirements.needsSupports = parseBoolean(properties.getSupportsEnabled());
        requirements.needsBrim = parseBoolean(properties.getBrimEnabled());
        requirements.hasAdvancedSettings = properties.getAdvancedSettings() != null &&
                !properties.getAdvancedSettings().trim().isEmpty();

        // Analyze performance requirements
        requirements.performanceLevel = analyzePerformanceRequirements(model, properties);

        return requirements;
    }

    private ModelComplexity analyzeModelComplexity(Model model) {
        long fileSize = model.getFileResource().getFileSize();

        // Simple heuristic based on file size
        if (fileSize < 500_000) { // < 500KB
            return ModelComplexity.SIMPLE;
        } else if (fileSize < 5_000_000) { // < 5MB
            return ModelComplexity.MEDIUM;
        } else {
            return ModelComplexity.COMPLEX;
        }
    }

    private QualityLevel analyzeQualityRequirements(SlicingProperty properties) {
        try {
            double layerHeight = Double.parseDouble(properties.getLayerHeightMm());

            if (layerHeight >= 0.3) {
                return QualityLevel.DRAFT;
            } else if (layerHeight >= 0.2) {
                return QualityLevel.STANDARD;
            } else if (layerHeight >= 0.1) {
                return QualityLevel.HIGH;
            } else {
                return QualityLevel.ULTRA;
            }
        } catch (NumberFormatException e) {
            return QualityLevel.STANDARD;
        }
    }

    private PerformanceLevel analyzePerformanceRequirements(Model model, SlicingProperty properties) {
        // Consider model size and quality level for performance requirements
        ModelComplexity complexity = analyzeModelComplexity(model);
        QualityLevel quality = analyzeQualityRequirements(properties);

        if (complexity == ModelComplexity.COMPLEX || quality == QualityLevel.ULTRA) {
            return PerformanceLevel.HIGH;
        } else if (complexity == ModelComplexity.MEDIUM || quality == QualityLevel.HIGH) {
            return PerformanceLevel.MEDIUM;
        } else {
            return PerformanceLevel.LOW;
        }
    }

    private SlicingEngine selectBasedOnRequirements(JobRequirements requirements) {
        // Priority-based selection logic

        // 1. For complex models or ultra-high quality, prefer PrusaSlicer
        if (requirements.complexity == ModelComplexity.COMPLEX ||
                requirements.qualityLevel == QualityLevel.ULTRA ||
                requirements.hasAdvancedSettings) {

            SlicingEngine prusaEngine = engineMap.get("prusaslicer");
            if (prusaEngine != null) {
                return prusaEngine;
            }
        }

        // 2. For medium complexity, any professional engine is fine
        if (requirements.complexity == ModelComplexity.MEDIUM) {
            SlicingEngine curaEngine = engineMap.get("cura");
            if (curaEngine != null) {
                return curaEngine;
            }

            SlicingEngine prusaEngine = engineMap.get("prusaslicer");
            if (prusaEngine != null) {
                return prusaEngine;
            }
        }

        // 3. For simple models, any engine works
        // Prefer faster engines for simple jobs
        SlicingEngine defaultEngine = engineMap.get("default");
        if (defaultEngine != null && requirements.complexity == ModelComplexity.SIMPLE) {
            return defaultEngine;
        }

        // 4. Fallback to default
        return getDefaultEngine();
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    // Inner classes for analysis
    private static class JobRequirements {
        ModelComplexity complexity = ModelComplexity.MEDIUM;
        QualityLevel qualityLevel = QualityLevel.STANDARD;
        PerformanceLevel performanceLevel = PerformanceLevel.MEDIUM;
        boolean needsSupports = false;
        boolean needsBrim = false;
        boolean hasAdvancedSettings = false;

        public ModelComplexity getComplexity() {
            return complexity;
        }

        public QualityLevel getQualityLevel() {
            return qualityLevel;
        }

        public PerformanceLevel getPerformanceLevel() {
            return performanceLevel;
        }
    }

    public enum ModelComplexity {
        SIMPLE("Simple"),
        MEDIUM("Medium"),
        COMPLEX("Complex");

        private final String displayName;

        ModelComplexity(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum QualityLevel {
        DRAFT("Draft"),
        STANDARD("Standard"),
        HIGH("High"),
        ULTRA("Ultra");

        private final String displayName;

        QualityLevel(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum PerformanceLevel {
        LOW("Low"),
        MEDIUM("Medium"),
        HIGH("High");

        private final String displayName;

        PerformanceLevel(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}