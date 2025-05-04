package it.aredegalli.printer.mapper.printer;

import it.aredegalli.printer.dto.printer.PrinterDto;
import it.aredegalli.printer.model.printer.Printer;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PrinterMapper {

    PrinterDto toDto(Printer driver);

    Printer toEntity(PrinterDto dto);

    List<PrinterDto> toDtoList(List<Printer> drivers);

    List<Printer> toEntityList(List<PrinterDto> dtos);
}
