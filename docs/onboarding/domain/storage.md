# Storage 도메인

## 역할과 경계

`storage`는 파일 metadata, 업로드 완료 확인, 접근 URL, 물리 삭제와 파일 사용 관계를 소유한다. 현재 Java 패키지는 `com.umc.product.storage`이며, 소비 도메인의 엔티티·repository를 런타임에 조회하지 않는다. 기존 소비 테이블을 읽는 JDBC 코드는 `registry-backfill` profile의 backfill/reconcile 경계에만 있다.

소비 도메인은 서버가 만든 `FileUsageCoordinate(namespace, resourceKey, slot)`으로 usage snapshot을 교체하거나 제거한다. 클라이언트가 namespace, resource key, slot을 입력하는 외부 계약은 없다. 파일 사용 registry와 Form/Chat ownership registry는 cardinality와 권한 의미가 달라 별도 registry로 유지한다. 설계 원칙은 [ADR-027](../../adr/027-engine-resource-ownership-namespaces.md)에 고정되어 있다.

## 저장 schema

expand migration은 [V2026.07.16.00.00](../../../src/main/resources/db/migration/V2026.07.16.00.00__create_file_usage_registry_and_cleanup_state.sql)이다.

| 테이블/열 | 의미와 불변식 |
| --- | --- |
| `file_usage_owner(id, usage_namespace, resource_key, slot, created_at, updated_at)` | opaque owner 좌표. `(usage_namespace, resource_key, slot)` unique이며 namespace/resource key/slot grammar CHECK를 가진다. |
| `file_usage(id, owner_id, file_id, created_at)` | owner와 file의 N:M snapshot. `(owner_id, file_id)` unique, owner FK는 `ON DELETE CASCADE`, `file_metadata` FK는 `ON DELETE RESTRICT`다. |
| `file_metadata.confirmed_at` | 서버가 S3 객체 검증을 끝낸 canonical 완료 시각. expand 단계에서는 nullable이다. |
| `file_metadata.unreferenced_at` | 전역 usage가 0이 된 시각. 마지막 detach에서만 설정하고 attach 시 null로 되돌린다. |
| `cleanup_claim_token`, `cleanup_claimed_at` | 물리 삭제 worker의 단일 CAS claim. token이 일치할 때만 finalize한다. |
| `cleanup_attempts`, `cleanup_next_attempt_at`, `cleanup_failed_at` | retry/backoff와 격리 상태. `cleanup_failed_at`이 있으면 scheduler 후보에서 제외한다. |
| `idx_file_metadata_cleanup_candidate` | `cleanup_failed_at IS NULL`인 pending/unreferenced 후보 탐색용 partial index다. |

backfill 제어 schema는 [V2026.07.16.00.30](../../../src/main/resources/db/migration/V2026.07.16.00.30__create_registry_backfill_control.sql)에서 추가한다.

| 테이블 | 열/용도 |
| --- | --- |
| `registry_backfill_checkpoint` | `(registry_name, source_name)`별 `last_parent_id`, `processed_rows`, `completed`, `updated_at`. keyset 재시작 지점이다. |
| `registry_cutover_state` | registry별 `DISABLED`, `BACKFILLING`, `VALIDATED`, `READY`, `BLOCKED`, 검증 시각과 상세 사유다. |

## Lifecycle과 cleanup

새 writer는 `is_uploaded`와 `confirmed_at`을 함께 변경한다. legacy row의 `is_uploaded=true, confirmed_at=NULL`은 audit 단계에서 `isConfirmedForAudit()` 호환으로 읽지만 `READY`에서는 attach를 허용하지 않는다. contract release는 [V2026.07.16.00.40](../../../src/main/resources/db/migration/V2026.07.16.00.40__enforce_file_upload_lifecycle.sql)에서 `is_uploaded = (confirmed_at IS NOT NULL)` CHECK를 `NOT VALID`로 추가한 뒤 validate한다. 이 artifact는 backfill/reconcile와 replica 확인이 끝난 뒤 적용한다.

기본 cleanup 정책은 [application.yml](../../../src/main/resources/application.yml)의 `app.storage.cleanup`에 있으며 모두 fail-safe로 시작한다.

