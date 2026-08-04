-- Recruiting QA seed
--
-- 목적
--   * gisu 10의 지부 27~32, 학교 27곳을 사용해 학교/지부별 집계가 보이게 한다.
--   * recruiting 관리 계정, 평가자, 지원자 계정을 만든다.
--   * 지원서 상태, 평가, 면접 일정, 최종 판정 감사 로그를 한 번에 구성한다.
--
-- 전제
--   * PostgreSQL / Flyway 최신 스키마에서 실행한다.
--   * gisu와 chapter_school 데이터가 이미 존재해야 한다.
--   * QA DB에서 1회 실행하는 용도다. 반복 실행이 필요하면 아래 QA 이메일 prefix를 기준으로
--     별도 cleanup 정책을 먼저 정한 뒤 사용한다.
--   * 모든 계정의 비밀번호는 password 이다.
--     password_hash는 Spring DelegatingPasswordEncoder의 bcrypt 형식이다.

BEGIN;

CREATE TEMP TABLE qa_recruiting_context ON COMMIT DROP AS
SELECT
    10::BIGINT AS gisu_id,
    1::BIGINT AS school_id_1,
    2::BIGINT AS school_id_2,
    6::BIGINT AS school_id_3;

CREATE TEMP TABLE qa_recruiting_school (
    school_no INTEGER PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    school_id BIGINT NOT NULL,
    chapter_name TEXT NOT NULL,
    school_name TEXT NOT NULL
) ON COMMIT DROP;

INSERT INTO qa_recruiting_school(school_no, chapter_id, school_id, chapter_name, school_name)
VALUES
    (1, 27, 1, 'Neon', '가천대학교'),
    (2, 27, 11, 'Neon', '동덕여자대학교'),
    (3, 27, 19, 'Neon', '숙명여자대학교'),
    (4, 27, 27, 'Neon', '인하대학교'),
    (5, 27, 32, 'Neon', '한국항공대학교'),
    (6, 28, 2, 'Xenon', '가톨릭대학교'),
    (7, 28, 8, 'Xenon', '단국대학교'),
    (8, 28, 9, 'Xenon', '덕성여자대학교'),
    (9, 28, 29, 'Xenon', '중앙대학교'),
    (10, 28, 33, 'Xenon', '한성대학교'),
    (11, 29, 6, 'Chromium', '광운대학교'),
    (12, 29, 13, 'Chromium', '동양미래대학교'),
    (13, 29, 17, 'Chromium', '서울여자대학교'),
    (14, 29, 30, 'Chromium', '한국공학대학교'),
    (15, 29, 31, 'Chromium', '한국외국어대학교'),
    (16, 30, 10, 'Ferrum', '동국대학교'),
    (17, 30, 25, 'Ferrum', '이화여자대학교'),
    (18, 30, 37, 'Ferrum', '홍익대학교 서울캠퍼스'),
    (19, 30, 38, 'Ferrum', '홍익대학교 세종캠퍼스'),
    (20, 31, 12, 'Platinum', '동아대학교'),
    (21, 31, 23, 'Platinum', '영남대학교'),
    (22, 31, 26, 'Platinum', '인제대학교'),
    (23, 32, 16, 'Selenium', '서경대학교'),
    (24, 32, 18, 'Selenium', '성신여자대학교'),
    (25, 32, 20, 'Selenium', '숭실대학교'),
    (26, 32, 21, 'Selenium', '안양대학교'),
    (27, 32, 34, 'Selenium', '한양대학교 ERICA');

DO $$
DECLARE
BEGIN
    IF NOT EXISTS (SELECT 1 FROM gisu WHERE id = 10 AND is_active = true) THEN
        RAISE EXCEPTION 'gisu_id=10이 현재 활성 기수가 아닙니다.';
    END IF;
    IF (SELECT COUNT(*) FROM qa_recruiting_school) <> 27 THEN
        RAISE EXCEPTION 'QA 학교 매핑이 27개가 아닙니다.';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM qa_recruiting_school q
        WHERE NOT EXISTS (SELECT 1 FROM chapter c WHERE c.id = q.chapter_id AND c.gisu_id = 10)
           OR NOT EXISTS (SELECT 1 FROM school s WHERE s.id = q.school_id)
           OR NOT EXISTS (
               SELECT 1 FROM chapter_school cs
               WHERE cs.chapter_id = q.chapter_id AND cs.school_id = q.school_id
           )
    ) THEN
        RAISE EXCEPTION 'QA 학교/지부 매핑이 현재 DB와 일치하지 않습니다.';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. 테스트 계정
