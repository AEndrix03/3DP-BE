package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper for SlicingProperty entity and DTO conversion
 * Handles complex mapping including material associations
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.WARN,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        imports = {UUID.class}
)
public interface SlicingPropertyMapper {

    /**
     * Converts SlicingProperty entity to DTO
     * Custom mapping for material associations -> material IDs
     */
    @Mapping(target = "materialIds", ignore = true)
    SlicingPropertyDto toDto(SlicingProperty entity);

    /**
     * Converts list of entities to DTOs
     */
    List<SlicingPropertyDto> toDtoList(List<SlicingProperty> entities);
}