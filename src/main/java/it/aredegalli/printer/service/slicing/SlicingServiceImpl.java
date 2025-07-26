package it.aredegalli.printer.service.slicing;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.slicing.MaterialDto;
import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.mapper.material.MaterialMapper;
import it.aredegalli.printer.mapper.slicing.SlicingQueueMapper;
import it.aredegalli.printer.mapper.slicing.SlicingResultMapper;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.metric.SlicingMetric;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import it.aredegalli.printer.model.slicing.queue.SlicingQueueResult;
import it.aredegalli.printer.model.slicing.result.SlicingResult;
import it.aredegalli.printer.model.slicing.result.SlicingResultMaterial;
import it.aredegalli.printer.model.validation.ModelValidation;
import it.aredegalli.printer.repository.model.ModelRepository;
import it.aredegalli.printer.repository.slicing.property.SlicingPropertyRepository;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueRepository;
import it.aredegalli.printer.repository.slicing.queue.SlicingQueueResultRepository;
import it.aredegalli.printer.repository.slicing.result.SlicingResultMaterialRepository;
import it.aredegalli.printer.repository.slicing.result.SlicingResultRepository;
import it.aredegalli.printer.repository.validation.ModelValidationRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngine;
import it.aredegalli.printer.service.slicing.engine.SlicingEngineSelector;
import it.aredegalli.printer.service.slicing.metrics.SlicingMetricsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlicingServiceImpl implements SlicingService {

    private final SlicingResultRepository slicingResultRepository;
    private final SlicingResultMapper slicingResultMapper;
    private final SlicingResultMaterialRepository slicingResultMaterialRepository;
    private final MaterialMapper materialMapper;
    private final LogService log;

    private final SlicingQueueRepository slicingQueueRepository;
    private final SlicingQueueResultRepository slicingQueueResultRepository;
    private final SlicingPropertyRepository slicingPropertyRepository;
    private final SlicingEngineSelector engineSelector;
    private final SlicingMetricsService metricsService;
    private final ModelRepository modelRepository;
    private final ModelValidationRepository modelValidationRepository;
    private final SlicingQueueMapper slicingQueueMapper;


    // Configuration
    @Value("${slicing.error-handling.max-retries:2}")
    private int maxRetries;

    @Value("${slicing.error-handling.fallback-to-default:true}")
    private boolean fallbackToDefault;

    @Override
    public List<SlicingResultDto> getAllSlicingResultBySourceId(UUID sourceId) {
        return slicingResultRepository.findBySourceFile_Id(sourceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public SlicingResultDto getSlicingResultById(UUID id) {
        return slicingResultRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("Slicing result not found"));
    }

    @Override
    @Transactional
    public void deleteSlicingResultById(UUID id) {
        SlicingResult result = slicingResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Slicing result not found"));

        slicingQueueResultRepository.deleteBySlicingResultId(id);

        Optional<SlicingMetric> metrics = Optional.ofNullable(metricsService.getMetricsBySlicingResultId(id));
        metrics.ifPresent(m -> log.debug("SlicingServiceImpl", "Deleting metrics for result: " + id));

        slicingResultRepository.deleteById(id);
        log.info("SlicingServiceImpl", "Slicing result with ID " + id + " was deleted");
    }

    @Override
    @Transactional
    public UUID queueSlicing(UUID modelId, UUID slicingPropertyId, String userId, Integer priority) {
        log.info("SlicingServiceImpl", "Queueing slicing for model: " + modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new NotFoundException("Model not found: " + modelId));

        SlicingProperty property = findSlicingPropertyById(slicingPropertyId);

        ModelValidation validation = validateModelIfNeeded(model);
        if (validation != null && validation.getHasErrors()) {
            throw new IllegalArgumentException("Model has validation errors: " + validation.getErrorDetails());
        }

        SlicingQueue queue = SlicingQueue.builder()
                .model(model)
                .slicingProperty(property)
                .priority(priority != null ? priority : 5)
                .status(SlicingStatus.QUEUED.getCode())
                .createdAt(Instant.now())
                .progressPercentage(0)
                .createdByUserId(userId)
                .build();

        queue = slicingQueueRepository.save(queue);
        log.info("SlicingServiceImpl", "Slicing queued with ID: " + queue.getId());
        return queue.getId();
    }

    @Override
    public SlicingQueueDto getQueueStatus(UUID queueId) {
        var queue = slicingQueueRepository.findById(queueId).orElseThrow(() -> new NotFoundException("SlicingQueue not found"));
        return slicingQueueMapper.toDto(queue);
    }

    @Override
    public List<SlicingQueueDto> getAllSlicingQueueByCreatedUserId(String userId) {
        var queue = slicingQueueRepository.findByCreatedByUserId(userId);
        return slicingQueueMapper.toDtoList(queue);
    }

    @Override
    @Async("slicingExecutor")
    @Transactional
    public void processSlicing(UUID queueId) {
        SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
        if (queue == null) {
            log.error("SlicingServiceImpl", "Queue not found: " + queueId);
            return;
        }

        log.info("SlicingServiceImpl",
                String.format("Starting slicing process for queue: %s, model: %s",
                        queueId, queue.getModel().getName()));

        try {
            updateQueueStatus(queue, SlicingStatus.PROCESSING, "Starting slicing process", 0);

            ModelValidation validation = validateModelIfNeeded(queue.getModel());
            if (validation.getHasErrors()) {
                throw new SlicingProcessException("Model validation failed: " + validation.getErrorDetails());
            }
            updateQueueProgress(queue, 10, "Model validated");

            SlicingEngine engine = selectSlicingEngine(queue);
            updateQueueProgress(queue, 20, "Engine selected: " + engine.getName());

            SlicingResult result = executeSlicingWithRetry(engine, queue);
            updateQueueProgress(queue, 80, "Slicing completed, analyzing results");

            SlicingMetric metrics = metricsService.calculateMetrics(result);
            updateQueueProgress(queue, 95, "Metrics calculated");

            createQueueResult(queue, result);

            updateQueueStatus(queue, SlicingStatus.COMPLETED, "Slicing completed successfully", 100);

            log.info("SlicingServiceImpl",
                    String.format("Slicing completed successfully for queue: %s, result: %s, layers: %d, time: %d min",
                            queueId, result.getId(), metrics.getLayerCount(), metrics.getEstimatedPrintTimeMinutes()));

        } catch (Exception e) {
            handleSlicingFailure(queue, e);
        }
    }

    @Retryable(backoff = @Backoff(delay = 30000, multiplier = 2))
    private SlicingResult executeSlicingWithRetry(SlicingEngine engine, SlicingQueue queue) {
        try {
            log.info("SlicingServiceImpl",
                    "Executing slicing with engine: " + engine.getName() + " v" + engine.getVersion());

            return engine.slice(queue.getModel(), queue.getSlicingProperty());

        } catch (Exception e) {
            log.warn("SlicingServiceImpl",
                    "Slicing attempt failed with " + engine.getName() + ": " + e.getMessage());

            // If retries exhausted and fallback enabled, try default engine
            if (fallbackToDefault && !engine.getName().equalsIgnoreCase("default")) {
                log.info("SlicingServiceImpl", "Attempting fallback to default engine");
                SlicingEngine defaultEngine = engineSelector.getDefaultEngine();
                return defaultEngine.slice(queue.getModel(), queue.getSlicingProperty());
            }

            throw new SlicingProcessException("Slicing failed after retries: " + e.getMessage(), e);
        }
    }

    private SlicingEngine selectSlicingEngine(SlicingQueue queue) {
        try {
            SlicingEngine engine = engineSelector.selectEngine(queue.getSlicingProperty(), queue.getModel());
            log.info("SlicingServiceImpl",
                    String.format("Selected engine: %s for model: %s (size: %d bytes)",
                            engine.getName(), queue.getModel().getName(),
                            queue.getModel().getFileResource().getFileSize()));
            return engine;
        } catch (Exception e) {
            log.warn("SlicingServiceImpl", "Engine selection failed, using default: " + e.getMessage());
            return engineSelector.getDefaultEngine();
        }
    }

    private ModelValidation validateModelIfNeeded(Model model) {
        Optional<ModelValidation> existing = modelValidationRepository.findByModelId(model.getId());

        if (existing.isPresent()) {
            return existing.get();
        }

        // Perform basic validation
        return validateModel(model);
    }

    private ModelValidation validateModel(Model model) {
        log.debug("SlicingServiceImpl", "Validating model: " + model.getId());

        ModelValidation.ModelValidationBuilder validationBuilder = ModelValidation.builder()
                .model(model)
                .validatedAt(Instant.now())
                .autoRepairApplied(false);

        boolean hasErrors = false;

        if (model.getFileResource() == null) {
            hasErrors = true;
            validationBuilder.errorDetails(java.util.Map.of("file", "No file resource associated"));
        } else {
            long fileSize = model.getFileResource().getFileSize();
            if (fileSize <= 0 || fileSize > 100 * 1024 * 1024) { // 100MB limit
                hasErrors = true;
                validationBuilder.errorDetails(java.util.Map.of("fileSize", "Invalid file size: " + fileSize));
            }
        }

        ModelValidation validation = validationBuilder
                .hasErrors(hasErrors)
                .isManifold(!hasErrors) // Simplified assumption
                .build();

        return modelValidationRepository.save(validation);
    }

    private void updateQueueStatus(SlicingQueue queue, SlicingStatus status, String message, Integer progress) {
        queue.setStatus(status.getCode());
        queue.setProgressPercentage(progress);

        if (status == SlicingStatus.PROCESSING && queue.getStartedAt() == null) {
            queue.setStartedAt(Instant.now());
        } else if (status == SlicingStatus.COMPLETED || status == SlicingStatus.FAILED) {
            queue.setCompletedAt(Instant.now());
        }

        if (status == SlicingStatus.FAILED) {
            queue.setErrorMessage(message);
        }

        slicingQueueRepository.save(queue);
        log.debug("SlicingServiceImpl",
                String.format("Queue %s: %s (%d%%) - %s", queue.getId(), status, progress, message));
    }

    private void updateQueueProgress(SlicingQueue queue, Integer progress, String message) {
        queue.setProgressPercentage(progress);
        slicingQueueRepository.save(queue);
        log.debug("SlicingServiceImpl",
                String.format("Queue %s progress: %d%% - %s", queue.getId(), progress, message));
    }

    private void createQueueResult(SlicingQueue queue, SlicingResult result) {
        SlicingQueueResult queueResult = SlicingQueueResult.builder()
                .slicingQueue(queue)
                .slicingResult(result)
                .build();

        slicingQueueResultRepository.save(queueResult);

        log.debug("SlicingServiceImpl",
                String.format("Created queue result mapping: queue=%s, result=%s", queue.getId(), result.getId()));
    }

    private void handleSlicingFailure(SlicingQueue queue, Exception e) {
        log.error("SlicingServiceImpl",
                String.format("Slicing failed for queue: %s, model: %s - %s",
                        queue.getId(), queue.getModel().getName(), e.getMessage()));

        updateQueueStatus(queue, SlicingStatus.FAILED, e.getMessage(), null);
    }

    private SlicingProperty findSlicingPropertyById(UUID id) {
        return this.slicingPropertyRepository.findById(id).orElse(null);
    }

    private SlicingResultDto toDto(SlicingResult slicingResult) {
        SlicingResultDto dto = slicingResultMapper.toDto(slicingResult);

        // Add materials information
        List<MaterialDto> materials = this.materialMapper.toDto(
                this.slicingResultMaterialRepository.findSlicingResultMaterialBySlicingResultId(dto.getId())
                        .stream()
                        .map(SlicingResultMaterial::getMaterial)
                        .toList()
        );
        dto.setMaterials(materials);

        return dto;
    }

    public static class SlicingProcessException extends RuntimeException {
        public SlicingProcessException(String message) {
            super(message);
        }

        public SlicingProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}