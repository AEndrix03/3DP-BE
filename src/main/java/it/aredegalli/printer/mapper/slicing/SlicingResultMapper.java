package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingResultDto;
import it.aredegalli.printer.model.slicing.SlicingResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {SlicingProfileMapper.class, MaterialMapper.class})
public interface SlicingResultMapper {

    @Mapping(target = "sourceId", source = "sourceFile.id")
    @Mapping(target = "generatedId", source = "generatedFile.id")
    @Mapping(target = "slicingProperty", source = "slicingProperty")
    @Mapping(target = "materials", ignore = true)
    SlicingResultDto toDto(SlicingResult result);

    List<SlicingResultDto> toDto(List<SlicingResult> results);

}