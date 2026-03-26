-- ============================================================
-- V9 - Add resource_type column and backfill from type
-- ============================================================

ALTER TABLE resources
    ADD COLUMN IF NOT EXISTS resource_type VARCHAR(255);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'resources'
          AND column_name = 'type'
    ) THEN
        EXECUTE '
            UPDATE resources
            SET resource_type = type
            WHERE resource_type IS NULL AND type IS NOT NULL
        ';
    END IF;
END $$;
