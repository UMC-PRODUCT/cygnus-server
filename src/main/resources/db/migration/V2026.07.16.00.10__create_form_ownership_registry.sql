CREATE TABLE form_ownership
(
    form_id            BIGINT                   NOT NULL,
    namespace          VARCHAR(100)             NOT NULL,
    owner_resource_key VARCHAR(128)             NOT NULL,
    slot               VARCHAR(50)              NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_form_ownership PRIMARY KEY (form_id),
    CONSTRAINT uq_form_ownership_owner_tuple UNIQUE (namespace, owner_resource_key, slot),
    CONSTRAINT fk_form_ownership_form
        FOREIGN KEY (form_id) REFERENCES form (id) ON DELETE CASCADE,
    CONSTRAINT ck_form_ownership_namespace
        CHECK (
            char_length(namespace) BETWEEN 1 AND 100
            AND namespace ~ '^[a-z][a-z0-9-]{0,31}(\.[a-z][a-z0-9-]{0,31})*$'
        ),
    CONSTRAINT ck_form_ownership_owner_resource_key
        CHECK (
            char_length(owner_resource_key) BETWEEN 1 AND 128
            AND owner_resource_key ~ '^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$'
        ),
    CONSTRAINT ck_form_ownership_slot
        CHECK (
            char_length(slot) BETWEEN 1 AND 50
            AND slot ~ '^[a-z][a-z0-9-]{0,49}$'
        )
);

CREATE INDEX idx_form_ownership_namespace
    ON form_ownership (namespace);

INSERT INTO form_ownership
    (form_id, namespace, owner_resource_key, slot, created_at, updated_at)
SELECT form_id, 'project.application-form', project_id::text, 'default',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM project_application_form

UNION ALL

SELECT vote_id, 'notice.vote', notice_id::text, 'default',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM notice_vote

UNION ALL

SELECT form_id, 'feedback.template', id::text, 'default',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM user_feedback_template;

INSERT INTO form_ownership
    (form_id, namespace, owner_resource_key, slot, created_at, updated_at)
SELECT current_form.id, 'form.standalone', current_form.id::text, 'default',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM form current_form
WHERE NOT EXISTS (
    SELECT 1
    FROM form_ownership ownership
    WHERE ownership.form_id = current_form.id
);
