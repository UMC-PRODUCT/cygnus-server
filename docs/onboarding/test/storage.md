# Storage 테스트

현재 테스트는 `src/test/java/com/umc/product/storage`에 있다. Storage 사용 registry, cleanup claim, lifecycle contract와 S3 경계를 함께 검증한다. 전체 운영 절차는 [Replica 환경의 registry migration runbook](../database-backfill-with-replicas.md), 도메인 설명은 [Storage 도메인](../domain/storage.md)에서 확인한다.

## 실행 명령

```bash
./gradlew test --tests 'com.umc.product.storage.domain.FileUsageCoordinateTest' \
  --tests 'com.umc.product.storage.adapter.out.persistence.FileUsagePersistenceAdapterTest' \
  --tests 'com.umc.product.storage.adapter.out.persistence.FileCleanupClaimPersistenceAdapterTest' \
  --tests 'com.umc.product.storage.application.service.FileUsageCommandServiceTest' \
  --tests 'com.umc.product.storage.application.service.FileDeletionServiceTest' \
  --tests 'com.umc.product.storage.application.service.FileCleanupServiceTest' \
  --tests 'com.umc.product.storage.RegistryEndToEndIntegrationTest'
```

S3 adapter만 확인할 때는 다음을 실행한다.

```bash
./gradlew test --tests 'com.umc.product.storage.adapter.out.s3.S3StorageAdapterTest'
```

## Schema·coordinate·persistence

| 테스트 | 검증하는 observable |
| --- | --- |
| [`FileUsageCoordinateTest`](../../../src/test/java/com/umc/product/storage/domain/FileUsageCoordinateTest.java) | namespace 100자·소문자 dot segment, resource key, slot grammar와 필수값을 domain에서 거부한다. |
| [`FileUsagePersistenceAdapterTest`](../../../src/test/java/com/umc/product/storage/adapter/out/persistence/FileUsagePersistenceAdapterTest.java) | 두 owner가 한 file을 공유하는 exact snapshot/count, owner/file unique, owner lock와 metadata lock 순서, grammar CHECK, usage가 남은 metadata delete의 FK RESTRICT, owner cascade를 PostgreSQL에서 확인한다. |
| [`FileUsagePersistenceAdapterTest`](../../../src/test/java/com/umc/product/storage/adapter/out/persistence/FileUsagePersistenceAdapterTest.java) | expand schema가 legacy `is_uploaded=true, confirmed_at=NULL` row를 읽고 cleanup partial index와 nullable lifecycle column을 보존하는지 확인한다. |

## Runtime dual-write·transaction

| 테스트 | 검증하는 observable |
| --- | --- |
| [`FileUsageCommandServiceTest`](../../../src/test/java/com/umc/product/storage/application/service/FileUsageCommandServiceTest.java) | owner를 먼저 직렬화하고 기존·신규 file ID 합집합을 정렬 lock한다. replace/remove snapshot의 exact diff, 공유 file detach 시 마지막 usage 전까지 `unreferenced_at`을 건드리지 않는 동작, 빈 snapshot anchor를 확인한다. |
| [`FileUsageConcurrencyIntegrationTest`](../../../src/test/java/com/umc/product/storage/application/service/FileUsageConcurrencyIntegrationTest.java) | attach/detach와 cleanup claim race에서 usage와 active claim을 동시에 만들지 않고 최종 DB 상태가 불변식을 만족하는지 확인한다. |
| [`FormAnswerUsageTransactionIntegrationTest`](../../../src/test/java/com/umc/product/form/application/service/command/FormAnswerUsageTransactionIntegrationTest.java) | Form Answer 저장 후 usage 등록 실패 시 Answer 변경까지 rollback한다. |
| [`ChatMessageUsageTransactionIntegrationTest`](../../../src/test/java/com/umc/product/chat/application/service/command/ChatMessageUsageTransactionIntegrationTest.java) | Chat message 저장 후 usage 실패 시 message와 watermark/event 경계를 rollback한다. |
| [`RegistryEndToEndIntegrationTest`](../../../src/test/java/com/umc/product/storage/RegistryEndToEndIntegrationTest.java) | upload confirm → Form/Chat 공유 attach → 마지막 detach → S3 delete/CAS finalize의 end-to-end 상태와 잘못된 uploader 거부를 확인한다. |

