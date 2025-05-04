package it.aredegalli.printer.repository.audit;

import it.aredegalli.printer.model.audit.AuditEventType;
import it.aredegalli.printer.model.audit.AuditLog;
import it.aredegalli.printer.repository.UUIDRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends UUIDRepository<AuditLog> {
    List<AuditLog> findByAssegnatarioId(String assegnatarioId);

    Long deleteByEventTypeAndTimestampBefore(AuditEventType eventType, Instant timestamp);
}
