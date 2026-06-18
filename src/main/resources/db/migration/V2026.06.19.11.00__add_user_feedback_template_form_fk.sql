CREATE INDEX IF NOT EXISTS idx_user_feedback_template_form_id
    ON user_feedback_template (form_id);

ALTER TABLE user_feedback_template
    ADD CONSTRAINT fk_user_feedback_template_form
        FOREIGN KEY (form_id) REFERENCES form (id);