-- -----------------------------------------------------------------------------

-- bcrypt("password")의 QA 전용 hash.
-- 애플리케이션에서 로그인 실패하면 PasswordEncoder 설정에 맞는 hash로 교체한다.
CREATE TEMP TABLE qa_recruiting_member (
    email TEXT PRIMARY KEY,
    member_id BIGINT NOT NULL
) ON COMMIT DROP;

WITH account_seed(name, nickname, email, school_no) AS (
    SELECT * FROM (VALUES
        ('QA 중앙 운영자', 'qa-central', 'qa.recruiting.central@example.test', 1),
        ('QA 학교 운영자', 'qa-school', 'qa.recruiting.school@example.test', 1),
        ('QA 평가자 1', 'qa-evaluator-1', 'qa.recruiting.evaluator1@example.test', 1),
        ('QA 평가자 2', 'qa-evaluator-2', 'qa.recruiting.evaluator2@example.test', 2),
        ('QA 지원자 01', 'qa-applicant-01', 'qa.recruiting.applicant01@example.test', 1),
        ('QA 지원자 02', 'qa-applicant-02', 'qa.recruiting.applicant02@example.test', 1),
        ('QA 지원자 03', 'qa-applicant-03', 'qa.recruiting.applicant03@example.test', 1),
        ('QA 지원자 04', 'qa-applicant-04', 'qa.recruiting.applicant04@example.test', 1),
        ('QA 지원자 05', 'qa-applicant-05', 'qa.recruiting.applicant05@example.test', 2),
        ('QA 지원자 06', 'qa-applicant-06', 'qa.recruiting.applicant06@example.test', 2),
        ('QA 지원자 07', 'qa-applicant-07', 'qa.recruiting.applicant07@example.test', 2),
        ('QA 지원자 08', 'qa-applicant-08', 'qa.recruiting.applicant08@example.test', 2),
        ('QA 지원자 09', 'qa-applicant-09', 'qa.recruiting.applicant09@example.test', 3),
        ('QA 지원자 10', 'qa-applicant-10', 'qa.recruiting.applicant10@example.test', 3),
        ('QA 지원자 11', 'qa-applicant-11', 'qa.recruiting.applicant11@example.test', 3),
        ('QA 지원자 12', 'qa-applicant-12', 'qa.recruiting.applicant12@example.test', 3)
    ) v(name, nickname, email, school_no)
), account_with_school AS (
    SELECT a.name, a.nickname, a.email,
        CASE a.school_no
            WHEN 1 THEN c.school_id_1
            WHEN 2 THEN c.school_id_2
            ELSE c.school_id_3
        END AS school_id
    FROM account_seed a
    CROSS JOIN qa_recruiting_context c
)
INSERT INTO member (
    name, nickname, email, password_hash, school_id, status, created_at, updated_at
)
SELECT
    a.name,
    a.nickname,
    a.email,
    '{bcrypt}$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    a.school_id,
    'ACTIVE',
    NOW(),
    NOW()
FROM account_with_school a
ON CONFLICT (email) DO NOTHING;

INSERT INTO qa_recruiting_member(email, member_id)
SELECT email, id
FROM member
WHERE email LIKE 'qa.recruiting.%@example.test';

-- 지원자·운영자·평가자를 현재 기수의 challenger로 연결한다.
INSERT INTO challenger (member_id, gisu_id, part, status, created_at, updated_at)
SELECT
    m.member_id,
    c.gisu_id,
    CASE
        WHEN m.email LIKE '%applicant01%' OR m.email LIKE '%applicant05%' OR m.email LIKE '%applicant09%' THEN 'PLAN'
        WHEN m.email LIKE '%applicant02%' OR m.email LIKE '%applicant06%' OR m.email LIKE '%applicant10%' THEN 'DESIGN'
        WHEN m.email LIKE '%applicant03%' OR m.email LIKE '%applicant07%' OR m.email LIKE '%applicant11%' THEN 'WEB'
        ELSE 'SPRINGBOOT'
    END,
    'ACTIVE',
    NOW(),
    NOW()
