package com.agora.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(nullable = false)
    private String adminUser;

    private String targetUser;

    @Column(nullable = false)
    private String action;

    /**
     * {@link SqlTypes#JSON} : validation {@code ddl-auto=validate} vs colonne {@code jsonb} PostgreSQL.
     * {@link AuditDetailsJsonConverter} : persistance / lecture fiables (H2, encodage chaîne).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = AuditDetailsJsonConverter.class)
    private Map<String, Object> details;

    @Column(nullable = false)
    private boolean impersonation;

    @Column(nullable = false)
    private Instant performedAt;
}
