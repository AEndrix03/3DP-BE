package it.aredegalli.printer.service.kafka.control.command;

import org.springframework.kafka.support.SendResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PrinterCommandService {
    CompletableFuture<SendResult<String, Object>> sendCommand(UUID driverId, String command, Integer priority);
}
