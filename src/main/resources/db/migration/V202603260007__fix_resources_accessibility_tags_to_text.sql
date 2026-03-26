-- ============================================================
-- V7 - Align resources.accessibility_tags with JPA converter
-- ============================================================

ALTER TABLE resources
    ALTER COLUMN accessibility_tags TYPE TEXT
    USING CASE
        WHEN accessibility_tags IS NULL THEN NULL
        ELSE array_to_string(accessibility_tags, ',')
    END;
