package com.agora.service.impl.audit;

import com.agora.entity.AuditLog;
import com.agora.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public void log(String action, String adminUser, String targetUser, Map<String, Object> details, boolean impersonation) {
        try {
            String json = details != null ? objectMapper.writeValueAsString(details) : null;

            AuditLog log = AuditLog.builder()
                    .action(action)
                    .adminUser(adminUser)
                    .targetUser(targetUser)
                    .details(json)
                    .impersonation(impersonation)
                    .performedAt(Instant.now())
                    .build();

            repository.save(log);

        } catch (Exception e) {
            // ❗ on ne casse jamais le flow métier pour un audit
            // 👉 SLF4J ici
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}