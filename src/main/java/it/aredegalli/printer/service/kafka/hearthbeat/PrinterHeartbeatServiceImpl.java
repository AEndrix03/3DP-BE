package it.aredegalli.printer.service.kafka.hearthbeat;

import it.aredegalli.printer.dto.kafka.hearthbeat.PrinterHeartbeatRequest;
import it.aredegalli.printer.dto.kafka.hearthbeat.PrinterHeartbeatResponse;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.printer.PrinterStatus;
import it.aredegalli.printer.repository.printer.PrinterRepository;
import it.aredegalli.printer.repository.printer.PrinterStatusRepository;
import it.aredegalli.printer.service.kafka.KafkaTopicEnum;
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
public class PrinterHeartbeatServiceImpl implements PrinterHeartbeatService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PrinterRepository printerRepository;
    private final PrinterStatusRepository printerStatusRepository;

    @Override()
    public CompletableFuture<SendResult<String, Object>> broadcastHeartbeatRequest() {
        log.info("Broadcasting health check to all drivers");

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_HEARTBEATH_REQUEST.getTopicName(), new PrinterHeartbeatRequest())
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Health check broadcast sent successfully: offset={}",
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send health check broadcast", ex);
                    }
                });
    }

    @KafkaListener(topics = "printer-heartbeat-response", groupId = "printer-server")
    public void handlePrinterHearthbeat(ConsumerRecord<String, Object> record) {
        PrinterHeartbeatResponse heartbeat = deserializeHeartbeatResponse(record.value());

        Printer printer = this.printerRepository.findByDriverId(UUID.fromString(heartbeat.getDriverId())).orElse(null);

        if (printer == null) {
            log.warn("[HEARTBEAT] Unknown driver heartbeat. Driver ID: {}", heartbeat.getDriverId());
            return;
        }

        PrinterStatus status = this.printerStatusRepository.findById(heartbeat.getStatusCode()).orElse(null);

        if (status == null) {
            log.warn("[HEARTBEAT] Unknown printer status code for driver {}: {}", heartbeat.getDriverId(), heartbeat.getStatusCode());
            status = this.printerStatusRepository.findById("UNK").orElse(null);
        }

        printer.setStatus(status);
        printer.setLastSeen(Instant.now());

        this.printerRepository.save(printer);
    }

    private static PrinterHeartbeatResponse deserializeHeartbeatResponse(Object payload) {
        PrinterHeartbeatResponse heartbeat;

        if (payload instanceof LinkedHashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            heartbeat = new PrinterHeartbeatResponse(
                    (String) map.get("driverId"),
                    (String) map.get("statusCode")
            );
        } else if (payload instanceof PrinterHeartbeatResponse) {
            heartbeat = (PrinterHeartbeatResponse) payload;
        } else {
            throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
        }
        return heartbeat;
    }

}
