-- ============================================================
-- V5 - Align resources.accessibility_tags with JPA mapping
-- ============================================================

ALTER TABLE resources
    ALTER COLUMN accessibility_tags TYPE VARCHAR(255)[]
    USING CASE
        WHEN accessibility_tags IS NULL OR accessibility_tags = '' THEN NULL
        ELSE string_to_array(accessibility_tags, ',')
    END;
