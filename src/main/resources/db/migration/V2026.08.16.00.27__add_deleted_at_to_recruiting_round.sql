-- 모집 공고 삭제를 되돌릴 수 있도록 차수를 물리 삭제 대신 soft delete로 전환한다.
ALTER TABLE public.recruiting_round
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

COMMENT ON COLUMN public.recruiting_round.deleted_at
    IS '차수를 삭제한 시각. NULL이면 활성 차수이며, 값이 있으면 복구 가능한 삭제 상태.';

-- 삭제된 차수가 (시즌, 유형, 차수 번호) 슬롯을 계속 점유하면 같은 번호로 다시 만들 수 없다.
-- 활성 차수끼리만 유일성을 강제하도록 부분 유니크 인덱스로 교체한다.
ALTER TABLE public.recruiting_round
    DROP CONSTRAINT uk_recruiting_round_season_type_no;

CREATE UNIQUE INDEX uk_recruiting_round_season_type_no
    ON public.recruiting_round (recruiting_season_id, type, round_no)
    WHERE deleted_at IS NULL;
