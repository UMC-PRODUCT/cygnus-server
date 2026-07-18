-- Challenger가 기존 part와 신규 track 목록을 함께 운용할 수 있도록 최종 호환 스키마를 구성한다.
-- 기존 part는 backfill하지 않으며, tracks가 비어 있으면 애플리케이션이 part를 변환해 사용한다.
ALTER TABLE public.challenger
    ALTER COLUMN part DROP NOT NULL,
    ADD COLUMN tracks TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
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
        CHECK (array_position(tracks, NULL) IS NULL),
    ADD CONSTRAINT challenger_tracks_no_duplicates_check
        CHECK (
            cardinality(array_positions(tracks, 'PLAN')) <= 1
            AND cardinality(array_positions(tracks, 'DESIGN')) <= 1
            AND cardinality(array_positions(tracks, 'WEB_PRODUCT_ENGINEER')) <= 1
            AND cardinality(array_positions(tracks, 'MOBILE_PRODUCT_ENGINEER')) <= 1
            AND cardinality(array_positions(tracks, 'INFRA_PLUS')) <= 1
        );
