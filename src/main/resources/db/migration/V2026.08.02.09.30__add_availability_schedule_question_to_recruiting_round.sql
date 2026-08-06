-- recruiting_round 에 면접 가능 일정(SCHEDULE) 질문 ID 컬럼을 추가하고,
-- availability_form_id <-> availability_schedule_question_id 짝(XOR) 무결성을
-- DB CHECK 제약으로 강제한다.
--
-- 배경(#1219):
--   #1208 이 이미 프로드에 적용된 V2026.07.15.13.30__create_recruiting_domain.sql 을
--   직접 수정해 이 컬럼/제약을 끼워 넣었고, 그 결과 Flyway 체크섬 불일치로 앱 부팅이
--   실패해 Instance Refresh 가 반복 롤백되었다. 해당 마이그레이션은 원본으로 되돌리고,
--   변경은 본 전진 마이그레이션으로 정석 처리한다.

ALTER TABLE public.recruiting_round
    ADD COLUMN availability_schedule_question_id BIGINT;

-- CHECK 제약을 신규 컬럼을 포함한 버전으로 교체한다.
--   기존 프로드 행 중 availability_form_id 만 설정되고 schedule_question 이 NULL 인
--   행이 있으면 즉시 검증 시 ADD CONSTRAINT 가 실패해 배포가 다시 막힌다. 이를 피하기 위해
--   NOT VALID 로 추가한다: 기존 행은 grandfathering, 신규/수정 행부터 강제된다.
--   (짝 무결성은 도메인 계층 RecruitingRoundConfiguration 이 이미 강제하고 있으며,
--    데이터 정합성 확인 후 별도 마이그레이션에서 VALIDATE CONSTRAINT 로 승격 가능)
ALTER TABLE public.recruiting_round DROP CONSTRAINT recruiting_round_schedule_check;
ALTER TABLE public.recruiting_round ADD CONSTRAINT recruiting_round_schedule_check CHECK (
    (
        document_start_at IS NULL
        AND document_end_at IS NULL
        AND document_result_published_at IS NULL
        AND interview_start_at IS NULL
        AND interview_end_at IS NULL
        AND final_result_published_at IS NULL
        AND availability_form_id IS NULL
        AND availability_schedule_question_id IS NULL
        AND NOT interview_required
    )
    OR (
        document_start_at IS NOT NULL
        AND document_end_at IS NOT NULL
        AND document_result_published_at IS NOT NULL
        AND final_result_published_at IS NOT NULL
        AND document_start_at < document_end_at
        AND document_end_at <= document_result_published_at
        AND (
            (
                availability_form_id IS NULL
                AND availability_schedule_question_id IS NULL
            )
            OR (
                availability_form_id IS NOT NULL
                AND availability_schedule_question_id IS NOT NULL
                AND availability_form_id > 0
                AND availability_schedule_question_id > 0
            )
        )
        AND (
            (
                interview_required
                AND interview_start_at IS NOT NULL
                AND interview_end_at IS NOT NULL
                AND document_result_published_at <= interview_start_at
                AND interview_start_at < interview_end_at
                AND interview_end_at <= final_result_published_at
            )
            OR (
                NOT interview_required
                AND interview_start_at IS NULL
                AND interview_end_at IS NULL
                AND availability_form_id IS NULL
                AND availability_schedule_question_id IS NULL
                AND document_result_published_at <= final_result_published_at
            )
        )
    )
) NOT VALID;
