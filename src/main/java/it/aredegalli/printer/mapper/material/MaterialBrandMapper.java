package it.aredegalli.printer.mapper.material;

import it.aredegalli.printer.dto.material.MaterialBrandDto;
import it.aredegalli.printer.model.material.MaterialBrand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MaterialBrandMapper {

    MaterialBrandDto toDto(MaterialBrand materialBrand);

    List<MaterialBrandDto> toDto(List<MaterialBrand> materialBrands);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    MaterialBrand toEntity(MaterialBrandDto materialBrandDto);

    List<MaterialBrand> toEntity(List<MaterialBrandDto> materialBrandDtos);
}