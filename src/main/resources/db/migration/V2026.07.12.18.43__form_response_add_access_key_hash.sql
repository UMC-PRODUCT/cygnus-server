ALTER TABLE form_response
    ADD COLUMN response_access_key_hash VARCHAR(64) NULL;

ALTER TABLE form_response
    ADD CONSTRAINT uk_form_response_access_key_hash
        UNIQUE (response_access_key_hash);
