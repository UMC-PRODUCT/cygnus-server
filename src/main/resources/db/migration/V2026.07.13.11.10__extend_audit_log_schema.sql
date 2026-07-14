ALTER TABLE public.audit_log
    ADD COLUMN outcome varchar(30) NOT NULL DEFAULT 'SUCCESS',
    ADD COLUMN source varchar(50) NOT NULL DEFAULT 'ANNOTATION',
    ADD COLUMN request_id varchar(100),
    ADD COLUMN trace_id varchar(100);

CREATE INDEX idx_audit_log_outcome_created_at
    ON public.audit_log (outcome, created_at);
CREATE INDEX idx_audit_log_source_created_at
    ON public.audit_log (source, created_at);
CREATE INDEX idx_audit_log_target_created_at
    ON public.audit_log (target_type, target_id, created_at);
CREATE INDEX idx_audit_log_request_id
    ON public.audit_log (request_id);
CREATE INDEX idx_audit_log_trace_id
    ON public.audit_log (trace_id);
