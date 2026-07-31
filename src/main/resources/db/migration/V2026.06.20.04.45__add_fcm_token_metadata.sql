-- 플랫폼별 알림 처리와 token lifecycle 관리를 위해 기기·등록·검증 metadata를 함께 저장한다.
ALTER TABLE fcm_token
    -- iOS/Android별 payload 정책과 운영 현황을 구분하기 위한 선택 정보다.
    ADD COLUMN platform VARCHAR(30),
    -- 앱 설치를 식별하고 해당 installation의 회원·token을 원자적으로 교체하는 식별자다.
    ADD COLUMN installation_id VARCHAR(100),
    -- 앱 버전별 payload 호환성과 점진 배포 상태를 확인하기 위한 선택 정보다.
    ADD COLUMN app_version VARCHAR(50),
    -- 마지막 등록·재등록 시점으로, 장기간 갱신되지 않은 token 정리 기준에 사용한다.
    ADD COLUMN last_registered_at TIMESTAMP(6) WITH TIME ZONE,
    -- 명시적 해제 또는 UNREGISTERED 판정 시점을 보존해 비활성화 이력을 추적한다.
    ADD COLUMN deactivated_at TIMESTAMP(6) WITH TIME ZONE,
    -- 마지막 Firebase dry-run 검증 시점으로, 주기 검증 대상 선정에 사용한다.
    ADD COLUMN last_validated_at TIMESTAMP(6) WITH TIME ZONE;

-- 기존 token도 등록 시점이 있도록 마지막 수정 시각, 생성 시각 순으로 초기화한다.
UPDATE fcm_token
SET last_registered_at = COALESCE(updated_at, created_at)
WHERE last_registered_at IS NULL;

-- 기존 token-only 등록은 installation 소유권을 검증할 수 없어 모두 비활성화한다.
-- 신규 앱이 installationId와 함께 재등록한 token만 다시 활성화된다.
UPDATE fcm_token
SET is_active = FALSE,
    deactivated_at = COALESCE(deactivated_at, CURRENT_TIMESTAMP)
WHERE is_active = TRUE;

-- 하나의 installation은 하나의 row와 현재 token만 보유한다.
CREATE UNIQUE INDEX uix_fcm_token_installation_id
    ON fcm_token (installation_id)
    WHERE installation_id IS NOT NULL;

-- 과거 row는 installation_id가 NULL일 수 있지만 다시 활성화할 수는 없다.
ALTER TABLE fcm_token
    ADD CONSTRAINT chk_fcm_token_active_installation
        CHECK (is_active = FALSE OR installation_id IS NOT NULL);

-- 같은 token의 활성 소유자를 빠르게 조회해 다른 회원에게 재등록될 때 이전 token을 비활성화한다.
CREATE INDEX ix_fcm_token_active_token
    ON fcm_token (fcm_token, is_active);

-- 검증되지 않았거나 오래전에 검증된 활성 token을 scheduler가 순서대로 batch 조회할 때 사용한다.
-- 기존 row의 last_validated_at은 의도적으로 NULL을 유지해 배포 후 첫 주기 검증 대상에 포함한다.
CREATE INDEX idx_fcm_token_validation_targets
    ON fcm_token (is_active, last_validated_at, id);
