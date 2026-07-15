ALTER TABLE chat_room
    ADD COLUMN pinned_message_id BIGINT;

ALTER TABLE chat_room
    ADD CONSTRAINT fk_chat_room_pinned_message
        FOREIGN KEY (pinned_message_id) REFERENCES chat_message (id) ON DELETE SET NULL;

CREATE INDEX idx_chat_room_pinned_message_id
    ON chat_room (pinned_message_id);
