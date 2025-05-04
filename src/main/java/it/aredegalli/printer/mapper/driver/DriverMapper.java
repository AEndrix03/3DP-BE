package it.aredegalli.printer.mapper.driver;

import it.aredegalli.printer.dto.driver.DriverDto;
import it.aredegalli.printer.model.driver.Driver;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    DriverDto toDto(Driver driver);

    Driver toEntity(DriverDto dto);

    List<DriverDto> toDtoList(List<Driver> drivers);

    List<Driver> toEntityList(List<DriverDto> dtos);
}
