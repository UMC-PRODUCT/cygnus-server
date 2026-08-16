ALTER TABLE refresh_token
    ADD COLUMN client_id VARCHAR(100);

CREATE INDEX idx_refresh_token_client_id ON refresh_token (client_id);
