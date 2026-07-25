-- 부하 테스트에서 특정된 홈 화면 hot path 의 FK 인덱스 부재 보완
-- (docs/loadtest/runs/2026-07-25-home-index-improvement 참조)
--
-- PostgreSQL 은 FK 에 인덱스를 자동 생성하지 않는다 (JPA @ManyToOne 도 마찬가지).
-- 아래 세 컬럼은 20만 행 규모에서 요청마다 풀스캔을 유발해 홈 시나리오 한계를
-- 12행동/s 로 묶었던 원인이다 — 인덱스 추가 후 동일 조건 재측정에서 약 50행동/s (4배).
--   · challenger_point.challenger_id   : 상벌점 조회 (개선 전 평균 20ms, DB 시간 31.5%)
--   · schedule_participant.member_id   : 내 일정 조회 (개선 전 42ms, 22.0%)
--   · schedule_participant.schedule_id : 일정 참여자 목록 (개선 전 87ms, 45.2%)
-- IF NOT EXISTS: 부하 테스트 리그에서 수동 생성한 DB 와의 충돌 방지.

CREATE INDEX IF NOT EXISTS idx_challenger_point_challenger_id
    ON challenger_point (challenger_id);

CREATE INDEX IF NOT EXISTS idx_schedule_participant_member_id
    ON schedule_participant (member_id);

CREATE INDEX IF NOT EXISTS idx_schedule_participant_schedule_id
    ON schedule_participant (schedule_id);
