ALTER TABLE public.file_metadata
    ADD CONSTRAINT ck_file_metadata_upload_lifecycle
        CHECK (is_uploaded = (confirmed_at IS NOT NULL)) NOT VALID;

ALTER TABLE public.file_metadata
    VALIDATE CONSTRAINT ck_file_metadata_upload_lifecycle;
