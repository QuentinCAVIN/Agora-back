package com.agora.util;

import com.agora.service.impl.audit.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {

        String action = audited.action();
        Map<String, Object> details = new HashMap<>();

        //  TRACE IDS
        String traceId = MDC.get("traceId");
        String correlationId = MDC.get("correlationId");

        details.put("traceId", traceId);
        details.put("correlationId", correlationId);

        // =========================
        //  PARAMS
        // =========================
        if (audited.logParams()) {
            try {
                details.put("params", pjp.getArgs());
            } catch (Exception ignored) {}
        }

        try {
            Object result = pjp.proceed();

            // =========================
            //  RESULT
            // =========================
            if (audited.logResult() && result != null) {
                try {
                    details.put("result", result);
                } catch (Exception ignored) {}
            }

            // =========================
            //  SUCCESS
            // =========================
            auditService.log(
                    action,
                    resolveUser(),
                    null,
                    details,
                    false
            );

            return result;

        } catch (Exception ex) {

            // =========================
            //  ERROR
            // =========================
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
        return "SYSTEM"; // TODO à brancher JWT plus tard
    }
}