-- ============================================================
-- V4 - Seed de resources (données de démonstration)
-- ============================================================

INSERT INTO resources (
    id,
    name,
    description,
    resource_type,
    capacity,
    active,
    accessibility_tags,
    deposit_amount_cents,
    image_url,
    created_at,
    updated_at,
    created_by,
    updated_by
)
VALUES
(
    gen_random_uuid(),
    'Salle des fêtes — Grande salle',
    'Grande salle pour événements jusqu''à 250 personnes',
    'IMMOBILIER',
    250,
    TRUE,
    '["PMR_ACCESS","PARKING","SOUND_SYSTEM"]',
    15000,
    'https://www.team-business-centers.com/wp-content/uploads/2021/07/Location-salle-de-r%C3%A9union-design-Paris-8-570x500.jpg',
    NOW(),
    NOW(),
    'seed',
    'seed'
),
(
    gen_random_uuid(),
    'Salle polyvalente — Petite salle',
    'Salle polyvalente pour réunions et activités associatives',
    'IMMOBILIER',
    80,
    TRUE,
    '["PMR_ACCESS"]',
    8000,
    'https://www.team-business-centers.com/wp-content/uploads/2021/07/Location-salle-de-r%C3%A9union-design-Paris-8-570x500.jpg',
    NOW(),
    NOW(),
    'seed',
    'seed'
),
(
    gen_random_uuid(),
    'Vidéoprojecteur Epson EB-X51',
    'Vidéoprojecteur portable avec câble HDMI',
    'MOBILIER',
    NULL,
    TRUE,
    '[]',
    5000,
    'https://www.team-business-centers.com/wp-content/uploads/2021/07/Location-salle-de-r%C3%A9union-design-Paris-8-570x500.jpg',
    NOW(),
    NOW(),
    'seed',
    'seed'
),
(
    gen_random_uuid(),
    'Chaises pliantes (lot de 50)',
    'Lot de 50 chaises pliantes — retrait en mairie',
    'MOBILIER',
    NULL,
    TRUE,
    '[]',
    3000,
    'https://www.team-business-centers.com/wp-content/uploads/2021/07/Location-salle-de-r%C3%A9union-design-Paris-8-570x500.jpg',
    NOW(),
    NOW(),
    'seed',
    'seed'
);
