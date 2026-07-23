ALTER TABLE public.event_outbox
    ADD COLUMN payload_redacted_at timestamp(6) with time zone,
    ADD COLUMN sanitized_last_error text;
