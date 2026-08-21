ALTER TABLE demoday_booth
    ADD COLUMN stamp_credential_hash VARCHAR(64),
    ADD COLUMN stamp_credential_cipher VARCHAR(255),
    ADD COLUMN stamp_credential_generated_at TIMESTAMP WITH TIME ZONE,
    -- 해시 충돌 확률보다는 애플리케이션 버그로 동일한 credential이
    -- 여러 부스에 할당되는 것을 방지하여 credential과 부스의 1:1 관계를 보장한다.
    ADD CONSTRAINT uk_demoday_booth_stamp_credential_hash
    UNIQUE (stamp_credential_hash);
