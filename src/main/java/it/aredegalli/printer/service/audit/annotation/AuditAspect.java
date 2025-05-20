package it.aredegalli.printer.service.audit.annotation;

import it.aredegalli.printer.service.audit.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    private final HttpServletRequest httpRequest;

    /**
     * Around advice that handles auditing for methods annotated with @Audit.
     *
     * @param joinPoint the join point representing the method execution
     * @param audit     the audit annotation instance
     * @return the result of the method execution
     * @throws Throwable if the method execution throws any exception
     */
    @Around("@annotation(audit)")
    public Object handleAudit(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        Map<String, Object> metadata = collectMetadata(joinPoint, audit);

        boolean success = true;
        Object result = null;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            success = false;
            throw ex;
        } finally {
            metadata.put("success", success);
            auditService.logEvent(audit.event(), audit.description(),
                    auditService.buildMetadata(audit.event(), metadata));
        }

        return result;
    }

    /**
     * Collects metadata for auditing purposes.
     *
     * @param joinPoint the join point representing the method execution
     * @param audit     the audit annotation instance
     * @return a map containing metadata key-value pairs
     */
    private Map<String, Object> collectMetadata(ProceedingJoinPoint joinPoint, Audit audit) {
        Map<String, Object> metadata = new HashMap<>();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();

        AuditMetadata[] annotations = method.getAnnotationsByType(AuditMetadata.class);
        for (AuditMetadata meta : annotations) {
            String key = meta.key();
            String value = evaluate(meta.value(), args, paramNames);
            metadata.put(key, value);
        }

        if (audit.callParams()) {
            for (int i = 0; i < paramNames.length; i++) {
                metadata.put("param_" + paramNames[i], args[i] != null ? args[i].toString() : "null");
            }
        }

        metadata.put("ip", httpRequest.getRemoteAddr());
        metadata.put("user-agent", httpRequest.getHeader("User-Agent"));

        return metadata;
    }

    /**
     * Evaluates an expression and returns the result as a string.
     *
     * @param expression the expression to evaluate
     * @param args       the method arguments
     * @param paramNames the method parameter names
     * @return the result of the expression evaluation
     */
    private String evaluate(String expression, Object[] args, String[] paramNames) {
        try {
            if (expression.startsWith("#{args[")) {
                int index = Integer.parseInt(expression.substring(7, expression.indexOf("]")));
                return args[index] != null ? args[index].toString() : "null";
            } else if (expression.equals("#{clientIp}")) {
                return httpRequest.getRemoteAddr();
            } else if (expression.equals("#{userAgent}")) {
                return httpRequest.getHeader("User-Agent");
            } else {
                return expression;
            }
        } catch (Exception e) {
            log.error("Failed to evaluate metadata expression '{}'", expression);
            return "error";
        }
    }
}
