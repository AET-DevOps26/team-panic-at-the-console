-- Self-service webhook sources (registered via the frontend Sources page).
-- The HMAC secret must stay recoverable for signature verification, so it is
-- stored as-is; the API returns it only once, on create/rotate.
CREATE TABLE webhook_sources (
    slug VARCHAR(64) PRIMARY KEY,
    secret VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    secret_rotated_at TIMESTAMPTZ
);