## Cleanup claim·retention·rollback 안전성

| 테스트 | 검증하는 observable |
| --- | --- |
| [`FileCleanupClaimPersistenceAdapterTest`](../../../src/test/java/com/umc/product/storage/adapter/out/persistence/FileCleanupClaimPersistenceAdapterTest.java) | pending 24시간과 confirmed-unreferenced 7일, usage 0 후보만 claim하고 `FOR UPDATE SKIP LOCKED` worker가 겹치지 않는지 확인한다. claim timeout은 새 token으로 reclaim하고 stale token finalize는 no-op이다. |
| 같은 테스트 | matching token·usage 0일 때만 metadata를 삭제한다. attach와 cleanup claim race에서는 한쪽만 성공한다. retry due 전에는 재claim하지 않고 max attempt는 `cleanup_failed_at`으로 격리한다. |
| [`FileCleanupServiceTest`](../../../src/test/java/com/umc/product/storage/application/service/FileCleanupServiceTest.java) | claim transaction 종료 후 S3 delete와 CAS finalize를 분리하고, S3/DB transient 실패를 같은 token failure와 backoff로 기록한다. registry가 `DISABLED`이거나 property가 false가 되면 S3 호출 직전에 중단한다. |
| [`FileDeletionServiceTest`](../../../src/test/java/com/umc/product/storage/application/service/FileDeletionServiceTest.java) | READY gate, requester/uploader/SUPER_ADMIN 권한, usage 0, active claim 및 failed 상태 거부, stale token no-op을 확인한다. |
| [`OrphanFileCleanupSchedulerTest`](../../../src/test/java/com/umc/product/storage/adapter/in/scheduler/OrphanFileCleanupSchedulerTest.java) | cleanup disabled/비-READY이면 use case를 호출하지 않고, READY batch의 success/retry/failure metric 및 예외 재전파를 확인한다. |
| [`FileCleanupPropertiesTest`](../../../src/test/java/com/umc/product/storage/application/service/FileCleanupPropertiesTest.java) | `app.storage.cleanup.enabled=false` fail-safe 기본 binding과 capped exponential backoff를 확인한다. |

failed cleanup을 수동 reset하는 운영 절차는 [Storage 도메인 cleanup section](../domain/storage.md#lifecycle과-cleanup)의 guarded transaction을 그대로 따른다. 테스트나 문서에 실제 file ID·member ID·token·secret을 넣지 않는다. S3 `NoSuchKey`/404 delete는 [`S3StorageAdapterTest`](../../../src/test/java/com/umc/product/storage/adapter/out/s3/S3StorageAdapterTest.java)의 idempotent success 시나리오로 고정한다.

## Flyway migration·readiness

registry 전용 검증은 [`RegistryFlywayDataMigrationIntegrationTest`](../../../src/test/java/com/umc/product/registry/RegistryFlywayDataMigrationIntegrationTest.java)와 [`RegistryReadinessServiceTest`](../../../src/test/java/com/umc/product/registry/application/service/RegistryReadinessServiceTest.java)에 있다. 이 묶음은 다음을 확인한다.

- 독립 PostgreSQL database에서 실제 Flyway target을 순서대로 적용해 9개 canonical Storage source와 4개 Form ownership mapping을 이관한다.
- null/blank/duplicate/pending Storage reference와 Form duplicate가 migration transaction을 rollback하는지 확인한다.
- 미도입 Chat row와 `chat.message` usage를 제거하고, Flyway 직후 Storage cutover는 `PENDING`인지 확인한다.
- legacy-only late write는 certification을 실패시키며, exact reconciliation 이후에만 Storage readiness가 `READY`인지 확인한다.
- lifecycle constraint와 exact-one namespace coverage 누락/중복이 cleanup/enforcement를 fail-closed로 막는다.

배포 전 query, Flyway 실패/rollback, replica 검증 순서는 [운영 runbook](../database-backfill-with-replicas.md)을 사용한다.
