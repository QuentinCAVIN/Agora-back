-- ============================================================
-- V9 - Add resource_type column and backfill from type
-- ============================================================

ALTER TABLE resources
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(255);

UPDATE resources
SET resource_type = type
WHERE resource_type IS NULL AND type IS NOT NULL;
