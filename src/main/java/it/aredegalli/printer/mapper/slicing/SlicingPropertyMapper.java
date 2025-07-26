package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.property.SlicingPropertyMaterial;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Mapping(target = "materialIds", source = "materialAssociations", qualifiedByName = "associationsToMaterialIds")
    SlicingPropertyDto toDto(SlicingProperty entity);

    /**
     * Converts list of entities to DTOs
     */
    List<SlicingPropertyDto> toDtoList(List<SlicingProperty> entities);

    /**
     * Converts SlicingPropertyMaterial associations to material IDs
     *
     * @param materialAssociations List of SlicingPropertyMaterial associations
     * @return List of material UUIDs
     */
    @Named("associationsToMaterialIds")
    default List<UUID> associationsToMaterialIds(List<SlicingPropertyMaterial> materialAssociations) {
        if (materialAssociations == null) {
            return null;
        }

        return materialAssociations.stream()
                .filter(association -> association != null && association.getMaterial() != null)
                .map(association -> association.getMaterial().getId())
                .collect(Collectors.toList());
    }
}