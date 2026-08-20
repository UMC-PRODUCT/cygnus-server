ALTER TABLE demoday_booth
    ADD COLUMN booth_code INTEGER NOT NULL,
    ADD CONSTRAINT ck_demoday_booth_code_positive CHECK (booth_code > 0),
    ADD CONSTRAINT uk_demoday_booth_poll_code UNIQUE (demoday_poll_id, booth_code);
