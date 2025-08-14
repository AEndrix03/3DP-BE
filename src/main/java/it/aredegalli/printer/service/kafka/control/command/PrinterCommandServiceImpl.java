package it.aredegalli.printer.service.kafka.control.command;

import it.aredegalli.printer.dto.kafka.control.command.PrinterCommandRequestDto;
import it.aredegalli.printer.dto.kafka.control.command.PrinterCommandResponseDto;
import it.aredegalli.printer.enums.kafka.KafkaTopicEnum;
import it.aredegalli.printer.enums.kafka.PrinterCommandExecutionStatusEnum;
import it.aredegalli.printer.model.job.CommandExecution;
import it.aredegalli.printer.repository.job.CommandExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterCommandServiceImpl implements PrinterCommandService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final CommandExecutionRepository commandExecutionRepository;

    @Override()
    public CompletableFuture<SendResult<String, Object>> sendCommand(UUID driverId, String command, Integer priority) {
        log.info("Sending command to printer connected to driver id: {}", driverId);

        CommandExecution exec = this.commandExecutionRepository.save(CommandExecution.builder()
                .driverId(driverId)
                .command(command)
                .priority(priority)
                .status(PrinterCommandExecutionStatusEnum.PENDING)
                .startedAt(Instant.now())
                .build());

        PrinterCommandRequestDto request = PrinterCommandRequestDto.builder()
                .requestId(exec.getId().toString())
                .driverId(driverId.toString())
                .command(command)
                .priority(priority)
                .build();

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_COMMAND_REQUEST.getTopicName(), driverId.toString(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Printer command sent successfully: offset={}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send printer command request", ex);
                    }
                });
    }

    @KafkaListener(topics = "printer-command-response", groupId = "printer-server")
    public void handlePrinterCommandResponse(ConsumerRecord<String, Object> record) {
        PrinterCommandResponseDto response = deserializeCommandResponse(record.value());

        CommandExecution execution = this.commandExecutionRepository.findById(UUID.fromString(response.getRequestId()))
                .orElse(null);

        if (execution == null) {
            log.warn("[COMMAND EXECUTION] Unknown command response. Driver ID: {}, Request ID: {}", response.getDriverId(), response.getRequestId());
            return;
        }

        execution.setOk(response.getOk());
        execution.setStatus(response.getOk() ? PrinterCommandExecutionStatusEnum.COMPLETED : PrinterCommandExecutionStatusEnum.FAILED);
        execution.setException(response.getException());
        execution.setInfo(response.getInfo());
        execution.setFinishedAt(Instant.now());

        this.commandExecutionRepository.save(execution);
        log.info("[COMMAND EXECUTION] Command execution processed successfully for driver {} and request {}", response.getDriverId(), response.getRequestId());
    }

    private static PrinterCommandResponseDto deserializeCommandResponse(Object payload) {
        PrinterCommandResponseDto check;

        if (payload instanceof LinkedHashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            check = PrinterCommandResponseDto.builder()
                    .driverId((String) map.get("driverId"))
                    .requestId((String) map.get("requestId"))
                    .ok((Boolean) map.get("ok"))
                    .exception((String) map.get("exception"))
                    .info((String) map.get("info"))
                    .build();
        } else if (payload instanceof PrinterCommandResponseDto) {
            check = (PrinterCommandResponseDto) payload;
        } else {
            throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
        }
        return check;
    }

}
