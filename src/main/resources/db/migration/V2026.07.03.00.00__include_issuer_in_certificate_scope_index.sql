DROP INDEX IF EXISTS idx_certificate_scope_valid;

CREATE INDEX idx_certificate_scope_valid
    ON certificate (type, issuer, recipient_member_id, gisu_id, project_id, merit_title, status, expires_at);
