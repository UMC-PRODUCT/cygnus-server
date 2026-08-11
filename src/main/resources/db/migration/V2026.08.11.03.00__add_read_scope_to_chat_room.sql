-- DEFAULT는 제거하지 않는다.
-- ASG instance refresh는 구/신 인스턴스가 함께 도는 구간이 있고, 그동안 구버전은 read_scope를
-- 포함하지 않은 INSERT를 계속 보낸다. DEFAULT가 없으면 그 INSERT가 NOT NULL 위반으로 전부 실패한다.
-- 또한 MEMBER_ONLY는 가장 닫힌 값이라, 값을 채우지 않는 코드가 생겨도 안전한 쪽으로 떨어진다.
ALTER TABLE chat_room
    ADD COLUMN read_scope VARCHAR(20) DEFAULT 'MEMBER_ONLY' NOT NULL;

-- Community thread 방은 스레드 상세와 동일하게 메시지 조회를 공개한다.
-- 기존 스레드도 같은 규칙을 따라야 하므로 소유 관계를 따라 backfill 한다.
-- 롤링 배포 오버랩 구간에 구버전이 만든 방은 MEMBER_ONLY로 남을 수 있다.
-- 같은 조건으로 이 UPDATE를 다시 실행하면 치유되며, 멱등이라 몇 번 돌려도 안전하다.
UPDATE chat_room
SET read_scope = 'PUBLIC'
WHERE id IN (SELECT chat_room_id FROM community_thread);

ALTER TABLE chat_room
    ADD CONSTRAINT ck_chat_room_read_scope
        CHECK (read_scope IN ('MEMBER_ONLY', 'PUBLIC'));
