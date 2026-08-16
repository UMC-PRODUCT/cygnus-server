DROP INDEX IF EXISTS idx_chat_message_room_id;

CREATE INDEX idx_chat_message_room_id_id_desc
    ON chat_message (room_id, id DESC);
