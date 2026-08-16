ALTER TABLE mission_submission
    ADD COLUMN withdrawn_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE study_group_schedule
    ADD COLUMN IF NOT EXISTS weekly_curriculum_id BIGINT;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM weekly_best_workbook WHERE study_group_id IS NULL) THEN
        RAISE EXCEPTION 'weekly_best_workbook.study_group_id contains NULL; clean the data before migration';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM weekly_best_workbook
        GROUP BY study_group_id, weekly_curriculum_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'duplicate weekly best workbook exists for the same study group and week';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM study_group_schedule
        WHERE weekly_curriculum_id IS NOT NULL
        GROUP BY study_group_id, weekly_curriculum_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'duplicate study group schedule exists for the same study group and week';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM study_group_schedule s
        LEFT JOIN weekly_curriculum w ON w.id = s.weekly_curriculum_id
        WHERE s.weekly_curriculum_id IS NOT NULL
          AND w.id IS NULL
    ) THEN
        RAISE EXCEPTION 'study_group_schedule.weekly_curriculum_id contains dangling references; clean the data before migration';
    END IF;
END
$$;

ALTER TABLE weekly_best_workbook
    ALTER COLUMN study_group_id SET NOT NULL;

ALTER TABLE weekly_best_workbook
    DROP CONSTRAINT IF EXISTS uk_weekly_best_workbook_member_week_study_group;

ALTER TABLE weekly_best_workbook
    ADD CONSTRAINT uk_weekly_best_workbook_study_group_week
        UNIQUE (study_group_id, weekly_curriculum_id);

ALTER TABLE study_group_schedule
    ADD CONSTRAINT uk_study_group_schedule_group_week
        UNIQUE (study_group_id, weekly_curriculum_id);

ALTER TABLE study_group_schedule
    ADD CONSTRAINT fk_study_group_schedule_weekly_curriculum
        FOREIGN KEY (weekly_curriculum_id) REFERENCES weekly_curriculum (id);

CREATE INDEX idx_mission_submission_active_workbook
    ON mission_submission (challenger_workbook_id)
    WHERE withdrawn_at IS NULL;

CREATE INDEX idx_mission_feedback_submission
    ON mission_feedback (mission_submission_id);

CREATE INDEX idx_weekly_best_workbook_week_id
    ON weekly_best_workbook (weekly_curriculum_id, id DESC);

CREATE INDEX idx_original_workbook_weekly_curriculum
    ON original_workbook (weekly_curriculum_id);
