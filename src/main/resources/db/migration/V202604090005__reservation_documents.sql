CREATE TABLE reservation_documents (
    id                 UUID PRIMARY KEY,
    reservation_id     UUID        NOT NULL,
    doc_type           VARCHAR(50) NOT NULL,
    original_filename  VARCHAR(500) NOT NULL,
    mime_type          VARCHAR(200) NOT NULL,
    size_bytes         BIGINT      NOT NULL,
    status             VARCHAR(50) NOT NULL,
    sent_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reservation_documents_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations (id)
);

CREATE INDEX idx_reservation_documents_reservation_id ON reservation_documents (reservation_id);
