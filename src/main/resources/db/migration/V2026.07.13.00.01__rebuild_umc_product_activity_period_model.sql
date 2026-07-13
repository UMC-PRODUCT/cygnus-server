CREATE EXTENSION IF NOT EXISTS btree_gist;

DROP TABLE IF EXISTS umc_product_squad_participant;
DROP TABLE IF EXISTS umc_product_part_membership;
DROP TABLE IF EXISTS umc_product_leadership;
DROP TABLE IF EXISTS umc_product_member_activity_period;
DROP TABLE IF EXISTS umc_product_part;
DROP TABLE IF EXISTS umc_product_chapter;
DROP TABLE IF EXISTS umc_product_squad;
DROP TABLE IF EXISTS umc_product_functional_membership;
DROP TABLE IF EXISTS umc_product_functional_unit;
DROP TABLE IF EXISTS umc_product_member;
DROP TABLE IF EXISTS umc_product_generation;

CREATE TABLE umc_product_member (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    introduction VARCHAR(2000),
    profile_image_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_umc_product_member_member_id UNIQUE (member_id)
);

CREATE TABLE umc_product_member_activity_period (
    id BIGSERIAL PRIMARY KEY,
    umc_product_member_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_umc_product_activity_period_member
        FOREIGN KEY (umc_product_member_id) REFERENCES umc_product_member(id) ON DELETE RESTRICT,
    CONSTRAINT ck_umc_product_activity_period_dates
        CHECK (end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ex_umc_product_activity_period_member_dates
        EXCLUDE USING gist (
            umc_product_member_id WITH =,
            (daterange(start_date, end_date + 2, '[)')) WITH &&
        )
);

CREATE INDEX ix_umc_product_member_activity_period_member
    ON umc_product_member_activity_period (umc_product_member_id);

CREATE INDEX ix_umc_product_member_activity_period_dates
    ON umc_product_member_activity_period (start_date, end_date);

CREATE TABLE umc_product_chapter (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    sort_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_umc_product_chapter_code UNIQUE (code)
);

CREATE INDEX ix_umc_product_chapter_active_sort
    ON umc_product_chapter (is_active, sort_order, id);

CREATE TABLE umc_product_part (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    sort_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_umc_product_part_chapter
        FOREIGN KEY (chapter_id) REFERENCES umc_product_chapter(id) ON DELETE RESTRICT,
    CONSTRAINT uk_umc_product_part_chapter_code UNIQUE (chapter_id, code)
);

CREATE INDEX ix_umc_product_part_chapter_active_sort
    ON umc_product_part (chapter_id, is_active, sort_order, id);

CREATE TABLE umc_product_part_membership (
    id BIGSERIAL PRIMARY KEY,
    member_activity_period_id BIGINT NOT NULL,
    part_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    position VARCHAR(32) NOT NULL,
    responsibility_title VARCHAR(200),
    responsibility_description VARCHAR(1000),
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_umc_product_part_membership_period
        FOREIGN KEY (member_activity_period_id)
        REFERENCES umc_product_member_activity_period(id) ON DELETE RESTRICT,
    CONSTRAINT fk_umc_product_part_membership_part
        FOREIGN KEY (part_id) REFERENCES umc_product_part(id) ON DELETE RESTRICT,
    CONSTRAINT ck_umc_product_part_membership_role
        CHECK (role IN ('MEMBER', 'PART_LEAD')),
    CONSTRAINT ck_umc_product_part_membership_position
        CHECK (position IN (
            'UNSPECIFIED',
            'PRODUCT_OWNER',
            'PRODUCT_DESIGNER',
            'IOS_DEVELOPER',
            'ANDROID_DEVELOPER',
            'WEB_DEVELOPER',
            'SERVER_DEVELOPER',
            'ETC'
        )),
    CONSTRAINT ck_umc_product_part_membership_dates
        CHECK (end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ex_umc_product_part_membership_activity
        EXCLUDE USING gist (
            member_activity_period_id WITH =,
            part_id WITH =,
            role WITH =,
            position WITH =,
            (COALESCE(responsibility_title, '')) WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        ),
    CONSTRAINT ex_umc_product_part_lead_dates
        EXCLUDE USING gist (
            part_id WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        ) WHERE (role = 'PART_LEAD')
);

CREATE INDEX ix_umc_product_part_membership_activity_period
    ON umc_product_part_membership (member_activity_period_id);

CREATE INDEX ix_umc_product_part_membership_part
    ON umc_product_part_membership (part_id);

CREATE INDEX ix_umc_product_part_membership_dates
    ON umc_product_part_membership (start_date, end_date);

CREATE TABLE umc_product_leadership (
    id BIGSERIAL PRIMARY KEY,
    member_activity_period_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_umc_product_leadership_period
        FOREIGN KEY (member_activity_period_id)
        REFERENCES umc_product_member_activity_period(id) ON DELETE RESTRICT,
    CONSTRAINT ck_umc_product_leadership_role
        CHECK (role IN ('UMC_PRODUCT_LEAD', 'UMC_PRODUCT_VICE_LEAD')),
    CONSTRAINT ck_umc_product_leadership_dates
        CHECK (end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ex_umc_product_leadership_member_dates
        EXCLUDE USING gist (
            member_activity_period_id WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        ),
    CONSTRAINT ex_umc_product_leadership_role_dates
        EXCLUDE USING gist (
            role WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        )
);

CREATE INDEX ix_umc_product_leadership_activity_period
    ON umc_product_leadership (member_activity_period_id);

CREATE INDEX ix_umc_product_leadership_role
    ON umc_product_leadership (role);

CREATE INDEX ix_umc_product_leadership_dates
    ON umc_product_leadership (start_date, end_date);

CREATE TABLE umc_product_squad (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    start_date DATE NOT NULL,
    end_date DATE,
    sort_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_umc_product_squad_code UNIQUE (code),
    CONSTRAINT ck_umc_product_squad_dates
        CHECK (end_date IS NULL OR start_date <= end_date)
);

CREATE INDEX ix_umc_product_squad_active_sort
    ON umc_product_squad (is_active, sort_order, id);

CREATE INDEX ix_umc_product_squad_dates
    ON umc_product_squad (start_date, end_date);

CREATE TABLE umc_product_squad_participant (
    id BIGSERIAL PRIMARY KEY,
    squad_id BIGINT NOT NULL,
    member_activity_period_id BIGINT NOT NULL,
    role VARCHAR(32) NOT NULL,
    position VARCHAR(32) NOT NULL,
    responsibility_title VARCHAR(200),
    responsibility_description VARCHAR(1000),
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_umc_product_squad_participant_squad
        FOREIGN KEY (squad_id) REFERENCES umc_product_squad(id) ON DELETE RESTRICT,
    CONSTRAINT fk_umc_product_squad_participant_period
        FOREIGN KEY (member_activity_period_id)
        REFERENCES umc_product_member_activity_period(id) ON DELETE RESTRICT,
    CONSTRAINT ck_umc_product_squad_participant_role
        CHECK (role IN ('MEMBER', 'SQUAD_LEAD')),
    CONSTRAINT ck_umc_product_squad_participant_position
        CHECK (position IN (
            'UNSPECIFIED',
            'PRODUCT_OWNER',
            'PRODUCT_DESIGNER',
            'IOS_DEVELOPER',
            'ANDROID_DEVELOPER',
            'WEB_DEVELOPER',
            'SERVER_DEVELOPER',
            'ETC'
        )),
    CONSTRAINT ck_umc_product_squad_participant_dates
        CHECK (end_date IS NULL OR start_date <= end_date),
    CONSTRAINT ex_umc_product_squad_participant_member_dates
        EXCLUDE USING gist (
            squad_id WITH =,
            member_activity_period_id WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        ),
    CONSTRAINT ex_umc_product_squad_lead_dates
        EXCLUDE USING gist (
            squad_id WITH =,
            (daterange(start_date, end_date + 1, '[)')) WITH &&
        ) WHERE (role = 'SQUAD_LEAD')
);

CREATE INDEX ix_umc_product_squad_participant_squad
    ON umc_product_squad_participant (squad_id);

CREATE INDEX ix_umc_product_squad_participant_activity_period
    ON umc_product_squad_participant (member_activity_period_id);

CREATE INDEX ix_umc_product_squad_participant_dates
    ON umc_product_squad_participant (start_date, end_date);
