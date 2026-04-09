package com.agora.service.impl.audit;

import com.agora.entity.audit.AuditLog;
import com.agora.repository.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;

    public void log(String action, String adminUser, String targetUser, Map<String, Object> details, boolean impersonation) {
        try {
            AuditLog logEntry = AuditLog.builder()
                    .action(action)
                    .adminUser(adminUser != null ? adminUser : "SYSTEM")
                    .targetUser(targetUser)
                    .details(details)
                    .impersonation(impersonation)
                    .performedAt(Instant.now())
                    .build();

            repository.save(logEntry);

        } catch (Exception e) {
            log.warn("Audit log failed for action={}: {}", action, e.getMessage(), e);
        }
    }
}
