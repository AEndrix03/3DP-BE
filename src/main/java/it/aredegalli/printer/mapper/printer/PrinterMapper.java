package it.aredegalli.printer.mapper.printer;

import it.aredegalli.printer.dto.printer.PrinterDto;
import it.aredegalli.printer.model.printer.Printer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PrinterMapper {

    @Mapping(target = "status", source = "status.code")
    PrinterDto toDto(Printer driver);

    List<PrinterDto> toDtoList(List<Printer> drivers);

}
