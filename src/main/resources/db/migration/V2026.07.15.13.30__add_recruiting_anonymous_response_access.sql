ALTER TABLE public.recruiting_application
    ADD COLUMN form_response_access_key VARCHAR(128),
    ADD CONSTRAINT recruiting_application_identity_mode_check
        CHECK (
            (
                applicant_member_id IS NOT NULL
                AND form_response_access_key IS NULL
            )
            OR (
                applicant_member_id IS NULL
                AND form_response_access_key IS NOT NULL
                AND BTRIM(form_response_access_key) <> ''
                AND privacy_term_id IS NOT NULL
                AND privacy_agreed_at IS NOT NULL
            )
        );
