package com.agora.entity.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String adminUser;

    private String targetUser;

    @Column(nullable = false)
    private String action;

    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(nullable = false)
    private boolean impersonation;

    @Column(nullable = false)
    private Instant performedAt;
}
