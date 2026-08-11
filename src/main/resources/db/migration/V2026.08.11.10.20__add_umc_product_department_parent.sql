ALTER TABLE umc_product_department ADD COLUMN parent_department_id BIGINT;

ALTER TABLE umc_product_department
    ADD CONSTRAINT fk_umc_product_department_parent
    FOREIGN KEY (parent_department_id) REFERENCES umc_product_department(id) ON DELETE RESTRICT;

CREATE INDEX ix_umc_product_department_parent
    ON umc_product_department (parent_department_id);
