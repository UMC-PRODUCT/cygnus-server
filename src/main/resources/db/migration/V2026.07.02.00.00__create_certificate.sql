-- 목적: 인증서 발급 이력, 진위 검증 상태, S3 파일 참조, PDF SHA-256 무결성 정보를 저장한다.
CREATE TABLE certificate
(
    id                     BIGSERIAL PRIMARY KEY,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    serial_number          VARCHAR(40)                 NOT NULL,
    type                   VARCHAR(30)                 NOT NULL,
    status                 VARCHAR(20)                 NOT NULL,
    issuer                 VARCHAR(40)                 NOT NULL,
    recipient_member_id    BIGINT                      NOT NULL,
    recipient_name         VARCHAR(100)                NOT NULL,
    recipient_school_name  VARCHAR(100),
    gisu_id                BIGINT                      NOT NULL,
    gisu_generation        BIGINT                      NOT NULL,
    project_id             BIGINT,
    project_name           VARCHAR(100),
    merit_title            VARCHAR(100),
    merit_description      VARCHAR(500),
    issued_by_member_id    BIGINT                      NOT NULL,
    issued_at              TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    expires_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    revoked_at             TIMESTAMP(6) WITH TIME ZONE,
    revoked_by_member_id   BIGINT,
    revoke_reason          VARCHAR(500),
    file_id                VARCHAR(100)                NOT NULL,
    file_sha256            VARCHAR(64)                 NOT NULL,
    CONSTRAINT uk_certificate_serial_number UNIQUE (serial_number),
    CONSTRAINT certificate_type_check CHECK (type IN ('COMPLETION', 'MERIT', 'PROJECT_PARTICIPATION')),
    CONSTRAINT certificate_status_check CHECK (status IN ('ISSUED', 'REVOKED', 'EXPIRED')),
    CONSTRAINT certificate_issuer_check CHECK (issuer IN ('UNIVERSITY_MAKEUS_CHALLENGE', 'NEORDINARY'))
);

-- 목적: 사용자가 본인 인증서 목록을 최신 발급순으로 조회할 때 사용하는 정렬 인덱스다.
CREATE INDEX idx_certificate_recipient_issued_at
    ON certificate (recipient_member_id, issued_at DESC, id DESC);

-- 목적: 동일 범위의 유효 인증서 재사용/재발급 판정을 빠르게 수행한다.
-- issuer를 포함해 같은 수신자, 기수, 프로젝트, 상명이라도 UMC와 Ne(O)rdinary 발급 주체를 분리한다.
CREATE INDEX idx_certificate_scope_valid
    ON certificate (type, issuer, recipient_member_id, gisu_id, project_id, merit_title, status, expires_at);
