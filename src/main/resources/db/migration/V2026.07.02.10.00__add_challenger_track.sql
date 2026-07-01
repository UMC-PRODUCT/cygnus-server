ALTER TABLE public.challenger
    ADD COLUMN track VARCHAR(255);

ALTER TABLE public.challenger
    ALTER COLUMN part DROP NOT NULL;

ALTER TABLE public.challenger
    ADD CONSTRAINT challenger_track_check
        CHECK (
            track IS NULL OR
            track IN (
                'PLAN',
                'DESIGN',
                'WEB_PRODUCT_ENGINEER',
                'MOBILE_PRODUCT_ENGINEER',
                'INFRA_CORE',
                'INFRA_PLUS'
            )
        );
