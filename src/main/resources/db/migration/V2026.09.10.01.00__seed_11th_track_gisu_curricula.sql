-- 11기 기준 데이터를 모든 환경에 동일하게 반영한다. 기존 기수 ID와 학습 이력은 보존한다.
DO $$
DECLARE
    target_gisu_id BIGINT;
BEGIN
    -- generation에는 UNIQUE 제약이 없으므로 조회와 등록 사이의 중복 생성을 막는다.
    LOCK TABLE public.gisu IN SHARE ROW EXCLUSIVE MODE;

    IF (SELECT COUNT(*) FROM public.gisu WHERE generation = 11) > 1 THEN
        RAISE EXCEPTION '11기 기수 데이터가 여러 개입니다. 중복 기수를 먼저 확인해주세요.';
    END IF;

    SELECT id INTO target_gisu_id FROM public.gisu WHERE generation = 11;

    IF target_gisu_id IS NOT NULL AND (
        EXISTS (SELECT 1 FROM public.curriculum WHERE gisu_id = target_gisu_id AND part IS NOT NULL)
        OR EXISTS (SELECT 1 FROM public.study_group WHERE gisu_id = target_gisu_id AND part IS NOT NULL)
        OR EXISTS (
            SELECT 1 FROM public.challenger
            WHERE gisu_id = target_gisu_id AND part IS NOT NULL
                AND (part <> 'ADMIN' OR COALESCE(cardinality(tracks), 0) > 0)
        )
        OR EXISTS (
            SELECT 1 FROM public.challenger_record
            WHERE gisu_id = target_gisu_id AND part IS NOT NULL AND challenger_role_type IS NULL
        )
    ) THEN
        RAISE EXCEPTION '11기에 기존 Part 학습 데이터가 있습니다. Track 전환 정책을 확인해주세요.';
    END IF;

    IF target_gisu_id IS NULL THEN
        INSERT INTO public.gisu (generation, is_active, learning_type, start_at, end_at, created_at, updated_at)
        VALUES (
            11, FALSE, 'TRACK',
            TIMESTAMPTZ '2026-09-01 00:00:00+09',
            TIMESTAMPTZ '2027-02-27 23:59:59.999999+09',
            CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        )
        RETURNING id INTO target_gisu_id;
    ELSE
        UPDATE public.gisu
        SET learning_type = 'TRACK',
            start_at = TIMESTAMPTZ '2026-09-01 00:00:00+09',
            end_at = TIMESTAMPTZ '2027-02-27 23:59:59.999999+09',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = target_gisu_id;
    END IF;

    -- 기존 비수강 운영진의 ID와 권한 연결을 보존하고 레거시 ADMIN Part만 제거한다.
    UPDATE public.challenger
    SET part = NULL, updated_at = CURRENT_TIMESTAMP
    WHERE gisu_id = target_gisu_id AND part = 'ADMIN' AND COALESCE(cardinality(tracks), 0) = 0;

    -- 실제 주차·워크북·미션은 별도로 등록한다. 이미 준비된 커리큘럼의 내용은 덮어쓰지 않는다.
    INSERT INTO public.curriculum (gisu_id, part, track, title, created_at, updated_at)
    SELECT target_gisu_id, NULL, seed.track, seed.title, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM (VALUES
        ('PLAN', '11기 기획 커리큘럼'),
        ('DESIGN', '11기 디자인 커리큘럼'),
        ('WEB_PRODUCT_ENGINEER', '11기 웹 프로덕트 엔지니어 커리큘럼'),
        ('MOBILE_PRODUCT_ENGINEER', '11기 모바일 프로덕트 엔지니어 커리큘럼')
    ) AS seed(track, title)
    ON CONFLICT (gisu_id, track) DO NOTHING;

    -- 활성 기수 부분 UNIQUE 제약을 지키며 이전 기수에서 11기로 전환한다.
    UPDATE public.gisu
    SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP
    WHERE is_active = TRUE AND id <> target_gisu_id;

    UPDATE public.gisu
    SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP
    WHERE id = target_gisu_id;
END $$;