| property | 기본값 | 동작 |
| --- | --- | --- |
| `app.storage.cleanup.enabled` (`FILE_CLEANUP_ENABLED`) | `false` | `false`이거나 Storage registry가 `READY`가 아니면 scheduler와 worker가 S3를 호출하지 않는다. |
| `poll-interval` | `PT1M` | `OrphanFileCleanupScheduler` 실행 간격 |
| `pending-retention` | `PT24H` | `confirmed_at IS NULL AND is_uploaded=false` pending 유예 |
| `unreferenced-retention` | `PT168H` | confirmed 상태에서 usage 0이 된 뒤 7일 유예 |
| `claim-timeout` | `PT15M` | 만료 claim을 새 token으로 reclaim |
| `batch-size` | `100` | 한 번에 `FOR UPDATE SKIP LOCKED`로 claim할 최대 수 |
| `max-attempts` | `10` | 초과 시 `cleanup_failed_at`으로 격리 |
| `initial-backoff` / `max-backoff` | `PT1M` / `PT6H` | 지수 backoff, 최대 6시간 |

claim은 `FileCleanupClaimPersistenceAdapter`의 짧은 `REQUIRES_NEW` transaction에서 usage 0, retention, claim 만료를 다시 확인한다. transient 실패는 token과 `cleanup_claimed_at`을 유지한 채 `cleanup_next_attempt_at`만 설정한다. due 시점이 되면 DB claim query가 해당 row를 lock하고 새 token으로 CAS reclaim한다. backoff가 없는 in-flight claim만 `claim-timeout`으로 reclaim하며, 최종 `FAILED`는 token을 유지하고 scheduler 후보에서 제외한다. 따라서 attach는 in-flight, retry/backoff, `FAILED` 전부를 fail-closed로 거부한다.

S3 delete 직전에 `FileCleanupClaimService.validateDeletionFence` 또한 독립 `REQUIRES_NEW` transaction으로 metadata row를 lock한다. 여기서 token이 현재 active claim과 일치하고, retry/`FAILED` 상태가 아니며, usage count가 0인지 확인한 뒤 commit한다. `FileCleanupService`는 `NOT_SUPPORTED` 경계에서 반환값이 true일 때만 즉시 S3를 호출한다. T2가 timeout claim을 새 token으로 reclaim한 뒤 재개된 stale T1은 이 검증에서 false를 받아 S3와 DB finalize를 모두 호출하지 않는다. 검증 commit과 S3 호출 사이에도 token fence가 유지되므로 attach는 열리지 않는다. S3 이후 finalize는 matching active token과 usage 0을 다시 확인하며, DB finalize가 실패해도 failure 기록이 동일 token을 유지한다. S3 `NoSuchKey` 또는 HTTP 404는 `S3StorageAdapter.delete`에서 `not_found` idempotent success로 처리해 retry하지 않으며, scheduler 경로에서도 success로 집계되어 retry를 예약하지 않는다.

`cleanup_failed_at`이 있는 파일을 다시 허용할 때는 운영자만 guarded transaction을 수행한다. 실제 file ID나 secret을 문서·명령 history에 적지 않는다.

```sql
BEGIN;

-- :file_id는 승인된 운영 도구의 bind parameter다. 결과가 1행인지 확인한다.
SELECT id, cleanup_failed_at, cleanup_claim_token, cleanup_attempts
FROM file_metadata
WHERE id = :file_id
FOR UPDATE;

-- usage 0인지 먼저 확인한다.
SELECT COUNT(*) AS usage_count
FROM file_usage
WHERE file_id = :file_id;

-- :failed_token은 SELECT로 잠긴 row에서 읽은 현재 token이고,
-- :reset_token은 승인 도구가 새로 발급한 서로 다른 UUID다.
UPDATE file_metadata
SET cleanup_failed_at = NULL,
    cleanup_attempts = 0,
    cleanup_next_attempt_at = CURRENT_TIMESTAMP,
    cleanup_claim_token = CAST(:reset_token AS uuid),
    cleanup_claimed_at = CURRENT_TIMESTAMP
WHERE id = :file_id
  AND cleanup_failed_at IS NOT NULL
  AND cleanup_claim_token = CAST(:failed_token AS uuid)
  AND CAST(:reset_token AS uuid) <> CAST(:failed_token AS uuid)
  AND NOT EXISTS (
      SELECT 1 FROM file_usage usage WHERE usage.file_id = file_metadata.id
  );

-- UPDATE count가 1이 아니면 ROLLBACK한다.
COMMIT;
```

