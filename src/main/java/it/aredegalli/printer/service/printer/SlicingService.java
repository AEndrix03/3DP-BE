package it.aredegalli.printer.service.printer;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;

import java.util.List;
import java.util.UUID;

public interface SlicingService {
    List<SlicingResultDto> getAllSlicingResultBySourceId(UUID sourceId);

    SlicingResultDto getSlicingResultById(UUID id);

    void deleteSlicingResultById(UUID id);
}
