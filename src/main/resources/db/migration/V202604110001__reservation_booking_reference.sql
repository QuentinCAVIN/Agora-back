-- Référence métier : YYMMDD (6) + numéro séquentiel sur 5 chiffres (unique par jour de réservation)

ALTER TABLE reservations
    ADD COLUMN IF NOT EXISTS booking_reference VARCHAR(16);

UPDATE reservations r
SET booking_reference = x.ref
FROM (
    SELECT id,
           TO_CHAR(reservation_date, 'YYMMDD')
               || LPAD(
                   ROW_NUMBER() OVER (
                       PARTITION BY reservation_date
                       ORDER BY created_at, id
                   )::text,
                   5,
                   '0'
               ) AS ref
    FROM reservations
    WHERE booking_reference IS NULL
) x
WHERE r.id = x.id;

ALTER TABLE reservations
    ALTER COLUMN booking_reference SET NOT NULL;

CREATE UNIQUE INDEX uq_reservations_booking_reference ON reservations (booking_reference);
