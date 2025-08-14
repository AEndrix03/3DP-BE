package it.aredegalli.printer.service.kafka.control.check;

import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PrinterCheckService {
    CompletableFuture<SendResult<String, Object>> checkPrinter(UUID driverId, UUID jobId, String criteria);
}
