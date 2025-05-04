package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.model.slicing.SlicingResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SlicingResultMapper {

    @Mapping(target = "id", source = "generatedFile.id")
    @Mapping(target = "sourceId", source = "sourceFile.id")
    SlicingResultDto toDto(SlicingResult result);

    List<SlicingResultDto> toDto(List<SlicingResult> results);
}
