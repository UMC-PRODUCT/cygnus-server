ALTER TABLE chat_room
    ADD COLUMN read_scope VARCHAR(20) DEFAULT 'MEMBER_ONLY' NOT NULL;

-- Community thread 방은 스레드 상세와 동일하게 메시지 조회를 공개한다.
-- 기존 스레드도 같은 규칙을 따라야 하므로 소유 관계를 따라 backfill 한다.
UPDATE chat_room
SET read_scope = 'PUBLIC'
WHERE id IN (SELECT chat_room_id FROM community_thread);

-- 이후 생성되는 방은 엔티티가 항상 값을 채우므로 기본값에 기대지 않는다.
ALTER TABLE chat_room
    ALTER COLUMN read_scope DROP DEFAULT;

ALTER TABLE chat_room
    ADD CONSTRAINT ck_chat_room_read_scope
        CHECK (read_scope IN ('MEMBER_ONLY', 'PUBLIC'));
