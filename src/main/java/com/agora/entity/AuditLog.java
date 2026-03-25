package com.agora.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Map;
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

    //  qui fait l’action
    @Column(nullable = false)
    private String adminUser;

    //  cible (optionnel)
    private String targetUser;

    //  action métier
    @Column(nullable = false)
    private String action;

    //  détails JSON (payload léger)
    @Column(columnDefinition = "jsonb")
    private String details;

    //  impersonation
    @Column(nullable = false)
    private boolean impersonation;

    // ⏱ timestamp
    @Column(nullable = false)
    private Instant performedAt;
}