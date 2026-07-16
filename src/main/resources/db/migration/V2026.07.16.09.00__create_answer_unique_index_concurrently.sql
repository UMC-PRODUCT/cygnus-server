-- answer 테이블 (form_response_id, question_id) 유니크 인덱스 생성.
-- 한 응답(form_response) 안에서 같은 질문(question)에 대한 답변이 두 번 저장되는 것을 DB 레벨에서 차단하기 위함.
-- 이후 별도 마이그레이션에서 이 인덱스를 UNIQUE 제약으로 승격한다.
--
-- CREATE INDEX CONCURRENTLY 는 Flyway 트랜잭션 안에서 실행 불가하므로
-- executeInTransaction=false 로 명시. 이 파일에는 다른 DDL 을 함께 두지 않는다.

-- flyway:executeInTransaction=false

CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uk_answer_form_response_question
    ON answer (form_response_id, question_id);
