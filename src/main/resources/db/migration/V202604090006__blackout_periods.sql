CREATE TABLE blackout_periods (
    id               UUID PRIMARY KEY,
    resource_id      UUID REFERENCES resources (id),
    date_from        DATE        NOT NULL,
    date_to          DATE        NOT NULL,
    reason           TEXT        NOT NULL,
    created_by_name  VARCHAR(200),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_blackout_periods_resource_id ON blackout_periods (resource_id);
CREATE INDEX idx_blackout_periods_dates ON blackout_periods (date_from, date_to);
