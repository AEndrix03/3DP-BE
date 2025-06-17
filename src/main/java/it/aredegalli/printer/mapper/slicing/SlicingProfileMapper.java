package it.aredegalli.printer.mapper.slicing;

import it.aredegalli.printer.dto.slicing.SlicingProfileDto;
import it.aredegalli.printer.model.slicing.SlicingProfile;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SlicingProfileMapper {

    SlicingProfileDto toDto(SlicingProfile slicingProfile);

    List<SlicingProfileDto> toDto(List<SlicingProfile> slicingProfiles);

    SlicingProfile toEntity(SlicingProfileDto slicingProfileDto);

    List<SlicingProfile> toEntity(List<SlicingProfileDto> slicingProfileDtos);

}