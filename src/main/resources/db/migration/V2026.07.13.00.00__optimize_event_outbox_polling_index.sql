DROP INDEX CONCURRENTLY IF EXISTS public.idx_event_outbox_publishable;

CREATE INDEX CONCURRENTLY idx_event_outbox_publishable
    ON public.event_outbox (next_attempt_at, id)
    WHERE status IN ('PENDING', 'PROCESSING');

DROP INDEX CONCURRENTLY IF EXISTS public.idx_event_outbox_pending;
