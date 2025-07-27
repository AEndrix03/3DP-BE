package it.aredegalli.printer.service.slicing;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.material.MaterialDto;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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

    @Value("${slicing.duplicate-prevention.enabled:true}")
    private boolean duplicatePreventionEnabled;

    // Concurrent processing prevention
    private final ConcurrentHashMap<UUID, ReentrantLock> processingLocks = new ConcurrentHashMap<>();

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

        // Check for duplicate queue entries if enabled
        if (duplicatePreventionEnabled) {
            Optional<SlicingQueue> existingQueue = findActiveQueueForModel(modelId, slicingPropertyId);
            if (existingQueue.isPresent()) {
                log.warn("SlicingServiceImpl",
                        "Duplicate slicing request detected for model: " + modelId +
                                ", returning existing queue: " + existingQueue.get().getId());
                return existingQueue.get().getId();
            }
        }

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new NotFoundException("Model not found: " + modelId));

        SlicingProperty property = findSlicingPropertyById(slicingPropertyId);
        if (property == null) {
            throw new NotFoundException("Slicing property not found: " + slicingPropertyId);
        }

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

    /**
     * Find active queue entry for the same model and properties to prevent duplicates
     */
    private Optional<SlicingQueue> findActiveQueueForModel(UUID modelId, UUID slicingPropertyId) {
        List<SlicingQueue> activeQueues = slicingQueueRepository.findByModelId(modelId);

        return activeQueues.stream()
                .filter(queue -> {
                    String status = queue.getStatus();
                    return (SlicingStatus.QUEUED.getCode().equals(status) ||
                            SlicingStatus.PROCESSING.getCode().equals(status)) &&
                            queue.getSlicingProperty().getId().equals(slicingPropertyId);
                })
                .findFirst();
    }

    @Override
    public SlicingQueueDto getQueueStatus(UUID queueId) {
        var queue = slicingQueueRepository.findById(queueId)
                .orElseThrow(() -> new NotFoundException("SlicingQueue not found"));
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
        // Prevent concurrent processing of the same queue
        ReentrantLock lock = processingLocks.computeIfAbsent(queueId, k -> new ReentrantLock());

        if (!lock.tryLock()) {
            log.warn("SlicingServiceImpl",
                    "Slicing queue " + queueId + " is already being processed, skipping duplicate");
            return;
        }

        try {
            processSlicingInternal(queueId);
        } finally {
            lock.unlock();
            processingLocks.remove(queueId);
        }
    }

    private void processSlicingInternal(UUID queueId) {
        SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
        if (queue == null) {
            log.error("SlicingServiceImpl", "Queue not found: " + queueId);
            return;
        }

        // Check if already processed or processing
        if (!SlicingStatus.QUEUED.getCode().equals(queue.getStatus())) {
            log.warn("SlicingServiceImpl",
                    "Queue " + queueId + " is already in status: " + queue.getStatus() + ", skipping");
            return;
        }

        log.info("SlicingServiceImpl",
                String.format("Starting slicing process for queue: %s, model: %s (size: %.2f MB)",
                        queueId, queue.getModel().getName(),
                        queue.getModel().getFileResource().getFileSize() / 1024.0 / 1024.0));

        Instant startTime = Instant.now();

        try {
            updateQueueStatus(queue, SlicingStatus.PROCESSING, "Starting slicing process", 0);

            // Validation phase
            ModelValidation validation = validateModelIfNeeded(queue.getModel());
            if (validation.getHasErrors()) {
                throw new SlicingProcessException("Model validation failed: " + validation.getErrorDetails());
            }
            updateQueueProgress(queue, 10, "Model validated");

            // Engine selection phase
            SlicingEngine engine = selectSlicingEngine(queue);
            updateQueueProgress(queue, 20, "Engine selected: " + engine.getName());

            // Slicing phase
            updateQueueProgress(queue, 30, "Starting slicing with " + engine.getName());
            SlicingResult result = executeSlicingWithRetry(engine, queue);
            updateQueueProgress(queue, 80, "Slicing completed, analyzing results");

            // Metrics calculation phase
            SlicingMetric metrics = metricsService.calculateMetrics(result);
            updateQueueProgress(queue, 95, "Metrics calculated");

            // Finalization phase
            createQueueResult(queue, result);

            Duration processingTime = Duration.between(startTime, Instant.now());
            String completionMessage = String.format(
                    "Slicing completed successfully in %d seconds. Lines: %d, Est. print time: %d min",
                    processingTime.getSeconds(), result.getLines(), metrics.getEstimatedPrintTimeMinutes());

            updateQueueStatus(queue, SlicingStatus.COMPLETED, completionMessage, 100);

            log.info("SlicingServiceImpl", completionMessage + " (Queue: " + queueId + ")");

        } catch (Exception e) {
            Duration processingTime = Duration.between(startTime, Instant.now());
            handleSlicingFailure(queue, e, processingTime);
        }
    }

    @Retryable(
            value = {SlicingProcessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 10000, multiplier = 2.0)
    )
    private SlicingResult executeSlicingWithRetry(SlicingEngine engine, SlicingQueue queue) {
        try {
            log.info("SlicingServiceImpl",
                    "Executing slicing with engine: " + engine.getName() + " v" + engine.getVersion());

            return engine.slice(queue.getModel(), queue.getSlicingProperty());

        } catch (Exception e) {
            log.warn("SlicingServiceImpl",
                    "Slicing attempt failed with " + engine.getName() + ": " + e.getMessage());

            // If retries exhausted and fallback enabled, try default engine
            if (fallbackToDefault && !engine.getName().toLowerCase().contains("default")) {
                log.info("SlicingServiceImpl", "Attempting fallback to default engine");
                SlicingEngine defaultEngine = engineSelector.getDefaultEngine();

                if (!defaultEngine.getName().equals(engine.getName())) {
                    return defaultEngine.slice(queue.getModel(), queue.getSlicingProperty());
                }
            }

            throw new SlicingProcessException("Slicing failed: " + e.getMessage(), e);
        }
    }

    private SlicingEngine selectSlicingEngine(SlicingQueue queue) {
        try {
            SlicingEngine engine = engineSelector.selectEngine(queue.getSlicingProperty(), queue.getModel());
            log.info("SlicingServiceImpl",
                    String.format("Selected engine: %s for model: %s (size: %.2f MB)",
                            engine.getName(), queue.getModel().getName(),
                            queue.getModel().getFileResource().getFileSize() / 1024.0 / 1024.0));
            return engine;
        } catch (Exception e) {
            log.warn("SlicingServiceImpl", "Engine selection failed, using default: " + e.getMessage());
            return engineSelector.getDefaultEngine();
        }
    }

    private ModelValidation validateModelIfNeeded(Model model) {
        Optional<ModelValidation> existing = modelValidationRepository.findByModelId(model.getId());
        return existing.orElseGet(() -> validateModel(model));
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
                String.format("Queue %s: %s (%s%%) - %s",
                        queue.getId(), status,
                        progress != null ? progress : "null", message));
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

    private void handleSlicingFailure(SlicingQueue queue, Exception e, Duration processingTime) {
        String errorMessage = String.format("Slicing failed after %d seconds: %s",
                processingTime.getSeconds(), e.getMessage());

        log.error("SlicingServiceImpl",
                String.format("Slicing failed for queue: %s, model: %s - %s",
                        queue.getId(), queue.getModel().getName(), errorMessage));

        updateQueueStatus(queue, SlicingStatus.FAILED, errorMessage, null);
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