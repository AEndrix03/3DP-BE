package it.aredegalli.printer.service.audit;

import it.aredegalli.printer.enums.audit.AuditEventTypeEnum;
import it.aredegalli.printer.model.audit.AuditEventType;
import it.aredegalli.printer.model.audit.AuditLog;
import it.aredegalli.printer.repository.audit.AuditEventTypeRepository;
import it.aredegalli.printer.repository.audit.AuditLogRepository;
import it.aredegalli.printer.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;
    private final AuditEventTypeRepository eventTypeRepository;
    private final HttpServletRequest httpServletRequest;

    @Async
    @Override
    public void logEvent(String assegnatarioId, AuditEventTypeEnum eventEnum, String description, Map<String, Object> metadata) {
        AuditEventType eventType = eventTypeRepository.findByDescription(eventEnum.name())
                .orElseGet(() -> eventTypeRepository.save(AuditEventType.builder().description(eventEnum.name()).build()));

        AuditLog audit = AuditLog.builder()
                .assegnatarioId(assegnatarioId)
                .eventType(eventType)
                .description(description)
                .metadata(metadata)
                .timestamp(Instant.now())
                .build();

        log.debug("[AUDIT] Audit event: {}", audit);

        auditLogRepository.save(audit);
    }

    @Override
    public Map<String, Object> buildMetadata(Map<String, Object> metadata) {
        Map<String, Object> map = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        map.put("ip", RequestUtil.getClientIp(httpServletRequest));
        map.put("user-agent", httpServletRequest.getHeader("User-Agent"));

        return map;
    }

    @Override
    public Map<String, Object> buildMetadata(AuditEventTypeEnum eventEnum, Map<String, Object> metadata) {
        Map<String, Object> map = this.buildMetadata(metadata);
        map.put("event", eventEnum.name());
        return map;
    }

    @Override
    public Map<String, Object> buildMetadata(AuditEventTypeEnum eventEnum) {
        return this.buildMetadata(eventEnum, Map.of());
    }

}
