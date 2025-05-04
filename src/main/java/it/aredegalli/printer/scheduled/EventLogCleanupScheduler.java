package it.aredegalli.printer.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
//@EnableScheduling
@RequiredArgsConstructor
public class EventLogCleanupScheduler {

    // TODO

    /*private final AuditEventTypeRepository auditEventTypeRepository;
    private final AuditLogRepository auditLogRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupAccessDeniedLogs() {
        cleanupLogs(AuditEventTypeEnum.ACCESS_DENIED, 30);
    }

    @Scheduled(cron = "0 0 4 * * *")
    public void cleanupAccessGrantedLogs() {
        cleanupLogs(AuditEventTypeEnum.ACCESS_GRANTED, 14);
    }

    @Scheduled(cron = "0 0 5 * * *")
    public void cleanupGetRequestLogs() {
        cleanupLogs(AuditEventTypeEnum.GET_REQUEST, 7);
    }

    private void cleanupLogs(AuditEventTypeEnum typeEnum, int daysToKeep) {
        AuditEventType eventType = auditEventTypeRepository.findByDescription(typeEnum.name()).orElse(null);
        if (eventType == null) {
            log.warn("[SCHEDULER] Event type {} not found, skipping cleanup", typeEnum);
            return;
        }

        Instant cutoffDate = Instant.now().minus(daysToKeep, ChronoUnit.DAYS);
        Long deleted = auditLogRepository.deleteByEventTypeAndTimestampBefore(eventType, cutoffDate);
        log.info("[SCHEDULER] Deleted {} logs of type {} older than {} days", deleted, typeEnum, daysToKeep);
    }*/
}