package it.aredegalli.printer.mapper.material;

import it.aredegalli.printer.dto.material.MaterialTypeDto;
import it.aredegalli.printer.model.material.MaterialType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaterialTypeMapper {

    MaterialTypeDto toDto(MaterialType materialType);

    List<MaterialTypeDto> toDto(List<MaterialType> materialTypes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MaterialType toEntity(MaterialTypeDto materialTypeDto);

    List<MaterialType> toEntity(List<MaterialTypeDto> materialTypeDtos);
}