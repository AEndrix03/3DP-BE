package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.MaterialDto;
import it.aredegalli.printer.model.slicing.Material;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaterialMapper {

    MaterialDto toDto(Material material);

    List<MaterialDto> toDto(List<Material> materials);

    Material toEntity(MaterialDto materialDto);

    List<Material> toEntity(List<MaterialDto> materialDtos);

}