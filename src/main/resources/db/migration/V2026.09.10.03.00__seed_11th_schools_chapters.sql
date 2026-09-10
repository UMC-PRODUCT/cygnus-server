-- 11기 운영 보드의 지부학교 목록을 등록한다. 기존 학교와 기수별 지부 배정은 보존한다.
DO $$
DECLARE
    target_gisu_id BIGINT;
    target_school_id BIGINT;
    target_chapter_id BIGINT;
    assigned_chapter_id BIGINT;
    existing_count BIGINT;
    seed RECORD;
BEGIN
    SELECT id INTO STRICT target_gisu_id
    FROM public.gisu
    WHERE generation = 11
    FOR SHARE;

    -- 학교명과 학교의 기수별 지부 배정에는 UNIQUE 제약이 없어 조회·등록 사이의 변경을 막는다.
    LOCK TABLE public.school, public.chapter, public.chapter_school IN SHARE ROW EXCLUSIVE MODE;

    FOR seed IN
        SELECT * FROM (VALUES
            ('캥거루', '가천대학교'),
            ('캥거루', '서울여자대학교'),
            ('캥거루', '한국항공대학교'),
            ('캥거루', '한성대학교'),
            ('쿼카', '동양미래대학교'),
            ('쿼카', '숭실대학교'),
            ('쿼카', '이화여자대학교'),
            ('쿼카', '인하대학교'),
            ('쿼카', '한양대학교 ERICA'),
            ('수달', '성신여자대학교'),
            ('수달', '숙명여자대학교'),
            ('수달', '중앙대학교'),
            ('수달', '한국외국어대학교'),
            ('아홀로틀', '가톨릭대학교'),
            ('아홀로틀', '덕성여자대학교'),
            ('아홀로틀', '세종대학교'),
            ('아홀로틀', '안양대학교'),
            ('아홀로틀', '홍익대학교 서울캠퍼스'),
            ('티라노', '단국대학교'),
            ('티라노', '동국대학교'),
            ('티라노', '동덕여자대학교'),
            ('티라노', '서경대학교'),
            ('티라노', '홍익대학교 세종캠퍼스'),
            -- 중앙 운영진의 소속 학교는 학교 목록에만 등록하고 지부를 배정하지 않는다.
            (NULL, '인천가톨릭대학교')
        ) AS mapping(chapter_name, school_name)
    LOOP
        SELECT COUNT(*), MIN(id) INTO existing_count, target_school_id
        FROM public.school
        WHERE name = seed.school_name;

        IF existing_count > 1 THEN
            RAISE EXCEPTION '학교명이 중복되어 11기 지부를 배정할 수 없습니다: %', seed.school_name;
        END IF;

        IF target_school_id IS NULL THEN
            INSERT INTO public.school (name, created_at, updated_at)
            VALUES (seed.school_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id INTO target_school_id;
        END IF;

        IF seed.chapter_name IS NULL THEN
            CONTINUE;
        END IF;

        INSERT INTO public.chapter (gisu_id, name, created_at, updated_at)
        VALUES (target_gisu_id, seed.chapter_name, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
        ON CONFLICT (gisu_id, name) DO NOTHING;

        SELECT id INTO STRICT target_chapter_id
        FROM public.chapter
        WHERE gisu_id = target_gisu_id AND name = seed.chapter_name;

        SELECT COUNT(*), MIN(chapter.id) INTO existing_count, assigned_chapter_id
        FROM public.chapter_school chapter_school
        JOIN public.chapter chapter ON chapter.id = chapter_school.chapter_id
        WHERE chapter_school.school_id = target_school_id AND chapter.gisu_id = target_gisu_id;

        IF existing_count > 1 THEN
            RAISE EXCEPTION '11기 학교의 지부 배정이 중복되어 있습니다: %', seed.school_name;
        END IF;

        IF assigned_chapter_id IS NOT NULL AND assigned_chapter_id <> target_chapter_id THEN
            RAISE EXCEPTION '11기 학교가 다른 지부에 배정되어 있습니다: %', seed.school_name;
        END IF;

        IF existing_count = 0 THEN
            INSERT INTO public.chapter_school (chapter_id, school_id, created_at, updated_at)
            VALUES (target_chapter_id, target_school_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
    END LOOP;
END $$;
