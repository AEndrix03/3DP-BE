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
import org.springframework.stereotype.Service;

import java.time.Duration;
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
        Optional.ofNullable(metricsService.getMetricsBySlicingResultId(id))
                .ifPresent(m -> log.debug("SlicingServiceImpl", "Deleting metrics for result: " + id));

        slicingResultRepository.deleteById(id);
        log.info("SlicingServiceImpl", "Slicing result with ID " + id + " was deleted");
    }

    @Override
    @Transactional
    public UUID queueSlicing(UUID modelId, UUID slicingPropertyId, String userId, Integer priority) {
        log.info("SlicingServiceImpl", "Queueing slicing for model: " + modelId);

        Optional<SlicingQueue> existingQueue = findActiveQueueForModel(modelId, slicingPropertyId);
        if (existingQueue.isPresent()) {
            log.warn("SlicingServiceImpl", "Duplicate slicing request detected for model: " + modelId);
            return existingQueue.get().getId();
        }

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new NotFoundException("Model not found: " + modelId));

        SlicingProperty property = slicingPropertyRepository.findById(slicingPropertyId)
                .orElseThrow(() -> new NotFoundException("Slicing property not found: " + slicingPropertyId));

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
    @Transactional
    public void processSlicing(UUID queueId) {
        SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
        if (queue == null) {
            log.error("SlicingServiceImpl", "Queue not found: " + queueId);
            return;
        }

        if (!SlicingStatus.QUEUED.getCode().equals(queue.getStatus())) {
            log.warn("SlicingServiceImpl", "Queue " + queueId + " is already in status: " + queue.getStatus());
            return;
        }

        log.info("SlicingServiceImpl", "Starting slicing process for queue: " + queueId);
        Instant startTime = Instant.now();

        try {
            updateQueueStatus(queue, SlicingStatus.PROCESSING, "Starting slicing process");

            ModelValidation validation = validateModelIfNeeded(queue.getModel());
            if (validation.getHasErrors()) {
                throw new SlicingProcessException("Model validation failed: " + validation.getErrorDetails());
            }

            SlicingEngine engine = engineSelector.selectEngine(queue.getSlicingProperty(), queue.getModel());
            SlicingResult result = engine.slice(queue.getModel(), queue.getSlicingProperty());

            metricsService.calculateMetrics(result);
            createQueueResult(queue, result);

            Duration processingTime = Duration.between(startTime, Instant.now());
            String completionMessage = String.format("Slicing completed in %d seconds. Lines: %d",
                    processingTime.getSeconds(), result.getLines());

            updateQueueStatus(queue, SlicingStatus.COMPLETED, completionMessage);
            log.info("SlicingServiceImpl", completionMessage);

        } catch (Exception e) {
            Duration processingTime = Duration.between(startTime, Instant.now());
            String errorMessage = String.format("Slicing failed after %d seconds: %s",
                    processingTime.getSeconds(), e.getMessage());

            updateQueueStatus(queue, SlicingStatus.FAILED, errorMessage);
            log.error("SlicingServiceImpl", errorMessage);
        }
    }

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
            if (fileSize <= 0 || fileSize > 100 * 1024 * 1024) {
                hasErrors = true;
                validationBuilder.errorDetails(java.util.Map.of("fileSize", "Invalid file size: " + fileSize));
            }
        }

        ModelValidation validation = validationBuilder
                .hasErrors(hasErrors)
                .isManifold(!hasErrors)
                .build();

        return modelValidationRepository.save(validation);
    }

    private void updateQueueStatus(SlicingQueue queue, SlicingStatus status, String message) {
        queue.setStatus(status.getCode());

        if (status == SlicingStatus.PROCESSING && queue.getStartedAt() == null) {
            queue.setStartedAt(Instant.now());
        } else if (status == SlicingStatus.COMPLETED || status == SlicingStatus.FAILED) {
            queue.setCompletedAt(Instant.now());
        }

        if (status == SlicingStatus.FAILED) {
            queue.setErrorMessage(message);
        }

        slicingQueueRepository.save(queue);
    }

    private void createQueueResult(SlicingQueue queue, SlicingResult result) {
        SlicingQueueResult queueResult = SlicingQueueResult.builder()
                .slicingQueue(queue)
                .slicingResult(result)
                .build();

        slicingQueueResultRepository.save(queueResult);
    }

    private SlicingResultDto toDto(SlicingResult slicingResult) {
        SlicingResultDto dto = slicingResultMapper.toDto(slicingResult);

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
    }
}