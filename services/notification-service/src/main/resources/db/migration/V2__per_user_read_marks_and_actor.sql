-- Read state becomes per-user: a broadcast notification marked read by one
-- user must stay unread for everyone else. Read marks live in a join table
-- keyed by (notification, user); a row means "this user has read it".
CREATE TABLE notification_reads (
    notification_id UUID NOT NULL REFERENCES notifications (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    read_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (notification_id, user_id)
);

CREATE INDEX idx_notification_reads_user ON notification_reads (user_id);

-- Preserve read state for personal notifications. The old global is_read flag
-- has no per-user meaning for broadcasts, so those intentionally revert to
-- unread for everyone (a one-time effect of this migration).
INSERT INTO notification_reads (notification_id, user_id, read_at)
SELECT id, recipient_id, now()
FROM notifications
WHERE is_read AND recipient_id IS NOT NULL;

ALTER TABLE notifications DROP COLUMN is_read;

-- The user whose action produced the notification (comment author, assigner,
-- escalator). NULL for machine-triggered events. Used to suppress notifying
-- users about their own actions.
ALTER TABLE notifications ADD COLUMN actor_id UUID;
