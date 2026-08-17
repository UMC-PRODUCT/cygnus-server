ALTER TABLE demoday_poll
    DROP CONSTRAINT demoday_poll_status_check;

ALTER TABLE demoday_poll
    ADD CONSTRAINT demoday_poll_status_check
    CHECK (status IN ('READY', 'OPEN', 'CLOSED'));
