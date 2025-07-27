package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;

import java.util.List;
import java.util.UUID;

public interface SlicingService {
    List<SlicingResultDto> getAllSlicingResultBySourceId(UUID sourceId);

    SlicingResultDto getSlicingResultById(UUID id);

    void deleteSlicingResultById(UUID id);

    SlicingQueueDto getQueueStatus(UUID queueId);

    UUID queueSlicing(UUID modelId, UUID slicingPropertyId, String userId, Integer priority);

    List<SlicingQueueDto> getAllSlicingQueueByCreatedUserId(String userId);

    void processSlicing(UUID queueId);
}