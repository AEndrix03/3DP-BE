package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.queue.SlicingQueueDto;
import it.aredegalli.printer.enums.slicing.SlicingStatus;
import it.aredegalli.printer.model.model.Model;
import it.aredegalli.printer.model.slicing.property.SlicingProperty;
import it.aredegalli.printer.model.slicing.queue.SlicingQueue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface SlicingQueueMapper {

    @Mapping(source = "model.id", target = "modelId")
    @Mapping(source = "slicingProperty.id", target = "slicingPropertyId")
    @Mapping(source = "status", target = "status", qualifiedByName = "stringToSlicingStatus")
    SlicingQueueDto toDto(SlicingQueue entity);

    @Mapping(source = "modelId", target = "model", qualifiedByName = "uuidToModel")
    @Mapping(source = "slicingPropertyId", target = "slicingProperty", qualifiedByName = "uuidToSlicingProperty")
    @Mapping(source = "status", target = "status", qualifiedByName = "slicingStatusToString")
    SlicingQueue toEntity(SlicingQueueDto dto);

    List<SlicingQueueDto> toDtoList(List<SlicingQueue> entities);

    List<SlicingQueue> toEntityList(List<SlicingQueueDto> dtos);

    @Named("stringToSlicingStatus")
    default SlicingStatus stringToSlicingStatus(String status) {
        return status != null ? SlicingStatus.valueOf(status) : null;
    }

    @Named("slicingStatusToString")
    default String slicingStatusToString(SlicingStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("uuidToModel")
    default Model uuidToModel(UUID modelId) {
        if (modelId == null) {
            return null;
        }
        Model model = new Model();
        model.setId(modelId);
        return model;
    }

    @Named("uuidToSlicingProperty")
    default SlicingProperty uuidToSlicingProperty(UUID slicingPropertyId) {
        if (slicingPropertyId == null) {
            return null;
        }
        SlicingProperty property = new SlicingProperty();
        property.setId(slicingPropertyId);
        return property;
    }

    @Mapping(source = "modelId", target = "model", qualifiedByName = "uuidToModel")
    @Mapping(source = "slicingPropertyId", target = "slicingProperty", qualifiedByName = "uuidToSlicingProperty")
    @Mapping(source = "status", target = "status", qualifiedByName = "slicingStatusToString")
    void updateEntityFromDto(SlicingQueueDto dto, @MappingTarget SlicingQueue entity);
}