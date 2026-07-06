CREATE TABLE notifications (
    id UUID PRIMARY KEY,

    incident_id UUID NOT NULL,

    -- Notification category derived from the originating NATS subject.
    type VARCHAR(100) NOT NULL,

    -- Target user for personal notifications (e.g. assignment).
    -- NULL means a broadcast notification visible to everyone.
    recipient_id UUID,

    message TEXT NOT NULL,

    is_read BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL
);

-- The list endpoint filters by recipient (personal + broadcast) and orders by recency.
CREATE INDEX idx_notifications_recipient_created
    ON notifications (recipient_id, created_at DESC);