FROM qa_recruiting_member m
CROSS JOIN qa_recruiting_context c
WHERE NOT EXISTS (
    SELECT 1 FROM challenger ch
    WHERE ch.member_id = m.member_id AND ch.gisu_id = c.gisu_id
);

-- 중앙 운영자 권한과 학교 운영자 권한.
INSERT INTO challenger_role (
    challenger_id, role_type, organization_type, organization_id,
    responsible_part, gisu_id, created_at, updated_at
)
SELECT
    ch.id,
    'CENTRAL_OPERATING_TEAM_MEMBER',
    'CENTRAL',
    NULL,
    NULL,
    ch.gisu_id,
    NOW(),
    NOW()
FROM challenger ch
JOIN qa_recruiting_member m ON m.member_id = ch.member_id
CROSS JOIN qa_recruiting_context c
WHERE m.email = 'qa.recruiting.central@example.test'
  AND ch.gisu_id = c.gisu_id
  AND NOT EXISTS (
      SELECT 1 FROM challenger_role r
      WHERE r.challenger_id = ch.id
        AND r.role_type = 'CENTRAL_OPERATING_TEAM_MEMBER'
        AND r.gisu_id = ch.gisu_id
  );

INSERT INTO challenger_role (
    challenger_id, role_type, organization_type, organization_id,
    responsible_part, gisu_id, created_at, updated_at
)
SELECT
    ch.id,
    'SCHOOL_PRESIDENT',
    'SCHOOL',
    c.school_id_1,
    NULL,
    c.gisu_id,
    NOW(),
    NOW()
FROM challenger ch
JOIN qa_recruiting_member m ON m.member_id = ch.member_id
CROSS JOIN qa_recruiting_context c
WHERE m.email = 'qa.recruiting.school@example.test'
  AND ch.gisu_id = c.gisu_id
  AND NOT EXISTS (
      SELECT 1 FROM challenger_role r
      WHERE r.challenger_id = ch.id
        AND r.role_type = 'SCHOOL_PRESIDENT'
        AND r.organization_id = c.school_id_1
        AND r.gisu_id = c.gisu_id
  );

CREATE TEMP TABLE qa_recruiting_actor ON COMMIT DROP AS
SELECT
    MAX(member_id) FILTER (WHERE email = 'qa.recruiting.central@example.test') AS central_member_id,
    MAX(member_id) FILTER (WHERE email = 'qa.recruiting.school@example.test') AS school_member_id,
    MAX(member_id) FILTER (WHERE email = 'qa.recruiting.evaluator1@example.test') AS evaluator_1_id,
    MAX(member_id) FILTER (WHERE email = 'qa.recruiting.evaluator2@example.test') AS evaluator_2_id
FROM qa_recruiting_member;

-- -----------------------------------------------------------------------------
-- 2. Season / quota / round / form
-- -----------------------------------------------------------------------------

CREATE TEMP TABLE qa_recruiting_season (
    school_id BIGINT PRIMARY KEY,
    season_id BIGINT NOT NULL,
    round_id BIGINT NOT NULL,
    application_form_id BIGINT NOT NULL,
    form_id BIGINT NOT NULL
) ON COMMIT DROP;

WITH school_list AS (
    SELECT c.gisu_id, s.school_id
    FROM qa_recruiting_context c
    JOIN qa_recruiting_school s ON true
), inserted_season AS (
    INSERT INTO recruiting_season (gisu_id, school_id, memo, created_at, updated_at)
    SELECT gisu_id, school_id, 'QA Recruiting seed', NOW(), NOW()
    FROM school_list
    ON CONFLICT (gisu_id, school_id) DO UPDATE SET memo = recruiting_season.memo
    RETURNING id, gisu_id, school_id
)
INSERT INTO qa_recruiting_season (school_id, season_id, round_id, application_form_id, form_id)
SELECT i.school_id, i.id, 0, 0, 0
FROM inserted_season i;

