ALTER TABLE demoday_entry_code
    ADD COLUMN redemption_request_id_hash VARCHAR(64);

ALTER TABLE demoday_entry_code
    ADD CONSTRAINT ck_demoday_entry_code_request_after_redemption
        CHECK (redemption_request_id_hash IS NULL OR redeemed_at IS NOT NULL);
