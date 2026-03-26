-- ============================================================
-- V4 - Creation de la table resources
-- ============================================================

CREATE TABLE resources (
    id                    UUID PRIMARY KEY,
    name                  VARCHAR(255)  NOT NULL,
    description           VARCHAR(1000),
    type                  VARCHAR(255)  NOT NULL,
    capacity              INTEGER       NOT NULL,
    active                BOOLEAN       NOT NULL DEFAULT TRUE,
    accessibility_tags    TEXT,
    deposit_amount_cents  DOUBLE PRECISION NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ   NOT NULL,
    updated_at            TIMESTAMPTZ   NOT NULL,
    created_by            VARCHAR(255),
    updated_by            VARCHAR(255),
    is_deleted            BOOLEAN       NOT NULL DEFAULT FALSE
);
