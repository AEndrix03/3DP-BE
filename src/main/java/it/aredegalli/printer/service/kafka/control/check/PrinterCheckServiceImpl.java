package it.aredegalli.printer.service.kafka.control.check;

import it.aredegalli.printer.dto.kafka.control.check.PrinterCheckRequestDto;
import it.aredegalli.printer.dto.kafka.control.check.PrinterCheckResponseDto;
import it.aredegalli.printer.enums.job.JobStatusEnum;
import it.aredegalli.printer.enums.kafka.KafkaTopicEnum;
import it.aredegalli.printer.model.job.Job;
import it.aredegalli.printer.model.job.JobProgressSnapshot;
import it.aredegalli.printer.model.printer.Printer;
import it.aredegalli.printer.model.printer.PrinterStatus;
import it.aredegalli.printer.repository.job.JobProgressSnapshotRepository;
import it.aredegalli.printer.repository.job.JobRepository;
import it.aredegalli.printer.repository.printer.PrinterRepository;
import it.aredegalli.printer.repository.printer.PrinterStatusRepository;
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
public class PrinterCheckServiceImpl implements PrinterCheckService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JobRepository jobRepository;
    private final PrinterRepository printerRepository;
    private final PrinterStatusRepository printerStatusRepository;
    private final JobProgressSnapshotRepository jobProgressSnapshotRepository;

    @Override()
    public CompletableFuture<SendResult<String, Object>> checkPrinter(UUID driverId, UUID jobId, String criteria) {
        log.info("Checking printer connected to driver id: {}", driverId);

        PrinterCheckRequestDto request = PrinterCheckRequestDto.builder()
                .driverId(driverId.toString())
                .jobId(jobId.toString())
                .criteria(criteria)
                .build();

        return this.kafkaTemplate.send(KafkaTopicEnum.PRINTER_HEARTBEATH_REQUEST.getTopicName(), driverId.toString(), request)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Printer check sent successfully: offset={}",
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send printer check request", ex);
                    }
                });
    }

    @KafkaListener(topics = "printer-check-response", groupId = "printer-server")
    public void handlePrinterCheck(ConsumerRecord<String, Object> record) {
        PrinterCheckResponseDto check = deserializeCheckResponse(record.value());

        Job job = this.jobRepository.findById(UUID.fromString(check.getJobId())).orElse(null);
        Printer printer = this.printerRepository.findByDriverId(UUID.fromString(check.getDriverId())).orElse(null);

        if (printer == null) {
            log.warn("[CHECK] Unknown driver check. Driver ID: {}", check.getDriverId());
            return;
        }

        if (job == null) {
            log.warn("[CHECK] Unknown job check. Driver ID: {}", check.getDriverId());
            return;
        }

        PrinterStatus printerStatus = this.printerStatusRepository.findById(check.getPrinterStatusCode()).orElse(null);

        if (printerStatus == null) {
            log.warn("[CHECK] Unknown printer status code for driver {}: {}", check.getDriverId(), check.getPrinterStatusCode());
            printerStatus = this.printerStatusRepository.findById("UNK").orElse(null);
        }

        printer.setStatus(printerStatus);
        printer.setLastSeen(Instant.now());

        job.setStatus(JobStatusEnum.valueOf(check.getJobStatusCode()));
        job.setProgress(Integer.parseInt(check.getCommandOffset()));

        JobProgressSnapshot progress = JobProgressSnapshot.builder()
                .job(job)
                .recordedAt(Instant.now())
                .statusCode(check.getJobStatusCode())
                .xPosition(parseBigDecimal(check.getXPosition()))
                .yPosition(parseBigDecimal(check.getYPosition()))
                .zPosition(parseBigDecimal(check.getZPosition()))
                .ePosition(parseBigDecimal(check.getEPosition()))
                .feed(parseBigDecimal(check.getFeed()))
                .currentLayer(parseInteger(check.getLayer()))
                .layerHeight(parseBigDecimal(check.getLayerHeight()))
                .extruderStatus(check.getExtruderStatus())
                .extruderTemp(parseBigDecimal(check.getExtruderTemp()))
                .bedTemp(parseBigDecimal(check.getBedTemp()))
                .fanStatus(check.getFanStatus())
                .fanSpeed(parseBigDecimal(check.getFanSpeed()))
                .commandOffset(parseLong(check.getCommandOffset()))
                .lastCommand(check.getLastCommand())
                .averageSpeed(parseBigDecimal(check.getAverageSpeed()))
                .exceptions(check.getExceptions())
                .logs(check.getLogs())
                .localProgressPercentage(null)
                .estimatedRemainingTimeMin(null)
                .materialUsedG(null)
                .errorCount(this.getErrorCountFromLog(check.getLogs()))
                .warningCount(this.getWarningCountFromLog(check.getLogs()))
                .build();

        this.printerRepository.save(printer);
        this.jobRepository.save(job);
        this.jobProgressSnapshotRepository.save(progress);

        log.info("[CHECK] Printer check processed successfully for driver {} and job {}", check.getDriverId(), check.getJobId());
    }

    private Integer getErrorCountFromLog(String checkLog) {
        return checkLog.split("ERR").length;
    }

    private Integer getWarningCountFromLog(String checkLog) {
        return checkLog.split("WARN").length;
    }

    private static PrinterCheckResponseDto deserializeCheckResponse(Object payload) {
        PrinterCheckResponseDto check;

        if (payload instanceof LinkedHashMap<?, ?>) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) payload;
            check = PrinterCheckResponseDto.builder()
                    .jobId((String) map.get("jobId"))
                    .driverId((String) map.get("driverId"))
                    .jobStatusCode((String) map.get("jobStatusCode"))
                    .printerStatusCode((String) map.get("printerStatusCode"))
                    .xPosition((String) map.get("xPosition"))
                    .yPosition((String) map.get("yPosition"))
                    .zPosition((String) map.get("zPosition"))
                    .ePosition((String) map.get("ePosition"))
                    .feed((String) map.get("feed"))
                    .layer((String) map.get("layer"))
                    .layerHeight((String) map.get("layerHeight"))
                    .extruderStatus((String) map.get("extruderStatus"))
                    .extruderTemp((String) map.get("extruderTemp"))
                    .bedTemp((String) map.get("bedTemp"))
                    .fanStatus((String) map.get("fanStatus"))
                    .fanSpeed((String) map.get("fanSpeed"))
                    .commandOffset((String) map.get("commandOffset"))
                    .lastCommand((String) map.get("lastCommand"))
                    .averageSpeed((String) map.get("averageSpeed"))
                    .exceptions((String) map.get("exceptions"))
                    .logs((String) map.get("logs"))
                    .build();
        } else if (payload instanceof PrinterCheckResponseDto) {
            check = (PrinterCheckResponseDto) payload;
        } else {
            throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass());
        }
        return check;
    }

    private static java.math.BigDecimal parseBigDecimal(String value) {
        try {
            return value != null ? new java.math.BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInteger(String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