-- 기존 QA 실행으로 Season이 이미 있으면 위 INSERT의 RETURNING 결과만으로는 매핑이 부족하므로 보정한다.
UPDATE qa_recruiting_season qs
SET season_id = rs.id
FROM recruiting_season rs
CROSS JOIN qa_recruiting_context c
WHERE rs.gisu_id = c.gisu_id
  AND rs.school_id = qs.school_id;

INSERT INTO recruiting_season_track_quota (
    recruiting_season_id, track, target_count, created_at, updated_at
)
SELECT qs.season_id, tracks.track, 5, NOW(), NOW()
FROM qa_recruiting_season qs
CROSS JOIN (VALUES
    ('PLAN'), ('DESIGN'), ('WEB_PRODUCT_ENGINEER'), ('MOBILE_PRODUCT_ENGINEER')
) tracks(track)
ON CONFLICT (recruiting_season_id, track) DO NOTHING;

CREATE TEMP TABLE qa_recruiting_round (
    school_id BIGINT PRIMARY KEY,
    round_id BIGINT NOT NULL,
    form_id BIGINT NOT NULL
) ON COMMIT DROP;

WITH new_round AS (
    INSERT INTO recruiting_round (
        recruiting_season_id, type, round_no, title, status,
        recruitable_tracks, second_choice_enabled,
        document_start_at, document_end_at, document_result_published_at,
        interview_required, interview_start_at, interview_end_at,
        final_result_published_at, announcement, contact_text,
        created_at, updated_at
    )
    SELECT
        qs.season_id,
        'REGULAR',
        1,
        'QA 본모집 ' || qs.school_id,
        'OPEN',
        ARRAY['PLAN', 'DESIGN', 'WEB_PRODUCT_ENGINEER', 'MOBILE_PRODUCT_ENGINEER']::TEXT[],
        TRUE,
        NOW() - INTERVAL '7 days',
        NOW() + INTERVAL '7 days',
        NOW() + INTERVAL '8 days',
        TRUE,
        NOW() + INTERVAL '9 days',
        NOW() + INTERVAL '10 days',
        NOW() + INTERVAL '11 days',
        'QA 모집 공고입니다.',
        'qa-recruiting@example.test',
        NOW(),
        NOW()
    FROM qa_recruiting_season qs
    ON CONFLICT (recruiting_season_id, type, round_no) DO UPDATE SET title = recruiting_round.title
    RETURNING id, recruiting_season_id
)
INSERT INTO qa_recruiting_round(school_id, round_id, form_id)
SELECT qs.school_id, r.id, 0
FROM qa_recruiting_season qs
JOIN recruiting_round r ON r.recruiting_season_id = qs.season_id
    AND r.type = 'REGULAR' AND r.round_no = 1;

INSERT INTO form (
    is_anonymous, created_at, created_member_id, ends_at_exclusive,
    starts_at, updated_at, description, status, title
)
SELECT
    TRUE, NOW(), a.central_member_id, NOW() + INTERVAL '7 days',
    NOW() - INTERVAL '7 days', NOW(), 'Recruiting QA 지원서 Form',
    'PUBLISHED', 'QA Recruiting Application Form ' || q.school_id
FROM qa_recruiting_round q
CROSS JOIN qa_recruiting_actor a;

UPDATE qa_recruiting_round q
SET form_id = f.id
FROM form f
WHERE f.title = 'QA Recruiting Application Form ' || q.school_id
  AND f.created_member_id = (SELECT central_member_id FROM qa_recruiting_actor)
  AND q.form_id = 0;

UPDATE qa_recruiting_season q
SET round_id = r.round_id, application_form_id = r.round_id, form_id = r.form_id
FROM qa_recruiting_round r
WHERE q.school_id = r.school_id;

-- recruiting_application_form은 round당 하나만 허용된다.
INSERT INTO recruiting_application_form (
    recruiting_round_id, form_id, status, created_at, updated_at
)
SELECT round_id, form_id, 'PUBLISHED', NOW(), NOW()
FROM qa_recruiting_round
ON CONFLICT (recruiting_round_id) DO NOTHING;

