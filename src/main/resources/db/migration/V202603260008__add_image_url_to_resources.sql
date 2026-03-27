-- ============================================================
-- V8 - Add image_url column to resources
-- ============================================================

ALTER TABLE resources
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255);
