CREATE TABLE timeline_events (
    id UUID PRIMARY KEY,

    incident_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    event_timestamp TIMESTAMPTZ NOT NULL,

    payload JSONB NOT NULL
);
