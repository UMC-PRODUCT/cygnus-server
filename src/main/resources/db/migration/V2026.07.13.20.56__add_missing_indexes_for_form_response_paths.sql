-- PostgreSQL 은 FK 컬럼에 자동 인덱스를 생성하지 않는다.
-- 아래 컬럼들은 실제 쿼리 경로 (조건부 섹션 이동, 답변 선택지 조회, 섹션 삭제 cascade) 에서 활용되므로 인덱스가 없으면 전체 스캔이 발생한다.

CREATE INDEX IF NOT EXISTS idx_question_option_question_id
    ON question_option (question_id);

CREATE INDEX IF NOT EXISTS idx_question_option_next_section_id
    ON question_option (next_section_id);

CREATE INDEX IF NOT EXISTS idx_answer_choice_answer_id
    ON answer_choice (answer_id);
