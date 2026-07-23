DROP INDEX CONCURRENTLY IF EXISTS public.idx_event_outbox_retention_failed;

CREATE INDEX CONCURRENTLY idx_event_outbox_retention_failed
    ON public.event_outbox (updated_at, id)
    WHERE status = 'FAILED' AND payload_redacted_at IS NULL;
