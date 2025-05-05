package it.aredegalli.printer.service.slicing;

import it.aredegalli.common.exception.NotFoundException;
import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.mapper.slicing.SlicingResultMapper;
import it.aredegalli.printer.repository.slicing.SlicingResultRepository;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlicingServiceImpl implements SlicingService {

    private final SlicingResultRepository slicingResultRepository;
    private final SlicingResultMapper slicingResultMapper;
    private final LogService log;

    @Override
    public List<SlicingResultDto> getAllSlicingResultBySourceId(UUID sourceId) {
        return slicingResultRepository.findBySourceFile_Id(sourceId)
                .stream()
                .map(slicingResultMapper::toDto)
                .toList();
    }

    @Override
    public SlicingResultDto getSlicingResultById(UUID id) {
        return slicingResultRepository.findById(id)
                .map(slicingResultMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Slicing result not found"));
    }

    @Override
    public void deleteSlicingResultById(UUID id) {
        slicingResultRepository.deleteById(id);
        log.info("SlicingServiceImpl", "Slicing result with ID " + id + " was deleted");
    }

}
