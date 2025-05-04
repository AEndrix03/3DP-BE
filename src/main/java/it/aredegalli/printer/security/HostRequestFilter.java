package it.aredegalli.printer.security;

import it.aredegalli.printer.enums.audit.AuditEventTypeEnum;
import it.aredegalli.printer.service.audit.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class HostRequestFilter extends OncePerRequestFilter {

    @Value("${security.whitelist.ips}")
    private String whitelistIps;

    private final AuditService auditService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String remoteAddr = request.getRemoteAddr();
        List<String> allowedIps = List.of(whitelistIps.split(","));

        boolean isWhitelisted = allowedIps.contains(remoteAddr);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("method", method);
        metadata.put("ip", remoteAddr);
        metadata.put("path", request.getRequestURI());

        if (!isWhitelisted) {
            log.warn("[SECURITY] Tentativo bloccato: IP={} metodo={}", remoteAddr, method);
            auditService.logEvent(null, AuditEventTypeEnum.API_ACCESS_DENIED, "Tentativo non autorizzato da IP: " + remoteAddr, metadata);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accesso riservato");
            return;
        }

        log.info("[SECURITY] Accesso consentito: IP={} metodo={}", remoteAddr, method);
        auditService.logEvent(null, AuditEventTypeEnum.API_ACCESS_GRANTED, "Accesso consentito da IP: " + remoteAddr, metadata);

        filterChain.doFilter(request, response);
    }
}
