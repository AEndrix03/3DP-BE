package it.aredegalli.printer.mapper.material;

import it.aredegalli.printer.dto.material.MaterialDto;
import it.aredegalli.printer.model.material.Material;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    MaterialDto toDto(Material material);

    List<MaterialDto> toDto(List<Material> materials);

    Material toEntity(MaterialDto materialDto);

    List<Material> toEntity(List<MaterialDto> materialDtos);

}