usage가 0이 아니거나 SELECT 후 token이 변했으면 reset하지 않는다. reset은 token을 `NULL`로 열지 않고 새 UUID로 회전하므로 old worker의 CAS를 실패시키고 attach fence를 유지한다. `cleanup_next_attempt_at=CURRENT_TIMESTAMP`이므로 `READY` gate를 통과한 scheduler가 즉시 또 하나의 새 token으로 due reclaim한다. update count가 1이 아니면 반드시 rollback한다.

## 현재 canonical mapping

backfill source의 원본은 [StorageFileUsageSource.java](../../../src/main/java/com/umc/product/storage/adapter/out/backfill/StorageFileUsageSource.java)에 있으며, 아래 아홉 개를 exact snapshot으로 취급한다.

| source | legacy column/shape | usage namespace | resource key | slot |
| --- | --- | --- | --- | --- |
| `member-profile-image` | `member.profile_image_id` (scalar) | `member` | `member.id` | `profile-image` |
| `school-logo` | `school.logo_image_id` (scalar) | `organization.school` | `school.id` | `logo` |
| `notice-images` | `notice_image.notice_id → image_id` (scalar row) | `notice` | `notice.id` | `images` |
| `project-logo` | `project.logo_file_id` (scalar) | `project` | `project.id` | `logo` |
| `project-thumbnail` | `project.thumbnail_file_id` (scalar) | `project` | `project.id` | `thumbnail` |
| `form-answer-attachments` | `answer.file_ids` (array) | `form.answer` | `answer.id` | `attachments` |
| `chat-message-attachments` | `chat_message.file_metadata_ids` (array) | `chat.message` | `chat_message.id` | `attachments` |
| `umc-product-member-profile-image` | `umc_product_member.profile_image_id` (scalar) | `organization.umc-product-member` | `umc_product_member.id` | `profile-image` |
| `certificate-file` | `certificate.file_id` (scalar) | `certificate` | `certificate.id` | `file` |

array source는 `unnest` 후 null/blank file ID를 제외한다. 같은 owner가 여러 file을 가질 수 있고, 한 file이 여러 owner에 공유될 수 있다. source에만 있고 registry에 없는 것은 missing drift, registry에만 있는 것은 stale drift로 기록한다. metadata가 없으면 broken, 기대 snapshot과 실제 snapshot이 다르면 conflict로 기록하며 자동으로 임의 선택하지 않는다.

## 관측과 운영 진입점

- batch scheduler는 `jobName=orphan_file_cleanup`으로 `operational.batch.job.seconds`, `operational.batch.job.total`, `operational.batch.job.processed.total`을 기록하고 `result=success|retry|failure`를 사용한다.
- S3 adapter는 `operational.external.call.seconds`와 `.total`에 `provider=STORAGE`, `operation=DELETE_OBJECT`, `result=success|not_found|failure`를 기록한다. file ID, member ID, token 같은 고카디널리티 값은 tag로 넣지 않는다.
- registry backfill은 `RegistryReconciliationResult.summary()` 로그(`registry=...,totalDrift=...,sources=[...]`)와 `registry_cutover_state.details`를 운영 증거로 사용한다.
- 실행 절차, replica 검증, rollback은 [Replica backfill 및 registry cutover runbook](../database-backfill-with-replicas.md)을 따른다. 수동 물리 삭제와 cleanup enable은 세 registry가 `READY`가 될 때까지 금지한다.

## 관련 코드와 테스트

- command/usage: [`FileUsageCommandService`](../../../src/main/java/com/umc/product/storage/application/service/FileUsageCommandService.java), [`FileDeletionService`](../../../src/main/java/com/umc/product/storage/application/service/FileDeletionService.java)
- cleanup: [`FileCleanupService`](../../../src/main/java/com/umc/product/storage/application/service/FileCleanupService.java), [`OrphanFileCleanupScheduler`](../../../src/main/java/com/umc/product/storage/adapter/in/scheduler/OrphanFileCleanupScheduler.java)
- 테스트 목록과 실행 명령: [`Storage 테스트`](../test/storage.md)

새 REST/GraphQL/FE 계약이나 client 입력을 추가하지 않는다. ownership namespace와 file usage 좌표는 trusted factory/adapter가 서버에서 결정한다.
