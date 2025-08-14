package it.aredegalli.printer.service.kafka.control.status;

import it.aredegalli.printer.dto.kafka.control.status.PrinterPauseRequestDto;
import it.aredegalli.printer.dto.kafka.control.status.PrinterStartRequestDto;
import it.aredegalli.printer.dto.kafka.control.status.PrinterStopRequestDto;
import it.aredegalli.printer.enums.kafka.KafkaTopicEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrinterStatusControlServiceImpl implements PrinterStatusControlService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override()
    public CompletableFuture<SendResult<String, Object>> startPrint(PrinterStartRequestDto request) {
        log.info("Starting printing for printer connected to driver id: {}", request.getDriverId());

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_START_REQUEST.getTopicName(), request.getDriverId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Printer start request sent successfully: offset={}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send printer start request", ex);
                    }
                });
    }

    @Override()
    public CompletableFuture<SendResult<String, Object>> stopPrint(PrinterStopRequestDto request) {
        log.info("Stopping printing for printer connected to driver id: {}", request.getDriverId());

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_STOP_REQUEST.getTopicName(), request.getDriverId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Printer stop request sent successfully: offset={}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send printer stop request", ex);
                    }
                });
    }

    @Override()
    public CompletableFuture<SendResult<String, Object>> pausePrint(PrinterPauseRequestDto request) {
        log.info("Pausing printing for printer connected to driver id: {}", request.getDriverId());

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_PAUSE_REQUEST.getTopicName(), request.getDriverId(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Printer pausing request sent successfully: offset={}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send printer pausing request", ex);
                    }
                });
    }

}
