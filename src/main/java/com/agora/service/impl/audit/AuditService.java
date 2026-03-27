package com.agora.service.impl.audit;

import com.agora.entity.audit.AuditLog;
import com.agora.repository.audit.AuditLogRepository;
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

            AuditLog logEntry = AuditLog.builder()
                    .action(action)
                    .adminUser(adminUser)
                    .targetUser(targetUser)
                    .details(json)
                    .impersonation(impersonation)
                    .performedAt(Instant.now())
                    .build();

            repository.save(logEntry);

        } catch (Exception e) {
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}
