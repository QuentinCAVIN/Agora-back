package com.agora.config;

import com.agora.service.impl.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

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
                details.put("params", Arrays.stream(pjp.getArgs()).map(this::safeAuditParam).toList());
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
                    resolveActorEmail(),
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
                    resolveActorEmail(),
                    null,
                    details,
                    false
            );

            throw ex;
        }
    }

    private Object safeAuditParam(Object arg) {
        if (arg == null) {
            return null;
        }
        try {
            return objectMapper.valueToTree(arg);
        } catch (IllegalArgumentException ex) {
            return arg.toString();
        }
    }

    /**
     * Acteur réel quand JWT présent ({@link SecurityUtils}), sinon repli pour tâches sans contexte HTTP.
     */
    private String resolveActorEmail() {
        return securityUtils.tryGetAuthenticatedEmail().orElse("SYSTEM");
    }
}
