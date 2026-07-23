ALTER TABLE public.event_outbox
    VALIDATE CONSTRAINT chk_event_outbox_payload_fingerprint_sha256;
