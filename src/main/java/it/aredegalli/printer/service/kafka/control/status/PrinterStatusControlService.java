package it.aredegalli.printer.service.kafka.control.status;

import it.aredegalli.printer.dto.kafka.control.status.PrinterPauseRequestDto;
import it.aredegalli.printer.dto.kafka.control.status.PrinterStartRequestDto;
import it.aredegalli.printer.dto.kafka.control.status.PrinterStopRequestDto;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

public interface PrinterStatusControlService {
    CompletableFuture<SendResult<String, Object>> startPrint(PrinterStartRequestDto request);

    CompletableFuture<SendResult<String, Object>> stopPrint(PrinterStopRequestDto request);

    CompletableFuture<SendResult<String, Object>> pausePrint(PrinterPauseRequestDto request);
}
