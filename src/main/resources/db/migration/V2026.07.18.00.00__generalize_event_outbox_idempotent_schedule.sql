ALTER TABLE public.event_outbox
    ADD COLUMN payload_fingerprint varchar(64),
    ADD COLUMN available_at timestamp(6) with time zone,
    ADD CONSTRAINT chk_event_outbox_payload_fingerprint_sha256
        CHECK (payload_fingerprint IS NULL OR payload_fingerprint ~ '^[0-9a-f]{64}$') NOT VALID;
