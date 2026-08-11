ALTER TABLE umc_product_squad RENAME TO umc_product_department;
ALTER TABLE umc_product_squad_participant RENAME TO umc_product_department_participant;
ALTER TABLE umc_product_department_participant RENAME COLUMN squad_id TO department_id;

-- 기존 CHECK와 부분 exclusion 제약은 새 role 값을 막으므로 값 변경 전에 제거한다.
ALTER TABLE umc_product_department_participant DROP CONSTRAINT ck_umc_product_squad_participant_role;
ALTER TABLE umc_product_department_participant DROP CONSTRAINT ex_umc_product_squad_lead_dates;

UPDATE umc_product_department_participant
SET role = 'DEPARTMENT_LEAD'
WHERE role = 'SQUAD_LEAD';

ALTER TABLE umc_product_department_participant
    ADD CONSTRAINT ck_umc_product_department_participant_role
    CHECK (role IN ('MEMBER', 'DEPARTMENT_LEAD'));

ALTER TABLE umc_product_department_participant
    ADD CONSTRAINT ex_umc_product_department_lead_dates
    EXCLUDE USING gist (
        department_id WITH =,
        (daterange(start_date, end_date + 1, '[)')) WITH &&
    ) WHERE (role = 'DEPARTMENT_LEAD');

ALTER TABLE umc_product_department
    RENAME CONSTRAINT uk_umc_product_squad_code TO uk_umc_product_department_code;
ALTER TABLE umc_product_department
    RENAME CONSTRAINT ck_umc_product_squad_dates TO ck_umc_product_department_dates;
ALTER INDEX ix_umc_product_squad_active_sort RENAME TO ix_umc_product_department_active_sort;
ALTER INDEX ix_umc_product_squad_dates RENAME TO ix_umc_product_department_dates;

ALTER TABLE umc_product_department_participant
    RENAME CONSTRAINT fk_umc_product_squad_participant_squad
    TO fk_umc_product_department_participant_department;
ALTER TABLE umc_product_department_participant
    RENAME CONSTRAINT fk_umc_product_squad_participant_period
    TO fk_umc_product_department_participant_period;
ALTER TABLE umc_product_department_participant
    RENAME CONSTRAINT ck_umc_product_squad_participant_position
    TO ck_umc_product_department_participant_position;
ALTER TABLE umc_product_department_participant
    RENAME CONSTRAINT ck_umc_product_squad_participant_dates
    TO ck_umc_product_department_participant_dates;
ALTER TABLE umc_product_department_participant
    RENAME CONSTRAINT ex_umc_product_squad_participant_member_dates
    TO ex_umc_product_department_participant_member_dates;
ALTER INDEX ix_umc_product_squad_participant_squad
    RENAME TO ix_umc_product_department_participant_department;
ALTER INDEX ix_umc_product_squad_participant_activity_period
    RENAME TO ix_umc_product_department_participant_activity_period;
ALTER INDEX ix_umc_product_squad_participant_dates
    RENAME TO ix_umc_product_department_participant_dates;
