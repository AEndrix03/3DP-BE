package it.aredegalli.printer.repository.audit;

import it.aredegalli.printer.model.audit.AuditEventType;
import it.aredegalli.printer.model.audit.AuditLog;
import it.aredegalli.printer.repository.UUIDRepository;

import java.time.Instant;

public interface AuditLogRepository extends UUIDRepository<AuditLog> {
    Long deleteByEventTypeAndTimestampBefore(AuditEventType eventType, Instant timestamp);
}
