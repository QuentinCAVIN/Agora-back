-- Marqueur « conseil municipal » : validations métier côté API (confirm. réservation déléguée).
-- Alignement : comptes déjà en DELEGATE_ADMIN reçoivent le drapeau admin_support (JWT ROLE_ADMIN_SUPPORT).

ALTER TABLE groups ADD COLUMN IF NOT EXISTS council_powers BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE groups SET council_powers = TRUE WHERE name = 'Conseillers municipaux';

UPDATE users u
SET admin_support = TRUE
WHERE EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.roles = 'DELEGATE_ADMIN'
);
