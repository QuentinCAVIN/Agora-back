-- ============================================================
-- V6 - Creation de la table audit_logs
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_user    VARCHAR(255) NOT NULL,
    target_user   VARCHAR(255),
    action        VARCHAR(255) NOT NULL,
    details       JSONB,
    impersonation BOOLEAN      NOT NULL,
    performed_at  TIMESTAMPTZ  NOT NULL
);
