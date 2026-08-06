-- 목적: 인증서 발급 정책을 템플릿 단일 기준으로 재구성한다. 기존 인증서 데이터는 보존하지 않는다.
DROP TABLE IF EXISTS certificate;

-- 목적: 인증서 발급 이력, 진위 검증 상태, S3 파일 참조, PDF SHA-256 무결성 정보를 저장한다.
CREATE TABLE certificate
(
    id                     BIGSERIAL PRIMARY KEY,
    created_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at             TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    serial_number          VARCHAR(40)                 NOT NULL,
    template               VARCHAR(80)                 NOT NULL,
    status                 VARCHAR(20)                 NOT NULL,
    recipient_member_id    BIGINT                      NOT NULL,
    recipient_name         VARCHAR(100)                NOT NULL,
    recipient_school_name  VARCHAR(100),
    gisu_id                BIGINT                      NOT NULL,
    gisu_generation        BIGINT                      NOT NULL,
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
    CONSTRAINT certificate_template_check CHECK (template IN (
        'UMC_COURSE_COMPLETION',
        'UMC_COURSE_MERIT',
        'UMC_DEMO_DAY_GRAND_PRIZE',
        'UMC_DEMO_DAY_FIRST_PRIZE',
        'UMC_DEMO_DAY_SECOND_PRIZE',
        'UMC_DEMO_DAY_PARTICIPATION_PRIZE',
        'UMC_DEMO_DAY_AWS_SPECIAL_PRIZE',
        'UMC_DEMO_DAY_BEST_CHALLENGER',
        'UMC_HACKATHON_CERTIFICATION_OF_COMPLETION',
        'UMC_HACKATHON_GRAND_PRIZE',
        'UMC_HACKATHON_FIRST_PRIZE',
        'UMC_HACKATHON_SECOND_PRIZE',
        'NEORDINARY_HACKATHON_GRAND_PRIZE',
        'NEORDINARY_HACKATHON_FIRST_PRIZE',
        'NEORDINARY_HACKATHON_SECOND_PRIZE',
        'NEORDINARY_HACKATHON_CERTIFICATION_OF_COMPLETION'
    )),
    CONSTRAINT certificate_status_check CHECK (status IN ('ISSUED', 'REVOKED', 'EXPIRED'))
);

-- 목적: 사용자가 본인 인증서 목록을 최신 발급순으로 조회할 때 사용하는 정렬 인덱스다.
CREATE INDEX idx_certificate_recipient_issued_at
    ON certificate (recipient_member_id, issued_at DESC, id DESC);

-- 목적: 동일 범위의 유효 인증서 재사용/재발급 판정을 빠르게 수행한다.
-- template을 포함해 같은 수신자와 기수라도 과정, 행사, 수상 종류가 다르면 별도 인증서로 판정한다.
CREATE INDEX idx_certificate_scope_valid
    ON certificate (template, recipient_member_id, gisu_id, merit_title, status, expires_at);
