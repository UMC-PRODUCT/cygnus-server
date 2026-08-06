ALTER TABLE chat_message
    ADD COLUMN reply_to_message_id BIGINT;

ALTER TABLE chat_message
    ADD CONSTRAINT fk_chat_message_reply_to
        FOREIGN KEY (reply_to_message_id) REFERENCES chat_message (id) ON DELETE SET NULL;

CREATE INDEX idx_chat_message_reply_to_message_id
    ON chat_message (reply_to_message_id);
