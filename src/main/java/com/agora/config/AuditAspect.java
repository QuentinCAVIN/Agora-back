package com.agora.config;

import com.agora.service.impl.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {

        String action = audited.action();
        Map<String, Object> details = new HashMap<>();

        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");

        details.put("traceId", traceId);
        details.put("correlationId", correlationId);

        if (audited.logParams()) {
            try {
                details.put("params", pjp.getArgs());
            } catch (Exception ignored) {
            }
        }

        try {
            Object result = pjp.proceed();

            if (audited.logResult() && result != null) {
                try {
                    details.put("result", result);
                } catch (Exception ignored) {
                }
            }

            auditService.log(
                    action,
                    resolveUser(),
                    null,
                    details,
                    false
            );

            return result;

        } catch (Exception ex) {

            if (audited.logError()) {
                details.put("error", ex.getMessage());
            }

            auditService.log(
                    action + "_FAILED",
                    resolveUser(),
                    null,
                    details,
                    false
            );

            throw ex;
        }
    }

    private String resolveUser() {
        return "SYSTEM";
    }
}
