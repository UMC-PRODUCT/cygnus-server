ALTER TABLE fcm_token
    ADD COLUMN platform VARCHAR(30),
    ADD COLUMN device_id VARCHAR(100),
    ADD COLUMN app_version VARCHAR(50),
    ADD COLUMN last_registered_at TIMESTAMP(6) WITH TIME ZONE,
    ADD COLUMN deactivated_at TIMESTAMP(6) WITH TIME ZONE;

UPDATE fcm_token
SET last_registered_at = COALESCE(updated_at, created_at)
WHERE last_registered_at IS NULL;

CREATE INDEX ix_fcm_token_active_token ON fcm_token (fcm_token, is_active);
