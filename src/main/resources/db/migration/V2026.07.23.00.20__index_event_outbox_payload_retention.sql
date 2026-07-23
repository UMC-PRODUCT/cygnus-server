DROP INDEX CONCURRENTLY IF EXISTS public.idx_event_outbox_retention_published;

CREATE INDEX CONCURRENTLY idx_event_outbox_retention_published
    ON public.event_outbox (published_at, id)
    WHERE status = 'PUBLISHED' AND payload_redacted_at IS NULL;
