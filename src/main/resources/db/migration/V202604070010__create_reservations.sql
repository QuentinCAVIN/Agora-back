-- ============================================================
-- V202604070010 - Create reservations table
-- ============================================================

CREATE TABLE reservations (
    id                 UUID         PRIMARY KEY,
    resource_id        UUID         NOT NULL,
    user_id            UUID         NOT NULL,
    reservation_date   DATE         NOT NULL,
    slot_start         TIME         NOT NULL,
    slot_end           TIME         NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    purpose            TEXT,
    group_id           UUID,
    recurring_group_id UUID,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cancelled_at       TIMESTAMPTZ,

    CONSTRAINT fk_reservation_resource FOREIGN KEY (resource_id) REFERENCES resources (id),
    CONSTRAINT fk_reservation_user     FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reservation_group    FOREIGN KEY (group_id) REFERENCES groups (id)
);

CREATE INDEX idx_reservations_resource_id ON reservations(resource_id);
CREATE INDEX idx_reservations_user_id ON reservations(user_id);
CREATE INDEX idx_reservations_reservation_date ON reservations(reservation_date);
CREATE INDEX idx_reservations_status ON reservations(status);
