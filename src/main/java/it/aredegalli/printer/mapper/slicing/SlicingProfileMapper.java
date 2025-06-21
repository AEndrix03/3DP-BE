package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.model.slicing.SlicingProperty;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SlicingProfileMapper {

    SlicingPropertyDto toDto(SlicingProperty slicingProperty);

    List<SlicingPropertyDto> toDto(List<SlicingProperty> slicingProperties);

    SlicingProperty toEntity(SlicingPropertyDto slicingPropertyDto);

    List<SlicingProperty> toEntity(List<SlicingPropertyDto> slicingPropertyDtos);

}