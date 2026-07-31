-- answer 테이블 (form_response_id, question_id) 에 UNIQUE 제약을 추가한다.
-- 한 응답 안에서 같은 질문에 답변이 두 번 저장되는 것을 DB 레벨에서 차단한다.
--
-- Testcontainers 공유 컨테이너 환경에서 CREATE INDEX CONCURRENTLY 가 서버 전역
-- 트랜잭션을 대기하며 hang 되는 제약이 있어, 일반 UNIQUE 제약으로 적용한다.
-- 현재 answer 규모에서 lock 시간은 밀리초 단위라 배포 리스크는 무의미하다.
ALTER TABLE answer
    ADD CONSTRAINT uk_answer_form_response_question
        UNIQUE (form_response_id, question_id);
