ALTER TABLE fcm_token
    ADD COLUMN IF NOT EXISTS last_validated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_fcm_token_validation_targets
    ON fcm_token (is_active, last_validated_at, id);
