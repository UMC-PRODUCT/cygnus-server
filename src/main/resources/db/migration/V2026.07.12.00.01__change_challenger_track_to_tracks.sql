ALTER TABLE public.challenger
    ADD COLUMN tracks TEXT[];

UPDATE public.challenger
SET tracks = CASE
    WHEN track IS NULL THEN ARRAY[]::TEXT[]
    ELSE ARRAY[track]::TEXT[]
END;

ALTER TABLE public.challenger
    ALTER COLUMN tracks SET NOT NULL;

-- Legacy part fallback을 위해 빈 배열은 허용하되, 배열 원소의 null과 미지원 값은 거부한다.
ALTER TABLE public.challenger
    ADD CONSTRAINT challenger_tracks_values_check
        CHECK (
            tracks <@ ARRAY[
                'PLAN',
                'DESIGN',
                'WEB_PRODUCT_ENGINEER',
                'MOBILE_PRODUCT_ENGINEER',
                'INFRA_PLUS'
            ]::TEXT[]
        ),
    ADD CONSTRAINT challenger_tracks_no_null_elements_check
        CHECK (array_position(tracks, NULL) IS NULL);

ALTER TABLE public.challenger
    DROP CONSTRAINT IF EXISTS challenger_track_check,
    DROP COLUMN track;
