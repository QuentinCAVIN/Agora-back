package com.agora.entity.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    /** JSONB PostgreSQL : mapper le type JDBC JSON pour éviter l’INSERT en varchar. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @Column(nullable = false)
    private boolean impersonation;

    @Column(nullable = false)
    private Instant performedAt;
}