UPDATE qa_recruiting_round q
SET form_id = f.form_id
FROM recruiting_application_form f
WHERE f.recruiting_round_id = q.round_id;

INSERT INTO form_section (form_id, title, order_no, target_key, description, type, created_at, updated_at)
SELECT
    q.form_id,
    section.title,
    section.order_no,
    section.target_key,
    section.description,
    'CUSTOM',
    NOW(),
    NOW()
FROM qa_recruiting_round q
CROSS JOIN (VALUES
    ('공통 질문', 0, 'COMMON', '기본 지원 정보'),
    ('PLAN 질문', 1, 'PLAN', 'PLAN 지원자 질문'),
    ('DESIGN 질문', 2, 'DESIGN', 'DESIGN 지원자 질문'),
    ('WEB 질문', 3, 'WEB_PRODUCT_ENGINEER', 'WEB 지원자 질문')
) section(title, order_no, target_key, description)
WHERE NOT EXISTS (
    SELECT 1 FROM form_section fs
    WHERE fs.form_id = q.form_id AND fs.target_key = section.target_key
);

INSERT INTO recruiting_form_section_policy (
    recruiting_application_form_id, form_section_id, type, track, created_at, updated_at
)
SELECT
    f.id,
    fs.id,
    CASE WHEN fs.target_key = 'COMMON' THEN 'COMMON' ELSE 'TRACK' END,
    CASE WHEN fs.target_key = 'COMMON' THEN NULL ELSE fs.target_key END,
    NOW(),
    NOW()
FROM recruiting_application_form f
JOIN qa_recruiting_round q ON q.round_id = f.recruiting_round_id
JOIN form_section fs ON fs.form_id = f.form_id
WHERE NOT EXISTS (
    SELECT 1 FROM recruiting_form_section_policy p
    WHERE p.form_section_id = fs.id
);

-- -----------------------------------------------------------------------------
-- 3. 평가자 / 공통 면접 질문
-- -----------------------------------------------------------------------------

INSERT INTO recruiting_round_evaluator (recruiting_round_id, member_id, created_at, updated_at)
SELECT q.round_id, a.evaluator_1_id, NOW(), NOW()
FROM qa_recruiting_round q CROSS JOIN qa_recruiting_actor a
ON CONFLICT (recruiting_round_id, member_id) DO NOTHING;

INSERT INTO recruiting_round_evaluator (recruiting_round_id, member_id, created_at, updated_at)
SELECT q.round_id, a.evaluator_2_id, NOW(), NOW()
FROM qa_recruiting_round q CROSS JOIN qa_recruiting_actor a
ON CONFLICT (recruiting_round_id, member_id) DO NOTHING;

INSERT INTO recruiting_round_interview_question (
    recruiting_round_id, content, order_no, active,
    creator_member_id, last_modified_by_member_id, created_at, updated_at
)
SELECT
    q.round_id,
    question.content,
    question.order_no,
    question.active,
    a.central_member_id,
    a.central_member_id,
    NOW(),
    NOW()
FROM qa_recruiting_round q
CROSS JOIN qa_recruiting_actor a
CROSS JOIN (VALUES
    ('지원 동기와 최근에 몰입했던 경험을 설명해주세요.', 0, TRUE),
    ('협업 중 갈등을 해결한 경험을 설명해주세요.', 1, TRUE),
    ('QA 비활성 질문입니다.', 2, FALSE)
) question(content, order_no, active)
WHERE NOT EXISTS (
    SELECT 1 FROM recruiting_round_interview_question x
    WHERE x.recruiting_round_id = q.round_id
      AND x.order_no = question.order_no
);

-- -----------------------------------------------------------------------------
-- 4. 지원서 27건: 학교·지부·파트·상태를 섞는다.
-- -----------------------------------------------------------------------------

CREATE TEMP TABLE qa_recruiting_application (
    application_id BIGINT PRIMARY KEY,
    applicant_member_id BIGINT NOT NULL,
    form_response_id BIGINT NOT NULL,
    status TEXT NOT NULL,
    evaluator_id BIGINT NOT NULL
) ON COMMIT DROP;

