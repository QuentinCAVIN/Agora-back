-- ============================================================
-- V2 — Enrichissement users + création groups + group_memberships
-- ============================================================

-- UUID helper (PostgreSQL)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Enrichissement de la table users existante
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL,
    ADD COLUMN password_hash  VARCHAR(255),
    ADD COLUMN first_name     VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN last_name      VARCHAR(100) NOT NULL DEFAULT '',
    ADD COLUMN phone          VARCHAR(50),
    ADD COLUMN account_type   VARCHAR(50)  NOT NULL DEFAULT 'AUTONOMOUS',
    ADD COLUMN account_status VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN internal_ref   VARCHAR(100),
    ADD COLUMN notes_admin    TEXT,
    ADD COLUMN created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    ADD COLUMN updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW();

ALTER TABLE users
    ADD CONSTRAINT users_email_unique       UNIQUE (email),
    ADD CONSTRAINT users_internal_ref_unique UNIQUE (internal_ref);

-- 2. Création de la table groups
CREATE TABLE groups (
    id        UUID         PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    is_preset BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT groups_name_unique UNIQUE (name)
);

-- 3. Insertion du groupe preset "Public"
INSERT INTO groups (id, name, is_preset)
VALUES (gen_random_uuid(), 'Public', TRUE);

-- 4. Création de la table group_memberships
CREATE TABLE group_memberships (
    id        UUID        PRIMARY KEY,
    user_id   UUID        NOT NULL,
    group_id  UUID        NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES users (id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES groups (id)
);
