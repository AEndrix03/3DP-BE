package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingPropertyDto;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.property.SlicingPropertyMaterial;
import org.mapstruct.*;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
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

    // ======================================
    // ENTITY TO DTO CONVERSION
    // ======================================

    /**
     * Converts SlicingProperty entity to DTO
     * Custom mapping for material associations -> material IDs
     */
    @Mapping(target = "materialIds", source = "materialAssociations", qualifiedByName = "associationsToMaterialIds")
    @Mapping(target = "configurationSummary", ignore = true) // Computed property
    @Mapping(target = "validForSlicing", ignore = true)
    // Computed property
    SlicingPropertyDto toDto(SlicingProperty entity);

    /**
     * Converts list of entities to DTOs
     */
    List<SlicingPropertyDto> toDtoList(List<SlicingProperty> entities);

    // ======================================
    // DTO TO ENTITY CONVERSION
    // ======================================

    /**
     * Converts SlicingPropertyDto to entity
     * Note: materialAssociations are not mapped directly from materialIds
     * They should be handled in service layer with proper entity management
     */
    @Mapping(target = "materialAssociations", ignore = true) // Handled in service layer
    @Mapping(target = "createdAt", ignore = true) // Managed by @PrePersist
    @Mapping(target = "updatedAt", ignore = true)
    // Managed by @PreUpdate
    SlicingProperty toEntity(SlicingPropertyDto dto);

    /**
     * Converts list of DTOs to entities
     */
    List<SlicingProperty> toEntityList(List<SlicingPropertyDto> dtos);

    // ======================================
    // PARTIAL UPDATE METHODS
    // ======================================

    /**
     * Updates existing entity with DTO data, ignoring null values
     * Useful for PATCH operations
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true) // Never update ID
    @Mapping(target = "materialAssociations", ignore = true) // Handled separately
    @Mapping(target = "createdAt", ignore = true) // Never update creation time
    @Mapping(target = "updatedAt", ignore = true) // Managed by @PreUpdate
    @Mapping(target = "createdByUserId", ignore = true)
    // Never change creator
    void updateEntityFromDTO(@MappingTarget SlicingProperty entity, SlicingPropertyDto dto);

    /**
     * Updates DTO with entity data for response mapping
     * Useful for returning updated data after persistence
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "materialIds", source = "materialAssociations", qualifiedByName = "associationsToMaterialIds")
    void updateDTOFromEntity(@MappingTarget SlicingPropertyDto dto, SlicingProperty entity);

    // ======================================
    // CUSTOM MAPPING METHODS
    // ======================================

    /**
     * Converts SlicingPropertyMaterial associations to list of material UUIDs
     * Extracts materialId from each association
     */
    @Named("associationsToMaterialIds")
    default List<UUID> associationsToMaterialIds(List<SlicingPropertyMaterial> associations) {
        if (CollectionUtils.isEmpty(associations)) {
            return Collections.emptyList();
        }

        return associations.stream()
                .filter(association -> association != null && association.getMaterialId() != null)
                .map(SlicingPropertyMaterial::getMaterialId)
                .distinct() // Remove duplicates if any
                .collect(Collectors.toList());
    }

    /**
     * Converts list of material UUIDs to SlicingPropertyMaterial associations
     * Note: This creates minimal associations with only IDs set
     * Full entity relationships should be handled in service layer
     */
    @Named("materialIdsToAssociations")
    default List<SlicingPropertyMaterial> materialIdsToAssociations(List<UUID> materialIds) {
        if (CollectionUtils.isEmpty(materialIds)) {
            return Collections.emptyList();
        }

        return materialIds.stream()
                .filter(uuid -> uuid != null)
                .distinct()
                .map(materialId -> SlicingPropertyMaterial.builder()
                        .materialId(materialId)
                        .build())
                .collect(Collectors.toList());
    }

    // ======================================
    // SPECIALIZED CONVERSION METHODS
    // ======================================

    /**
     * Creates a new DTO from entity without material associations
     * Useful for lightweight operations or when materials are loaded separately
     */
    @Mapping(target = "materialIds", ignore = true)
    SlicingPropertyDto toDTOWithoutMaterials(SlicingProperty entity);

    /**
     * Creates entity from create request DTO
     * Sets sensible defaults and ignores computed fields
     */
    @Mapping(target = "id", ignore = true) // Generated
    @Mapping(target = "materialAssociations", ignore = true) // Handled in service
    @Mapping(target = "createdAt", ignore = true) // @PrePersist
    @Mapping(target = "updatedAt", ignore = true) // @PrePersist
    @Mapping(target = "isActive", constant = "true")
    // New profiles are active by default
    SlicingProperty toEntityForCreation(SlicingPropertyDto dto);

    /**
     * Maps entity to summary DTO with only essential fields
     * Useful for listing operations where full details aren't needed
     */
    @Mapping(target = "materialIds", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "firstLayerHeightMm", ignore = true)
    @Mapping(target = "lineWidthMm", ignore = true)
    @Mapping(target = "firstLayerSpeedMmS", ignore = true)
    @Mapping(target = "travelSpeedMmS", ignore = true)
    @Mapping(target = "infillSpeedMmS", ignore = true)
    @Mapping(target = "outerWallSpeedMmS", ignore = true)
    @Mapping(target = "innerWallSpeedMmS", ignore = true)
    @Mapping(target = "topBottomSpeedMmS", ignore = true)
    @Mapping(target = "topBottomThicknessMm", ignore = true)
    @Mapping(target = "supportAngleThreshold", ignore = true)
    @Mapping(target = "supportDensityPercentage", ignore = true)
    @Mapping(target = "supportPattern", ignore = true)
    @Mapping(target = "supportZDistanceMm", ignore = true)
    @Mapping(target = "adhesionType", ignore = true)
    @Mapping(target = "brimWidthMm", ignore = true)
    @Mapping(target = "fanSpeedPercentage", ignore = true)
    @Mapping(target = "retractionDistanceMm", ignore = true)
    @Mapping(target = "zhopHeightMm", ignore = true)
    @Mapping(target = "extruderTempC", ignore = true)
    @Mapping(target = "bedTempC", ignore = true)
    @Mapping(target = "advancedSettings", ignore = true)
    SlicingPropertyDto toSummaryDTO(SlicingProperty entity);

    // ======================================
    // AFTER MAPPING PROCESSING
    // ======================================

    /**
     * Post-processing after entity to DTO conversion
     * Handles any additional logic needed after mapping
     */
    @AfterMapping
    default void afterEntityToDTO(@MappingTarget SlicingPropertyDto dto, SlicingProperty entity) {
        // Ensure computed properties are consistent
        if (dto.getIsActive() == null) {
            dto.setIsActive(true);
        }

        // Validate material IDs list
        if (dto.getMaterialIds() != null) {
            dto.setMaterialIds(
                    dto.getMaterialIds().stream()
                            .filter(uuid -> uuid != null)
                            .distinct()
                            .collect(Collectors.toList())
            );
        }
    }

    /**
     * Post-processing after DTO to entity conversion
     * Ensures entity consistency and defaults
     */
    @AfterMapping
    default void afterDTOToEntity(@MappingTarget SlicingProperty entity, SlicingPropertyDto dto) {
        // Ensure boolean fields have defaults
        if (entity.getIsActive() == null) {
            entity.setIsActive(true);
        }
        if (entity.getIsPublic() == null) {
            entity.setIsPublic(false);
        }
        if (entity.getSupportsEnabled() == null) {
            entity.setSupportsEnabled(false);
        }
        if (entity.getBrimEnabled() == null) {
            entity.setBrimEnabled(false);
        }
        if (entity.getFanEnabled() == null) {
            entity.setFanEnabled(true);
        }
        if (entity.getRetractionEnabled() == null) {
            entity.setRetractionEnabled(true);
        }
        if (entity.getZhopEnabled() == null) {
            entity.setZhopEnabled(false);
        }

        // Ensure advanced settings is valid JSON
        if (entity.getAdvancedSettings() == null || entity.getAdvancedSettings().trim().isEmpty()) {
            entity.setAdvancedSettings("{}");
        }
    }
}