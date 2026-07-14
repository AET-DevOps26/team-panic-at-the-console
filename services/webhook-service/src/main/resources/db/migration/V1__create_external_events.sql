CREATE TABLE external_events (
    id UUID PRIMARY KEY,
    source VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    delivery_id VARCHAR(255),
    received_at TIMESTAMPTZ NOT NULL,
    raw_payload JSONB NOT NULL,
    published_at TIMESTAMPTZ,
    publish_attempts INT NOT NULL DEFAULT 0
);

-- Redeliveries (e.g. GitHub "Redeliver") carry the same delivery id and must
-- not produce a second External Event.
CREATE UNIQUE INDEX ux_external_events_source_delivery
    ON external_events (source, delivery_id)
    WHERE delivery_id IS NOT NULL;

CREATE INDEX ix_external_events_received_at
    ON external_events (received_at DESC);

-- Publish-retry scan: only unpublished rows are ever selected.
CREATE INDEX ix_external_events_unpublished
    ON external_events (received_at)
    WHERE published_at IS NULL;
