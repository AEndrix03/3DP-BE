package it.aredegalli.printer.service.printer;

import it.aredegalli.printer.dto.printer.PrinterCreateDto;
import it.aredegalli.printer.dto.printer.PrinterDto;
import it.aredegalli.printer.dto.printer.PrinterFilterDto;

import java.util.List;
import java.util.UUID;

public interface PrinterService {

    PrinterDto getPrinterById(UUID id);

    List<PrinterDto> getAllPrinters();

    UUID createPrinter(PrinterCreateDto printerCreateDto);

    UUID connectDriverToPrinter(UUID printerId, UUID driverId);

    UUID disconnectDriverFromPrinter(UUID printerId);

    PrinterDto getPrinterByName(String name);

    PrinterDto getPrinterByDriverId(UUID driverId);

    UUID renamePrinter(UUID printerId, String newName);

    UUID deletePrinter(UUID printerId);

    List<PrinterDto> searchPrinters(PrinterFilterDto filters);

    List<PrinterDto> getPrintersByStatus(String status);
}