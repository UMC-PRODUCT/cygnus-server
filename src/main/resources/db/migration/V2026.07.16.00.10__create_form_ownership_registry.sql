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
