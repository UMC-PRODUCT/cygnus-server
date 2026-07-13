-- 기명/익명 응답의 식별자 XOR invariant 를 DB 레벨에서 강제한다.
-- 기명 응답: respondent_member_id 세팅, response_access_key_hash = NULL
-- 익명 응답: respondent_member_id = NULL, response_access_key_hash 세팅
-- 정확히 둘 중 하나만 NULL 이어야 한다.

ALTER TABLE form_response
    ADD CONSTRAINT ck_form_response_identifier_xor
        CHECK ((respondent_member_id IS NULL) <> (response_access_key_hash IS NULL));