DO $$
DECLARE
    r RECORD;
    v_form_response_id BIGINT;
    v_application_id BIGINT;
    v_round_id BIGINT;
    v_form_id BIGINT;
    v_application_key TEXT;
    v_first_choice TEXT;
    v_second_choice TEXT;
    v_status TEXT;
    v_accepted_track TEXT;
    v_registration_status TEXT;
BEGIN
    FOR r IN
        SELECT
            ROW_NUMBER() OVER (ORDER BY school_map.school_no, m.member_id) AS no,
            m.member_id,
            m.email,
            q.round_id,
            q.form_id,
            CASE ((ROW_NUMBER() OVER (ORDER BY m.member_id) - 1) % 4)
                WHEN 0 THEN 'PLAN'
                WHEN 1 THEN 'DESIGN'
                WHEN 2 THEN 'WEB_PRODUCT_ENGINEER'
                ELSE 'MOBILE_PRODUCT_ENGINEER'
            END AS first_choice,
            CASE ((ROW_NUMBER() OVER (ORDER BY m.member_id) - 1) % 3)
                WHEN 0 THEN 'DESIGN'
                WHEN 1 THEN 'WEB_PRODUCT_ENGINEER'
                ELSE NULL
            END AS second_choice
        FROM (
            SELECT s.*, ROW_NUMBER() OVER (ORDER BY s.school_no) AS school_rank
            FROM qa_recruiting_school s
        ) school_map
        JOIN (
            SELECT m.*, ROW_NUMBER() OVER (ORDER BY m.member_id) AS applicant_rank
            FROM qa_recruiting_member m
            WHERE m.email LIKE 'qa.recruiting.applicant%@example.test'
        ) m ON m.applicant_rank = ((school_map.school_rank - 1) % 12) + 1
        JOIN qa_recruiting_round q ON q.school_id = school_map.school_id
        ORDER BY m.member_id
    LOOP
        v_first_choice := r.first_choice;
        v_second_choice := CASE
            WHEN r.second_choice = r.first_choice THEN NULL
            ELSE r.second_choice
        END;
        v_status := CASE r.no % 6
            WHEN 0 THEN 'DRAFT'
            WHEN 1 THEN 'SUBMITTED'
            WHEN 2 THEN 'INTERVIEW_ASSIGNED'
            WHEN 3 THEN 'INTERVIEW_SKIPPED'
            WHEN 4 THEN 'FINAL_PASSED'
            ELSE 'FINAL_FAILED'
        END;
        v_accepted_track := CASE
            WHEN v_status = 'FINAL_PASSED' THEN v_first_choice
            ELSE NULL
        END;
        v_registration_status := CASE
            WHEN v_status = 'FINAL_PASSED' AND r.no % 2 = 0 THEN 'REGISTERED'
            WHEN v_status = 'FINAL_PASSED' THEN 'READY'
            ELSE 'NOT_READY'
        END;
        v_application_key := 'QA' || LPAD(r.no::TEXT, 4, '0');

        INSERT INTO form_response (
            form_id, respondent_member_id, status, submitted_at,
            last_saved_at, created_at, updated_at
        ) VALUES (
            r.form_id,
            r.member_id,
            CASE WHEN v_status = 'DRAFT' THEN 'DRAFT' ELSE 'SUBMITTED' END,
            CASE WHEN v_status = 'DRAFT' THEN NULL ELSE NOW() - (r.no || ' days')::INTERVAL END,
            NOW(),
            NOW(),
            NOW()
        ) RETURNING id INTO v_form_response_id;

        INSERT INTO recruiting_application (
            recruiting_round_id, recruiting_application_form_id, form_response_id,
            form_response_access_key, applicant_member_id,
            applicant_name, applicant_email, first_choice, second_choice,
            privacy_term_id, privacy_agreed_at, application_key,
            accepted_track, status, registration_status, submitted_at,
            status_changed_member_id, status_change_reason, status_changed_at,
            created_at, updated_at
        )
        SELECT
            r.round_id,
            f.id,
            v_form_response_id,
            NULL,
            r.member_id,
            'QA지원자' || LPAD(r.no::TEXT, 2, '0'),
            r.email,
            v_first_choice,
            v_second_choice,
            NULL,
            NULL,
            v_application_key,
            v_accepted_track,
            v_status,
            v_registration_status,
            CASE WHEN v_status = 'DRAFT' THEN NULL ELSE NOW() - (r.no || ' days')::INTERVAL END,
            CASE WHEN v_status IN ('FINAL_PASSED', 'FINAL_FAILED') THEN a.central_member_id ELSE NULL END,
            CASE WHEN v_status IN ('FINAL_PASSED', 'FINAL_FAILED') THEN 'QA 최종 판정' ELSE NULL END,
            CASE WHEN v_status IN ('FINAL_PASSED', 'FINAL_FAILED') THEN NOW() - (r.no || ' hours')::INTERVAL ELSE NULL END,
            NOW(),
            NOW()
        FROM recruiting_application_form f
        CROSS JOIN qa_recruiting_actor a
        WHERE f.recruiting_round_id = r.round_id
        RETURNING id INTO v_application_id;

        INSERT INTO qa_recruiting_application(application_id, applicant_member_id, form_response_id, status, evaluator_id)
        SELECT v_application_id, r.member_id, v_form_response_id, v_status, a.evaluator_1_id
        FROM qa_recruiting_actor a;
    END LOOP;
