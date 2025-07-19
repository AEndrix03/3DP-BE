package it.aredegalli.printer.service.slicing;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.model.slicing.SlicingQueue;

import java.util.List;
import java.util.UUID;

public interface SlicingService {
    List<SlicingResultDto> getAllSlicingResultBySourceId(UUID sourceId);

    SlicingResultDto getSlicingResultById(UUID id);

    void deleteSlicingResultById(UUID id);

    SlicingQueue getQueueStatus(UUID queueId);

    UUID queueSlicing(UUID modelId, String slicingPropertyId, Integer priority);

    void processSlicing(UUID queueId);
}
