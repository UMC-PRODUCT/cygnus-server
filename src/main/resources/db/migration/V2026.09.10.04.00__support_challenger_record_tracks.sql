-- 기존 코드와 단일 track을 보존하고, 신규 발급은 tracks를 수강 정보의 기준으로 사용한다.
-- 구버전 서버의 scalar-only 쓰기는 배열에 반영되지 않으므로 전환 중 코드 발급은 중단해야 한다.
ALTER TABLE public.challenger_record
    ADD COLUMN tracks TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];

UPDATE public.challenger_record
SET tracks = ARRAY[track]::TEXT[]
WHERE track IS NOT NULL;

ALTER TABLE public.challenger_record
    DROP CONSTRAINT challenger_record_track_registration_check,
    ALTER COLUMN chapter_id DROP NOT NULL,
    ADD CONSTRAINT challenger_record_tracks_values_check
        CHECK (tracks <@ ARRAY['PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER']::TEXT[]),
    ADD CONSTRAINT challenger_record_tracks_no_null_elements_check
        CHECK (array_position(tracks, NULL) IS NULL),
    ADD CONSTRAINT challenger_record_tracks_no_duplicates_check
        CHECK (
            cardinality(array_positions(tracks, 'PLAN')) <= 1
            AND cardinality(array_positions(tracks, 'DESIGN')) <= 1
            AND cardinality(array_positions(tracks, 'WEB_PRODUCT_ENGINEER')) <= 1
            AND cardinality(array_positions(tracks, 'MOBILE_PRODUCT_ENGINEER')) <= 1
        ),
    ADD CONSTRAINT challenger_record_optional_chapter_check
        CHECK (
            chapter_id IS NOT NULL
            OR (
                challenger_role_type IS NOT NULL
                AND challenger_role_type IN (
                    'CENTRAL_PRESIDENT', 'CENTRAL_VICE_PRESIDENT',
                    'CENTRAL_OPERATING_TEAM_MEMBER', 'CENTRAL_EDUCATION_TEAM_MEMBER'
                )
                AND cardinality(tracks) = 0
                AND track IS NULL
            )
        );