END $$;

-- 지원자별 개별 면접 질문 일부.
INSERT INTO recruiting_application_interview_question (
    recruiting_application_id, content, order_no, active, created_at, updated_at
)
SELECT application_id, '이 지원자에게만 확인할 추가 질문입니다.', 0, TRUE, NOW(), NOW()
FROM qa_recruiting_application
WHERE status IN ('INTERVIEW_ASSIGNED', 'FINAL_PASSED')
  AND NOT EXISTS (
      SELECT 1 FROM recruiting_application_interview_question q
      WHERE q.recruiting_application_id = qa_recruiting_application.application_id
  );

-- -----------------------------------------------------------------------------
-- 5. 평가 현황
-- -----------------------------------------------------------------------------

INSERT INTO recruiting_application_evaluation (
    recruiting_application_id, evaluator_member_id, stage, decision,
    comment, submitted_at, created_at, updated_at
)
SELECT
    q.application_id,
    q.evaluator_id,
    'DOCUMENT',
    CASE WHEN q.status = 'FINAL_FAILED' THEN 'REJECTED' ELSE 'APPROVED' END,
    'QA 서류 평가',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days',
    NOW() - INTERVAL '2 days'
FROM qa_recruiting_application q
WHERE q.status <> 'DRAFT'
ON CONFLICT (recruiting_application_id, evaluator_member_id, stage) DO NOTHING;

INSERT INTO recruiting_application_evaluation (
    recruiting_application_id, evaluator_member_id, stage, decision,
    comment, submitted_at, created_at, updated_at
)
SELECT
    q.application_id,
    CASE WHEN q.application_id % 2 = 0 THEN a.evaluator_2_id ELSE q.evaluator_id END,
    'INTERVIEW',
    CASE WHEN q.status = 'FINAL_FAILED' THEN 'REJECTED' ELSE 'APPROVED' END,
    'QA 면접 평가',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
FROM qa_recruiting_application q
CROSS JOIN qa_recruiting_actor a
WHERE q.status IN ('INTERVIEW_ASSIGNED', 'FINAL_PASSED', 'FINAL_FAILED')
ON CONFLICT (recruiting_application_id, evaluator_member_id, stage) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 6. 면접 일정 상태
-- -----------------------------------------------------------------------------

