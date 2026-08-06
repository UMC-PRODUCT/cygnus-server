-- 학교 관리 화면에서 어드민이 입력하는 학교 약칭을 저장하기 위한 컬럼을 추가한다.
ALTER TABLE school
    -- 목록·상세에서 짧게 노출 이름
    ADD COLUMN short_name VARCHAR(50);
