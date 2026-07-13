CREATE INDEX idx_event_outbox_publishable
    ON public.event_outbox (next_attempt_at, id)
    WHERE status IN ('PENDING', 'PROCESSING');

DROP INDEX IF EXISTS public.idx_event_outbox_pending;
