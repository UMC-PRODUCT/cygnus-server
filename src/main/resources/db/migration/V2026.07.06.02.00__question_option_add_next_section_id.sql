ALTER TABLE question_option
    ADD COLUMN next_section_id BIGINT,
    ADD CONSTRAINT fk_question_option_next_section
        FOREIGN KEY (next_section_id)
        REFERENCES form_section(id)
        ON DELETE SET NULL;
