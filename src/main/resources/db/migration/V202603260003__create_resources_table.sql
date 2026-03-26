-- EXTENSION (optionnel si PostgreSQL récent)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE resources (
                           id UUID PRIMARY KEY,

                           name VARCHAR(255) NOT NULL,
                           description TEXT,

                           resource_type VARCHAR(50) NOT NULL,

                           capacity INTEGER,

                           active BOOLEAN NOT NULL DEFAULT TRUE,

                           accessibility_tags JSONB NOT NULL DEFAULT '[]',

                           deposit_amount_cents DOUBLE PRECISION NOT NULL DEFAULT 0,

                           image_url TEXT,

                           created_at TIMESTAMP NOT NULL,
                           updated_at TIMESTAMP,
                           created_by VARCHAR(100),
                           updated_by VARCHAR(100)
);

-- Index utiles 🔥
CREATE INDEX idx_resources_type ON resources(resource_type);
CREATE INDEX idx_resources_active ON resources(active);
CREATE INDEX idx_resources_capacity ON resources(capacity);
CREATE INDEX idx_resources_tags ON resources USING GIN (accessibility_tags);