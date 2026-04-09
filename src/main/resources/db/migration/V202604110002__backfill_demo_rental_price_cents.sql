-- Tarifs catalogue de démonstration : la colonne a été ajoutée après le seed initial (valeurs NULL).
UPDATE resources
SET rental_price_cents = CASE name
    WHEN 'Salle des fêtes — Grande salle' THEN 12000
    WHEN 'Salle polyvalente — Petite salle' THEN 8000
    WHEN 'Vidéoprojecteur Epson EB-X51' THEN 4500
    WHEN 'Chaises pliantes (lot de 50)' THEN 3000
    ELSE rental_price_cents
END
WHERE rental_price_cents IS NULL;
