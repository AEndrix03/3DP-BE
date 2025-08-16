package it.aredegalli.printer.service.kafka.control.command;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

public interface PrinterCommandService {

    UUID sendCommand(UUID driverId, String command, Integer priority);

    SseEmitter createEmitter(String requestId);
}
