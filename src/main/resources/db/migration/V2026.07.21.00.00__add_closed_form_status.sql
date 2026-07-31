-- Form 수집 종료 상태를 구분해 Recruiting Round CLOSED와 실제 Form 상태를 동기화한다.
ALTER TABLE public.form
    DROP CONSTRAINT form_status_check;

-- CLOSED Form은 기존 응답을 보존하지만 더 이상 신규 작성·수정·제출을 받지 않는다.
ALTER TABLE public.form
    ADD CONSTRAINT form_status_check
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED'));
