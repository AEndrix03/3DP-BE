package it.aredegalli.printer.service.printer.detail;

import it.aredegalli.printer.dto.printer.detail.PrinterDetailCreateDto;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailDto;
import it.aredegalli.printer.dto.printer.detail.PrinterDetailSaveDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface PrinterDetailService {
    PrinterDetailDto getPrinterById(UUID printerId);

    @Transactional
    UUID createPrinter(PrinterDetailCreateDto createDto);

    @Transactional
    UUID savePrinter(PrinterDetailSaveDto saveDto);
}
