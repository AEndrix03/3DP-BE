package it.aredegalli.printer.controller.api;

import it.aredegalli.printer.dto.kafka.control.command.PrinterCommandPreRequestDto;
import it.aredegalli.printer.service.kafka.control.command.PrinterCommandService;
import it.aredegalli.printer.service.log.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/command")
@RequiredArgsConstructor
public class CommandExecutionController {

    private final PrinterCommandService printerCommandService;
    private final LogService log;

    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamEvents(@RequestParam("requestId") String requestId) {
        return printerCommandService.createEmitter(requestId);
    }

    @PostMapping
    public ResponseEntity<UUID> executeCommand(@RequestBody PrinterCommandPreRequestDto commandPreRequestDto) {
        String command = commandPreRequestDto.getCommand();
        String driverId = commandPreRequestDto.getDriverId();
        Integer priority = commandPreRequestDto.getPriority() != null ? commandPreRequestDto.getPriority() : 5;
        log.info("CommandExecutionController", "Executing command: " + command + " for driverId: " + driverId + " with priority: " + priority);
        return ResponseEntity.ok(printerCommandService.sendCommand(UUID.fromString(driverId), command, priority));
    }

}
