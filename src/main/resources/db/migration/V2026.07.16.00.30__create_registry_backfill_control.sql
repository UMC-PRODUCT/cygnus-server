CREATE TABLE public.registry_backfill_checkpoint
(
    registry_name  VARCHAR(100)                NOT NULL,
    source_name    VARCHAR(100)                NOT NULL,
    last_parent_id BIGINT                      NOT NULL DEFAULT 0,
    processed_rows BIGINT                      NOT NULL DEFAULT 0,
    completed      BOOLEAN                     NOT NULL DEFAULT FALSE,
    updated_at     TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_registry_backfill_checkpoint
        PRIMARY KEY (registry_name, source_name),
    CONSTRAINT ck_registry_backfill_checkpoint_last_parent
        CHECK (last_parent_id >= 0),
    CONSTRAINT ck_registry_backfill_checkpoint_processed_rows
        CHECK (processed_rows >= 0)
);

CREATE TABLE public.registry_cutover_state
(
    registry_name VARCHAR(100)                NOT NULL,
    status        VARCHAR(20)                 NOT NULL,
    verified_at   TIMESTAMP(6) WITH TIME ZONE,
    details       TEXT                        NOT NULL DEFAULT '',
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_registry_cutover_state PRIMARY KEY (registry_name),
    CONSTRAINT ck_registry_cutover_state_status
        CHECK (status IN ('DISABLED', 'BACKFILLING', 'VALIDATED', 'READY', 'BLOCKED'))
);
