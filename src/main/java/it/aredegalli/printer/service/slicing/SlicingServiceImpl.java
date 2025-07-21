package it.aredegalli.printer.service.slicing;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.slicing.MaterialDto;
import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.mapper.slicing.MaterialMapper;
import it.aredegalli.printer.mapper.slicing.SlicingResultMapper;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.*;
import it.aredegalli.printer.repository.model.ModelRepository;
import it.aredegalli.printer.repository.slicing.SlicingQueueRepository;
import it.aredegalli.printer.repository.slicing.SlicingQueueResultRepository;
import it.aredegalli.printer.repository.slicing.SlicingResultMaterialRepository;
import it.aredegalli.printer.repository.slicing.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import it.aredegalli.printer.service.slicing.engine.SlicingEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
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
    private final SlicingEngine slicingEngine;
    private final ModelRepository modelRepository;
    private final SlicingQueueResultRepository slicingQueueResultRepository;

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
    public void deleteSlicingResultById(UUID id) {
        slicingResultRepository.deleteById(id);
        log.info("SlicingServiceImpl", "Slicing result with ID " + id + " was deleted");
    }

    // NEW METHODS
    public UUID queueSlicing(UUID modelId, UUID slicingPropertyId, Integer priority) {
        log.info("SlicingServiceImpl", "Queueing slicing for model: " + modelId);

        Model model = modelRepository.findById(modelId)
                .orElseThrow(() -> new RuntimeException("Model not found: " + modelId));

        SlicingProperty property = findSlicingPropertyById(slicingPropertyId);

        SlicingQueue queue = SlicingQueue.builder()
                .model(model)
                .slicingProperty(property)
                .priority(priority != null ? priority : 5)
                .status(SlicingStatus.QUEUED.getCode())
                .createdAt(Instant.now())
                .progressPercentage(0)
                .build();

        queue = slicingQueueRepository.save(queue);
        log.info("SlicingServiceImpl", "Slicing queued with ID: " + queue.getId());
        return queue.getId();
    }

    @Override
    public SlicingQueue getQueueStatus(UUID queueId) {
        return slicingQueueRepository.findById(queueId).orElse(null);
    }

    @Async("slicingExecutor")
    public void processSlicing(UUID queueId) {
        SlicingQueue queue = slicingQueueRepository.findById(queueId).orElse(null);
        if (queue == null) {
            log.error("SlicingServiceImpl", "Queue not found: " + queueId);
            return;
        }

        try {
            // Update status to processing
            queue.setStatus(SlicingStatus.PROCESSING.getCode());
            queue.setStartedAt(Instant.now());
            queue.setProgressPercentage(0);
            slicingQueueRepository.save(queue);

            // Execute slicing using existing FileResource infrastructure
            SlicingResult result = slicingEngine.slice(queue.getModel(), queue.getSlicingProperty());

            // Update progress
            queue.setProgressPercentage(50);
            slicingQueueRepository.save(queue);

            // Mark as completed
            queue.setStatus(SlicingStatus.COMPLETED.getCode());
            queue.setCompletedAt(Instant.now());
            queue.setProgressPercentage(100);
            slicingQueueRepository.save(queue);

            slicingQueueResultRepository.save(SlicingQueueResult.builder()
                    .slicingQueue(queue)
                    .slicingResult(result)
                    .build()
            );

            log.info("SlicingServiceImpl", "Slicing completed for queue: " + queueId);

        } catch (Exception e) {
            log.error("SlicingServiceImpl", "Slicing failed for queue: " + queueId + " - " + e.getMessage());

            queue.setStatus(SlicingStatus.FAILED.getCode());
            queue.setErrorMessage(e.getMessage());
            queue.setCompletedAt(Instant.now());
            slicingQueueRepository.save(queue);
        }
    }

    //TODO
    private SlicingProperty findSlicingPropertyById(UUID id) {
        // Implementation to find SlicingProperty by ID from existing system
        // This would need existing SlicingProperty repository access
        return SlicingProperty.builder()
                .id(id)
                .name("Default Profile")
                .layerHeightMm("0.2")
                .extruderTempC(200)
                .bedTempC(60)
                .build();
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
}