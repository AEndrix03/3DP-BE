package it.aredegalli.printer.repository.audit;

import it.aredegalli.printer.model.audit.AuditEventType;
import it.aredegalli.printer.repository.UUIDRepository;

import java.util.Optional;

public interface AuditEventTypeRepository extends UUIDRepository<AuditEventType> {
    Optional<AuditEventType> findByDescription(String description);

}
