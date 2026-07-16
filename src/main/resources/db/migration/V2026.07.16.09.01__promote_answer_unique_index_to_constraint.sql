-- 이전 마이그레이션에서 CONCURRENTLY 로 만든 uk_answer_form_response_question 인덱스를
-- UNIQUE 제약으로 승격하여 information_schema.table_constraints 에도 노출되도록 한다.
-- USING INDEX 방식은 인덱스를 재구축하지 않고 constraint 만 추가하므로 lock time 이 짧다.
ALTER TABLE answer
    ADD CONSTRAINT uk_answer_form_response_question
        UNIQUE USING INDEX uk_answer_form_response_question;
