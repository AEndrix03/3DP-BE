package it.aredegalli.printer.mapper.model;

import it.aredegalli.printer.dto.model.ModelDto;
import it.aredegalli.printer.model.model.Model;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ModelMapper {

    @Mapping(target = "resourceId", source = "fileResource.id")
    ModelDto toDto(Model model);

    List<ModelDto> toDto(List<Model> models);

}
