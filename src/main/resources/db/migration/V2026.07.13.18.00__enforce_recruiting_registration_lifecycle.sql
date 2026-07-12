ALTER TABLE public.recruiting_application
    ADD CONSTRAINT recruiting_application_final_pass_track_check
        CHECK (status <> 'FINAL_PASSED' OR accepted_track IS NOT NULL),
    ADD CONSTRAINT recruiting_application_registration_lifecycle_check
        CHECK (
            registration_status = 'NOT_READY'
            OR (status = 'FINAL_PASSED' AND accepted_track IS NOT NULL)
        );

CREATE INDEX ix_recruiting_application_quota_usage
    ON public.recruiting_application (recruiting_round_id, accepted_track, registration_status)
    WHERE registration_status IN ('READY', 'REGISTERED');
