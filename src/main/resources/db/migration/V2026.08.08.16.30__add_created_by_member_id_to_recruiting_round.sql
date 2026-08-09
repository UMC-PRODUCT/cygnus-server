-- 모집 목록/공유 보관함에서 공고 작성자를 노출하기 위해 차수에 작성자를 기록한다.
ALTER TABLE public.recruiting_round
    ADD COLUMN created_by_member_id BIGINT;

COMMENT ON COLUMN public.recruiting_round.created_by_member_id
    IS '차수를 생성한 운영진 member ID. 컬럼 추가 이전 차수는 NULL.';
