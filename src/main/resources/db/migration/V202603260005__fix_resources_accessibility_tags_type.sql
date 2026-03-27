-- ============================================================
-- V5 - Align resources.accessibility_tags with JPA mapping
-- ============================================================

-- Le mapping JPA utilise un AttributeConverter JSON (colonne TEXT).
-- On s'assure uniquement que la colonne a bien un default JSON vide.
ALTER TABLE resources
    ALTER COLUMN accessibility_tags SET DEFAULT '[]';

UPDATE resources
SET accessibility_tags = '[]'
WHERE accessibility_tags IS NULL OR accessibility_tags = '';
