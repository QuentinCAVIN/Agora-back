package com.agora.entity.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class Auditable {

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(updatable = false)
    private String createdBy;

    private String updatedBy;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = resolveUser();
        this.updatedBy = resolveUser();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
        this.updatedBy = resolveUser();
    }

    private String resolveUser() {
        // Pas d'injection JPA ici : prévoir plus tard un AuditorAware<String> branché sur
        // com.agora.config.SecurityUtils.tryGetAuthenticatedEmail() pour remplir createdBy/updatedBy.
        return "SYSTEM";
    }
}