INSERT INTO recruiting_interview_schedule (
    recruiting_application_id, availability_form_response_id, status,
    starts_at, ends_at, location, contact_snapshot,
    request_mail_status, request_mail_attempts, request_mail_error, request_mail_sent_at,
    confirmation_mail_status, confirmation_mail_attempts, confirmation_mail_error,
    confirmation_mail_sent_at, created_at, updated_at
)
SELECT
    q.application_id,
    CASE WHEN q.application_id % 3 = 0 THEN q.form_response_id ELSE NULL END,
    CASE WHEN q.application_id % 3 = 0 THEN 'CONFIRMED' ELSE 'AVAILABILITY_REQUESTED' END,
    CASE WHEN q.application_id % 3 = 0 THEN NOW() + INTERVAL '2 days' ELSE NULL END,
    CASE WHEN q.application_id % 3 = 0 THEN NOW() + INTERVAL '2 days 1 hour' ELSE NULL END,
    CASE WHEN q.application_id % 3 = 0 THEN 'QA 면접실 A' ELSE NULL END,
    'qa-recruiting@example.test',
    CASE WHEN q.application_id % 3 = 1 THEN 'FAILED' ELSE 'SENT' END,
    CASE WHEN q.application_id % 3 = 1 THEN 2 ELSE 1 END,
    CASE WHEN q.application_id % 3 = 1 THEN 'QA 메일 발송 실패' ELSE NULL END,
    CASE WHEN q.application_id % 3 = 1 THEN NULL ELSE NOW() - INTERVAL '1 day' END,
    CASE WHEN q.application_id % 3 = 0 THEN 'SENT' ELSE 'PENDING' END,
    CASE WHEN q.application_id % 3 = 0 THEN 1 ELSE 0 END,
    NULL,
    CASE WHEN q.application_id % 3 = 0 THEN NOW() - INTERVAL '1 day' ELSE NULL END,
    NOW(),
    NOW()
FROM qa_recruiting_application q
WHERE q.status = 'INTERVIEW_ASSIGNED'
ON CONFLICT (recruiting_application_id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 7. 최종 판정 이력용 audit_log
-- -----------------------------------------------------------------------------

-- Recruiting에는 별도 decision-history 테이블이 없고 지원서에는 최신 상태만 저장되므로,
-- 최종 판정 이력 화면/감사 로그 QA를 위해 audit_log에도 판정 이벤트를 넣는다.
INSERT INTO audit_log (
    domain, action, target_type, target_id, actor_member_id,
    description, details, ip_address, created_at
)
SELECT
    'RECRUITMENT',
    CASE WHEN q.status = 'FINAL_PASSED' THEN 'APPROVE' ELSE 'REJECT' END,
    'RECRUITING_APPLICATION',
    q.application_id::TEXT,
    a.central_member_id,
    CASE WHEN q.status = 'FINAL_PASSED' THEN 'QA 최종 합격 판정' ELSE 'QA 최종 불합격 판정' END,
    jsonb_build_object(
        'source', 'qa-seed',
        'status', q.status,
        'acceptedTrack', CASE WHEN q.status = 'FINAL_PASSED' THEN 'QA' ELSE NULL END,
        'reason', 'QA 최종 판정 이력'
    ),
    '127.0.0.1',
    NOW() - ((q.application_id % 6) || ' days')::INTERVAL
FROM qa_recruiting_application q
CROSS JOIN qa_recruiting_actor a
WHERE q.status IN ('FINAL_PASSED', 'FINAL_FAILED');

-- 시딩 결과 확인용 요약.
SELECT email, member_id, 'password' AS password
FROM qa_recruiting_member
ORDER BY email;

SELECT 'accounts' AS item, COUNT(*)::TEXT AS count FROM qa_recruiting_member
UNION ALL
SELECT 'seasons', COUNT(*)::TEXT FROM recruiting_season WHERE memo = 'QA Recruiting seed'
UNION ALL
SELECT 'applications', COUNT(*)::TEXT FROM qa_recruiting_application
UNION ALL
SELECT 'evaluations', COUNT(*)::TEXT FROM recruiting_application_evaluation e
WHERE e.recruiting_application_id IN (SELECT application_id FROM qa_recruiting_application)
UNION ALL
SELECT 'audit_logs', COUNT(*)::TEXT FROM audit_log
WHERE domain = 'RECRUITMENT' AND details ->> 'source' = 'qa-seed';

COMMIT;

-- 계정
-- qa.recruiting.central@example.test / password
-- qa.recruiting.school@example.test  / password
-- qa.recruiting.evaluator1@example.test / password
-- qa.recruiting.evaluator2@example.test / password
-- qa.recruiting.applicant01@example.test ~ applicant12@example.test / password
