# Replica 환경의 Database Backfill 운영 가이드

> 이 문서의 첫 번째 섹션은 UMC PRODUCT의 Storage usage/Form ownership/Chat ownership registry cutover 전용 runbook이다. 아래의 일반 PostgreSQL backfill 원칙은 이 절차를 보완한다. 운영 DB의 실제 secret, bucket 값, token, member/file/room ID는 명령·로그·문서에 남기지 말고 bind parameter와 배포 도구의 secret store를 사용한다.

## Registry cutover의 대상과 schema

세 registry의 canonical 이름은 `file-usage`, `form-ownership`, `chat-room-ownership`이다. expand migration은 [Storage usage](../../src/main/resources/db/migration/V2026.07.16.00.00__create_file_usage_registry_and_cleanup_state.sql), [Form ownership](../../src/main/resources/db/migration/V2026.07.16.00.10__create_form_ownership_registry.sql), [Chat ownership](../../src/main/resources/db/migration/V2026.07.16.00.20__create_chat_room_ownership_registry.sql), [backfill control](../../src/main/resources/db/migration/V2026.07.16.00.30__create_registry_backfill_control.sql) 순서다.

| registry | 핵심 schema | 삭제/쓰기 불변식 |
| --- | --- | --- |
| `file-usage` | `file_usage_owner` 좌표 + `file_usage(owner_id,file_id)` + `file_metadata` lifecycle/cleanup 열 | owner 좌표와 owner/file pair unique. usage→owner는 cascade, usage→metadata는 restrict. |
| `form-ownership` | `form_ownership(form_id PK, namespace, owner_resource_key, slot)` | form FK cascade, Form ID와 owner tuple 모두 unique, binding insert-only immutable. |
| `chat-room-ownership` | `chat_room_ownership(room_id PK, namespace, owner_resource_key, slot)` | room FK cascade, room ID와 owner tuple 모두 unique, binding insert-only immutable. |
| control | `registry_backfill_checkpoint`, `registry_cutover_state` | source별 keyset checkpoint와 상태 CAS 전이. |

모든 좌표는 서버 trusted factory가 생성한다. namespace는 소문자 dot segment(전체 100자 이하), resource key는 영숫자 시작 128자 이하, slot은 소문자 kebab 50자 이하이며 요청·STOMP destination·GraphQL input으로 받지 않는다. 권한의 원본은 Form/Chat ownership row이고 기존 scalar는 navigation mirror다. 자세한 정책은 [ADR-027](../adr/027-engine-resource-ownership-namespaces.md)을 따른다.

## State machine과 gate

`registry_cutover_state.status`의 허용 전이는 다음과 같다.

```text
DISABLED -> BACKFILLING -> VALIDATED -> READY
     ^          |              |         |
     |          +-> BLOCKED <-+---------+
     +---------------- BLOCKED
```

- `DISABLED`: 기본/rollback 상태. cleanup과 strict ownership enforcement는 꺼져 있다.
- `BACKFILLING`: preflight와 checkpoint reset 후 keyset write 중이다.
- `VALIDATED`: 해당 registry의 backfill 후 reconcile이 clean이다. 아직 READY가 아니다.
- `READY`: 세 registry final reconcile, primary/replica verification, exact-one namespace coverage가 모두 clean이고 cutover fence가 승인했다.
- `BLOCKED`: preflight, drift, replica mismatch, old-writer drift, lifecycle 오류 또는 상태 경합. 수동 원인 확인 후 `DISABLED`로 되돌리고 checkpoint를 낮은 PK부터 다시 만든다.

READY 전에는 수동 물리 삭제·cleanup scheduler·strict ownership을 열지 않는다. `RegistryCutoverFenceService`는 세 registry가 모두 validated/clean일 때만 `READY`로 전이하고 `enableCleanupAndEnforcement()`를 호출한다. READY 뒤 drift가 생기면 `reconcileRuntimeFence()`가 properties를 먼저 닫고 상태를 BLOCKED로 만든다.

## Mapping catalog와 retention

### Storage usage 9개 source

`StorageFileUsageSource.ALL`의 source와 mapping은 다음과 같다. source명은 checkpoint의 `source_name`과 같다.

| source | legacy table/column | namespace | resource key | slot | shape |
| --- | --- | --- | --- | --- | --- |
| `member-profile-image` | `member.profile_image_id` | `member` | `member.id` | `profile-image` | scalar |
| `school-logo` | `school.logo_image_id` | `organization.school` | `school.id` | `logo` | scalar |
| `notice-images` | `notice_image.notice_id/image_id` | `notice` | `notice.id` | `images` | scalar row |
| `project-logo` | `project.logo_file_id` | `project` | `project.id` | `logo` | scalar |
| `project-thumbnail` | `project.thumbnail_file_id` | `project` | `project.id` | `thumbnail` | scalar |
| `form-answer-attachments` | `answer.file_ids` | `form.answer` | `answer.id` | `attachments` | array/unnest |
| `chat-message-attachments` | `chat_message.file_metadata_ids` | `chat.message` | `chat_message.id` | `attachments` | array/unnest |
| `umc-product-member-profile-image` | `umc_product_member.profile_image_id` | `organization.umc-product-member` | `umc_product_member.id` | `profile-image` | scalar |
| `certificate-file` | `certificate.file_id` | `certificate` | `certificate.id` | `file` | scalar |

### Form/Chat ownership

| source | namespace/slot | owner resource key | 원본 |
| --- | --- | --- | --- |
| `project-application-form` | `project.application-form/default` | `project_application_form.project_id` | [Project source](../../src/main/java/com/umc/product/project/adapter/out/backfill/ProjectFormOwnershipBackfillSource.java) |
| `notice-vote` | `notice.vote/default` | `notice_vote.notice_id` | [Notice source](../../src/main/java/com/umc/product/notice/adapter/out/backfill/NoticeVoteFormOwnershipBackfillSource.java) |
| `feedback-template` | `feedback.template/default` | `user_feedback_template.id` | [Feedback source](../../src/main/java/com/umc/product/feedback/adapter/out/backfill/FeedbackTemplateFormOwnershipBackfillSource.java) |
| `form-standalone` | `form.standalone/default` | `form.id` | [Form rollout](../../src/main/java/com/umc/product/form/adapter/out/backfill/FormOwnershipRolloutAdapter.java) |
| `chat-room` | `chat.standalone/default` | `chat_room.id` | [Chat rollout](../../src/main/java/com/umc/product/chat/adapter/out/backfill/ChatRoomOwnershipRolloutAdapter.java) |

동일 Form/room의 복수 owner 또는 동일 tuple의 복수 engine ID는 preflight에서 hard-fail한다. missing/stale/broken/conflict를 자동으로 고치거나 덮어쓰지 않는다.

### Storage cleanup retention과 claim

`app.storage.cleanup.enabled` 기본값은 `false`다. 기본 retention은 pending `PT24H`, confirmed-unreferenced `PT168H(7일)`, claim timeout `PT15M`, batch 100, max attempts 10, initial/max backoff `PT1M`/`PT6H`, poll interval `PT1M`이다. 환경 override 이름은 [application.yml](../../src/main/resources/application.yml)의 `FILE_CLEANUP_*`를 사용한다.

candidate transaction은 usage 0과 retention을 `FOR UPDATE SKIP LOCKED`로 확인하고 token을 발급한다. transient failure는 token과 claimed 시각을 유지하고 next attempt만 예약하며, due 시점의 DB claim query가 새 token으로 CAS reclaim한다. S3 delete 직전에 독립 transaction으로 current active token과 usage 0을 재검증한 뒤, S3 delete는 transaction 밖에서 수행한다. finalize는 token CAS + usage 0을 다시 확인한다. retry/backoff와 최종 `FAILED`도 token을 유지하므로 attach fence가 열리지 않는다. `NoSuchKey`/404는 S3 adapter에서 `not_found` idempotent success로 기록하고 metadata finalize로 진행하며, scheduler에서는 success로 집계해 retry를 예약하지 않는다.

`cleanup_failed_at` 수동 reset은 다음 guard를 모두 충족할 때만 승인한다.

1. 대상 metadata를 `FOR UPDATE`로 잠근다.
2. `SELECT COUNT(*) FROM file_usage WHERE file_id=:file_id`가 0인지 확인한다.
3. lock한 row의 현재 token을 `:failed_token`으로 보존하고, 승인 도구가 서로 다른 새 UUID `:reset_token`을 발급한다.
4. 같은 guarded transaction에서 old-token CAS로 `cleanup_claim_token=:reset_token`, `cleanup_claimed_at=CURRENT_TIMESTAMP`, `cleanup_next_attempt_at=CURRENT_TIMESTAMP`, `cleanup_failed_at=NULL`, `cleanup_attempts=0`으로 update한다. `WHERE cleanup_claim_token=:failed_token AND cleanup_failed_at IS NOT NULL AND NOT EXISTS (...)`를 포함하고 update count가 1일 때만 commit한다.
5. usage가 생기거나 token이 변했거나 update count가 1이 아니면 즉시 rollback한다. token을 `NULL`로 열지 않아 old worker의 CAS와 reset 사이 attach를 모두 차단하며, due scheduler가 또 하나의 새 token으로 reclaim한다. reset은 cleanup enabled/READY를 우회하지 않는다.

정확한 bind-parameter SQL은 [Storage 도메인 reset 절차](domain/storage.md#lifecycle과-cleanup)를 단일 원본으로 사용한다.

## registry-backfill one-shot

runner는 `registry-backfill` profile에서만 생성된다([`RegistryBackfillRunnerConfiguration`](../../src/main/java/com/umc/product/registry/adapter/in/runner/RegistryBackfillRunnerConfiguration.java)). action은 `app.registry.backfill.action`의 `BACKFILL` 또는 `RECONCILE`이고, `batch-size`와 `detail-limit`은 양수여야 한다. 아래 명령은 실제 저장소의 one-shot invocation이며 secret/실제 ID를 포함하지 않는다.

```bash
# keyset backfill + source reconcile. runner가 완료 후 Spring context를 종료한다.
./gradlew bootRun --args='--spring.profiles.active=registry-backfill --app.registry.backfill.action=BACKFILL --app.registry.backfill.batch-size=500 --app.registry.backfill.detail-limit=100'

# read-only source/registry reconcile. backfill row/checkpoint/state를 쓰지 않는다.
./gradlew bootRun --args='--spring.profiles.active=registry-backfill --app.registry.backfill.action=RECONCILE --app.registry.backfill.detail-limit=100'
```

환경 변수로 같은 binding을 전달하려면 `SPRING_PROFILES_ACTIVE=registry-backfill APP_REGISTRY_BACKFILL_ACTION=BACKFILL`처럼 사용하되, 운영 secret은 shell history에 노출하지 않는다.

### Action별 write/read-only

| action | 실제 동작 | 운영 판정 |
| --- | --- | --- |
| `BACKFILL` | advisory lock 획득, preflight, checkpoint reset/저장, source table read, registry row insert, Storage의 `confirmed_at` 호환 보정 및 `unreferenced_at` 계산, final reconcile, 상태 `BACKFILLING→VALIDATED` 전이 | write action. primary에만 연결하고 replica routing endpoint로 실행하지 않는다. |
| `RECONCILE` | 각 rollout의 source/registry/metadata를 읽고 `RegistryReconciliationResult.summary()`를 log로 출력한다. advisory lock은 잡지만 business row/checkpoint/state를 변경하지 않는다. | data read-only. 결과 drift가 있어도 coordinator는 result를 반환하므로 exit 0만으로 clean을 판단하지 않는다. |

`BACKFILL`에서 drift 또는 예외가 나면 상태를 `BLOCKED`로 만들고 예외를 전파한다. 이미 `VALIDATED`/`READY`인 registry는 다시 BACKFILL하지 않는다. `BLOCKED` 재시도는 `DISABLED` 전이와 checkpoint reset 후 낮은 PK부터 full rescan한다.

### Exit code 해석

- `BACKFILL` exit `0`: runner가 종료했고 각 rollout의 reconcile이 clean하여 세 registry가 `VALIDATED`까지 갔다는 뜻이다. `READY`와 properties enable은 아직 cutover fence의 별도 gate다.
- `BACKFILL` non-zero: preflight/table/DB 오류, checkpoint 경합, source mapping 오류 또는 drift 예외다. 해당 registry가 `BLOCKED`인지 status query로 확인하고 원인 정리 전 재실행하지 않는다.
- `RECONCILE` exit `0`: 읽기 작업이 끝났다는 뜻일 뿐 drift 0이 아니다. 로그의 `registry=...,totalDrift=...`와 source counts를 확인한다.
- `RECONCILE` non-zero: profile binding/startup, DB connection, adapter 예외다. 로그와 status를 함께 확인한다.

## Preflight, checkpoint, status와 drift 판정

### Preflight

1. migration history에 `.00`, `.10`, `.20`, `.30`이 있고 대상 table/constraint/index가 존재하는지 확인한다.
2. `SELECT pg_is_in_recovery()` 결과가 `false`인 primary인지 확인한다. replica endpoint에서 `BACKFILL`을 실행하지 않는다.
3. Form source는 `project_id`별 복수 application form과 mapping source의 duplicate coordinate를 검사한다. Chat source는 `chat_room`과 ownership table 존재를 확인한다. Storage source는 9개 table/column/array type과 file metadata FK를 확인한다.
4. dual-writer가 배포됐고 old pod가 drain됐으며 모든 serving pod version이 canonical mapping을 지원하는지 deployment inventory로 확인한다.
5. baseline으로 primary latency/lock, replica replay lag/WAL disk, cleanup/enforcement property를 기록한다. 값이 임계치를 넘으면 시작하지 않는다.

### Checkpoint/status 조회

운영 console의 read-only SQL session에서 실제 ID 대신 일반 조회를 실행한다.

```sql
SELECT registry_name, status, verified_at, details, updated_at
FROM registry_cutover_state
ORDER BY registry_name;

SELECT registry_name, source_name, last_parent_id, processed_rows, completed, updated_at
FROM registry_backfill_checkpoint
ORDER BY registry_name, source_name;

SELECT pg_is_in_recovery() AS is_replica;
```

`last_parent_id`는 source별 keyset 경계이고 `completed=true`인 source만 다음 단계로 간주한다. checkpoint가 증가하지 않거나 source 순서가 뒤집히면 작업을 중단하고 `BLOCKED` 원인을 확인한다.

### Four-way drift

모든 registry와 source는 다음 네 가지를 별도로 세고 detail limit 안에서 좌표를 기록한다.

| 판정 | 코드 enum | 의미와 조치 |
| --- | --- | --- |
| missing | `SOURCE_ONLY_MISSING` | canonical source에는 있는데 registry row/usage가 없다. dual-writer·batch를 고친 뒤 재backfill한다. |
| stale | `REGISTRY_ONLY_STALE` | registry에만 남은 row다. 원본 삭제/rollback 흔적을 확인하고 자동 삭제하지 않는다. |
| broken | `BROKEN_REFERENCE` | source가 가리키는 metadata/Form/room이 없다. 원본 FK/legacy 데이터를 운영자가 정리할 때까지 차단한다. |
| conflict | `OWNERSHIP_CONFLICT` | 같은 owner tuple의 다른 file/engine ID, mirror mismatch, 기대 namespace/slot 불일치다. 임의 overwrite 금지, owner 결정을 명시적으로 정리한다. |

추가 blocker인 `DUPLICATE_REFERENCE`, `INVALID_REFERENCE`, `LIFECYCLE_CONFLICT`, `UPLOAD_CONFIRMATION_MISMATCH`, Form duplicate owner도 drift 0 gate에 포함한다. 네 종류를 합쳐 “정합성 양호”로 축약하지 말고 source별 count와 detail을 보존한다.

### Exact-one namespace coverage

Form coverage의 required namespace는 `form.standalone`, `project.application-form`, `notice.vote`, `feedback.template`이고 Chat coverage는 `chat.standalone`이다. `RegistryReadinessService`는 persisted namespace와 consumer declared namespace의 합집합을 만들고 evaluator count가 각 namespace당 정확히 1인지 확인한다. 0개·2개 이상·malformed·coverage adapter 예외·empty coverage는 모두 invalid이며 `STRICT`/READY를 열지 않는다.

```sql
SELECT DISTINCT namespace FROM form_ownership ORDER BY namespace;
SELECT DISTINCT namespace FROM chat_room_ownership ORDER BY namespace;
```

위 SQL은 persisted 집합만 보여 주므로 declared/evaluator 집합은 application readiness 결과와 registry test artifact로 함께 확인한다. client가 namespace를 추가하거나 기존 evaluator를 우회하는 fallback은 허용하지 않는다.

## Primary/replica 검증

`BACKFILL`은 primary write endpoint에서 keyset batch를 commit한다. physical replica는 WAL replay, logical subscriber는 additive schema와 apply 상태를 먼저 확인한다. primary에서 다음을 관측한다.

```sql
SELECT application_name, state, sync_state,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)) AS replay_gap,
       write_lag, flush_lag, replay_lag
FROM pg_stat_replication;

SELECT slot_name, slot_type, active, wal_status,
       pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal
FROM pg_replication_slots;
```

필수 replica가 `streaming`에서 이탈하거나 baseline 대비 replay gap/WAL disk/DB latency/standby conflict가 증가하면 batch를 중지하고 따라잡을 때까지 기다린다. 모든 replica에서 migration, registry row count, lifecycle contract와 reconcile 결과가 보이고 logical apply error가 없어야 한다. synchronous durability를 낮춰서 속도를 확보하지 말고 batch size/속도를 줄인다.

## Forward rollout 순서

`RegistryCutoverPlan`이 요구하는 milestone 전체 순서를 [코드](../../src/main/java/com/umc/product/registry/domain/RegistryCutoverMilestone.java)와 동일하게 지킨다.

1. `SCHEMA_EXPAND_ARTIFACT`: 네 expand migration을 replica/subscriber에 호환 순서로 배포한다. `.00.40` contract는 아직 적용하지 않는다.
2. `STORAGE_MAINTENANCE_START`: 운영 control plane에서 정확히 `STORAGE` maintenance를 시작하고 Storage 쓰기/삭제 변경을 관찰한다.
3. `DUAL_WRITER_ROLLING_DEPLOY`: 모든 runtime writer가 legacy source와 registry snapshot/binding을 같은 REQUIRED transaction에서 기록하도록 rolling deploy한다. 새 pod와 old pod가 섞이는 동안에는 backfill을 시작하지 않는다.
4. `OLD_POD_DRAIN_AND_VERSION_CHECK`: old pod를 drain하고 serving pod image/version·writer capability inventory가 한 버전인지 확인한다. old writer가 남아 있으면 drift가 다시 생기므로 gate를 통과시키지 않는다.
5. `STORAGE_MAINTENANCE_END`: dual-writer와 version 확인이 끝난 뒤 maintenance를 종료한다. 종료 시점을 deployment log에 기록한다.
6. `KEYSET_BACKFILL`: 위 `BACKFILL` one-shot을 primary에서 실행한다. source별 checkpoint와 advisory lock으로 재시작한다.
7. `PRIMARY_REPLICA_FOUR_DRIFT_ZERO`: primary와 모든 필수 replica가 catch up한 뒤 네 drift와 추가 lifecycle/duplicate drift가 모두 0인지 확인한다.
8. `ROLLBACK_WINDOW_END`: 관측 window 동안 old-writer drift, replica mismatch, cleanup retry 폭증이 없는지 확인한다.
9. `CONTRACT_00_40`: [lifecycle contract migration](../../src/main/resources/db/migration/V2026.07.16.00.40__enforce_file_upload_lifecycle.sql)을 `NOT VALID` 추가 → legacy mismatch 보정 → `VALIDATE` 순서로 실행하고 모든 replica에서 `convalidated=true`를 확인한다.
10. `FINAL_RECONCILE`: `RECONCILE` one-shot과 readiness coverage를 다시 실행한다. 결과가 하나라도 non-zero이면 `BLOCKED`로 닫는다.
11. `THREE_REGISTRIES_READY`: 세 registry를 `VALIDATED→READY`로 전환한다. `READY`는 application property만 true로 바꿨다는 뜻이 아니다.
12. `PROPERTIES_ENABLE`: cutover fence의 `enableCleanupAndEnforcement()`를 호출한다. `app.engine-ownership.enforcement-enabled=true`와 `app.storage.cleanup.enabled=true`를 readiness/운영 config에 반영하되, 두 property 모두 세 registry READY와 final reconcile 뒤에만 enable한다. 이 호출은 code에서 한 번의 atomic runtime switch로 기록한다.

### Property 순서

- 시작/대기: `app.storage.cleanup.enabled=false`, `app.engine-ownership.enforcement-enabled=false`를 명시한다. repository 기본값도 둘 다 false다.
- forward enable: `registry_cutover_state` 세 행이 `READY`이고 final reconcile/replica/coverage가 clean인지 확인한 다음 runtime switch로 enforcement와 cleanup을 함께 enable한다. cleanup을 READY 전 수동으로 먼저 켜지 않는다.
- drift/rollback disable: `STORAGE` maintenance를 먼저 시작하고 세 registry를 `BLOCKED→DISABLED`로 닫은 다음 `disableCleanupAndEnforcement()`로 두 property를 false로 만든다. 그 후 traffic transition을 수행한다. transition 실패 시에도 properties는 false이며 traffic은 전환하지 않는다.

## Rollback과 재시작

rollback 사유는 status `details`와 deployment log에 남기되 secret·실제 ID는 남기지 않는다.

1. `STORAGE` maintenance를 시작한다.
2. runtime switch를 통해 cleanup/enforcement가 더 이상 write/delete를 하지 않게 준비한다.
3. 각 registry를 현재 상태에서 `BLOCKED`로 만든 뒤 `DISABLED`로 CAS 전이한다. 이미 `DISABLED`면 no-op이다.
4. `app.storage.cleanup.enabled=false`와 `app.engine-ownership.enforcement-enabled=false`를 확인한다.
5. traffic을 이전 version/경로로 전환한다. 상태 전이나 property disable이 실패하면 traffic을 전환하지 않는다.
6. 다음 forward rollout에서 source checkpoint를 삭제/reset하고 old-writer 변경까지 낮은 PK부터 full backfill한다. stale registry row는 보존한 채 reconcile 결과로 확인하고, 임의 삭제하지 않는다.

READY 이후 runtime fence에서 old-writer source row, replica mismatch, namespace coverage 오류가 보이면 자동으로 properties를 닫고 세 registry를 `BLOCKED`로 전환한다. 원인 해결 전 재-enable하지 않는다.

## 완료 체크리스트와 증거

- [ ] start/end `STORAGE` maintenance, dual-writer rollout, old pod drain/version check가 deployment log에 있다.
- [ ] `BACKFILL` exit와 registry별 `VALIDATED` status, source checkpoint `completed=true`를 확인했다.
- [ ] `RECONCILE` summary의 `totalDrift=0`과 네 drift 및 추가 lifecycle drift 0을 확인했다.
- [ ] primary `pg_is_in_recovery=false`, 필수 replica streaming/catch-up, logical apply error 없음, WAL disk 정상이다.
- [ ] Form/Chat exact-one namespace coverage가 empty/missing/duplicate가 아니다.
- [ ] `.00.40` lifecycle contract가 모든 replica에서 validated다.
- [ ] 세 registry만 `READY`로 바꾸고 properties enable은 마지막에 한 번 수행했다.
- [ ] rollback window와 `READY` 후 runtime fence가 clean이다.
- [ ] cleanup claim/CAS, S3 not-found idempotent success, failed reset guard와 metric을 점검했다.

운영 evidence에는 invocation, exit code, status/checkpoint SQL 결과, reconcile summary, replica lag snapshot, lifecycle constraint validation, property switch/maintenance 순서를 포함한다. production secret·실제 ID·token은 redaction한다.

## 목적

이 문서는 운영 중인 PostgreSQL 데이터베이스에서 기존 데이터를 대량으로 보정하는 backfill을 안전하게 수행하기 위한 기준을 정리한다. 특히 physical 또는 logical replica가 있을 때 발생할 수 있는 replication lag, WAL 누적, standby query conflict, failover 문제를 중점적으로 다룬다.

Index 생성은 기존 row를 변경하는 일반적인 data backfill과는 다르다. 하지만 기존 데이터를 전체 스캔하고 I/O, CPU, WAL, lock에 영향을 준다는 점에서 동일한 운영 절차와 관측 기준을 적용한다.

## 핵심 원칙

Backfill의 WAL 생성 속도가 가장 느린 필수 replica의 WAL replay 속도를 지속적으로 넘지 않게 한다.

다음 원칙을 기본값으로 사용한다.

- 전체 row를 하나의 transaction에서 변경하지 않는다.
- Primary key 기반 keyset pagination으로 작은 batch를 처리한다.
- 각 batch는 독립적으로 commit하고 재실행 가능하게 만든다.
- 처리 속도는 primary 처리량이 아니라 replica replay 상태를 기준으로 조절한다.
- 실행 전에 중단 조건과 복구 절차를 정한다.
- Schema 변경은 expand, backfill, contract 순서로 배포한다.
- 모든 replica가 따라잡기 전에는 신규 column이나 상태로 read 경로를 완전히 전환하지 않는다.

## 주요 위험

| 위험 | Primary 영향 | Replica 영향 |
|---|---|---|
| 큰 단일 transaction | Lock 장기 보유, rollback 비용 증가, vacuum 지연 | 변경 가시성이 늦어지고 lag가 한꺼번에 증가할 수 있다. |
| 과도한 batch 속도 | CPU, I/O, WAL 생성량 급증 | WAL 수신, flush, replay가 밀린다. |
| 대량 `UPDATE` | Dead tuple과 autovacuum 부하 발생 | Vacuum WAL과 standby 장기 조회가 충돌할 수 있다. |
| Indexed column 변경 | Row마다 index 갱신이 발생하고 HOT update가 제한된다. | 추가 WAL과 replay I/O가 발생한다. |
| Replication slot | Replica가 소비하지 못한 WAL을 계속 보관한다. | Replica 장애가 primary 디스크 고갈로 전파될 수 있다. |
| Synchronous replica | Commit이 replica 응답을 기다린다. | 느린 replica가 primary transaction latency를 직접 높인다. |
| Logical replica | 변경 row마다 logical decoding과 전송 비용이 발생한다. | Schema 또는 constraint 불일치가 replication 중단으로 이어질 수 있다. |

## 실행 전 확인

### Replication 구조 확인

다음 항목을 먼저 확인한다.

- Replica가 physical replication인지 logical replication인지 확인한다.
- Replica별 `sync_state`가 `sync`, `quorum`, `potential`, `async` 중 무엇인지 확인한다.
- Cascading replica가 있다면 primary에서 직접 보이지 않는 downstream replica까지 확인한다.
- Replication slot 사용 여부와 WAL 보존 한도를 확인한다.
- Read traffic을 처리하는 replica와 failover 전용 replica를 구분한다.
- Logical subscriber를 failover 대상으로 사용하는 경우 sequence 동기화 방법을 확인한다.

### 기준값과 중단 조건 정의

Backfill 시작 전에 평시 기준값을 기록한다.

- Primary API DB latency와 transaction latency
- Primary와 replica의 CPU, disk IOPS, disk queue
- WAL 생성 속도와 replica replay 속도
- Replica별 LSN byte gap과 시간 기준 lag
- `pg_wal` 및 전체 disk 여유 공간
- Table의 `n_dead_tup`, autovacuum 실행 상태
- Standby query cancellation 횟수

환경마다 허용 가능한 값이 다르므로 고정된 임계값을 공통 규칙으로 사용하지 않는다. 서비스의 replica staleness SLO와 disk 용량을 기준으로 다음 중단 조건을 사전에 정한다.

- Replica가 `streaming` 상태에서 이탈한다.
- Replay gap이 정해진 byte 또는 시간 기준을 넘는다.
- Primary 또는 replica DB latency가 허용치를 넘는다.
- Replication slot이 보관한 WAL이 disk 안전 범위를 넘는다.
- Autovacuum이 dead tuple 증가 속도를 따라가지 못한다.
- Replica의 query cancellation이 급증한다.

조건을 넘으면 batch 크기만 줄이는 데 그치지 않고 작업을 일시 중지해 replica가 따라잡도록 한다.

### Backfill 대상과 side effect 확인

다음 항목을 확인한다.

- Update 대상 column에 연결된 index가 있는지 확인한다.
- Trigger, audit log, domain event 또는 외부 연동이 row마다 실행되는지 확인한다.
- Foreign key 검사에 필요한 referenced index가 존재하는지 확인한다.
- ORM callback이나 애플리케이션 이벤트를 거치지 않고 직접 SQL을 실행해도 되는지 확인한다.
- Backfill 중 발생하는 신규 write가 새 column도 함께 채우는지 확인한다.
- 중단 후 재실행했을 때 동일한 결과가 나오는지 확인한다.

## Schema 변경 순서

운영 중 schema와 데이터를 함께 변경할 때는 다음 순서를 사용한다.

1. Nullable column 또는 호환 가능한 신규 schema를 추가한다.
2. 애플리케이션이 신규 write부터 기존 값과 새 값을 함께 기록하도록 배포한다.
3. 기존 row를 batch backfill한다.
4. Primary와 모든 replica의 backfill 및 replication 완료를 확인한다.
5. 애플리케이션 read 경로를 신규 column으로 전환한다.
6. Constraint를 검증한다.
7. 이전 column과 호환 로직은 별도 배포에서 제거한다.

PostgreSQL은 non-volatile constant default를 metadata로 처리할 수 있어 table rewrite 없이 column을 빠르게 추가할 수 있다. 반면 `clock_timestamp()` 같은 volatile default, 일부 generated column, identity column 또는 type 변경은 table과 index 전체 rewrite를 유발할 수 있으므로 실행 계획을 별도로 확인한다.

기존 데이터 검증이 필요한 constraint는 최초 DDL과 validation을 분리한다.

```sql
ALTER TABLE target_table
    ADD CONSTRAINT target_new_column_nn
    CHECK (new_column IS NOT NULL) NOT VALID;

ALTER TABLE target_table
    VALIDATE CONSTRAINT target_new_column_nn;
```

`NOT VALID` 상태에서도 신규 `INSERT`와 `UPDATE`에는 constraint가 적용된다. 이후 `VALIDATE CONSTRAINT`가 기존 row를 스캔한다.

## Batch 실행 방식

### Keyset 기반 처리

`OFFSET` 기반 pagination은 뒤쪽 batch로 갈수록 스캔 비용이 커지고 concurrent write에 취약하므로 사용하지 않는다. Primary key 또는 변경되지 않는 unique key를 기준으로 처리한다.

```sql
WITH target AS (
    SELECT id
    FROM target_table
    WHERE id > :last_id
      AND new_column IS NULL
    ORDER BY id
    LIMIT :batch_size
)
UPDATE target_table AS t
SET new_column = :backfill_value
FROM target
WHERE t.id = target.id
RETURNING t.id;
```

각 batch에서 반환된 가장 큰 `id`를 checkpoint로 저장한다. `new_column IS NULL`처럼 이미 처리된 row를 제외하는 조건을 함께 사용해 동일 batch가 다시 실행돼도 안전하게 만든다.

### Transaction 크기

- 각 batch를 별도 transaction으로 실행한다.
- 처음에는 worker 하나와 작은 batch로 시작한다.
- Batch 처리 시간, WAL 증가량, replay gap을 보고 크기를 점진적으로 조절한다.
- 여러 worker를 병렬 실행하면 WAL 생성과 lock 경쟁도 함께 증가하므로 기본값으로 사용하지 않는다.
- Synchronous replica가 있으면 각 batch commit마다 replica 응답을 기다린다는 점을 반영한다.

필요하면 batch transaction에 제한 시간을 적용한다.

```sql
SET LOCAL lock_timeout = '1s';
SET LOCAL statement_timeout = '30s';
```

시간 값은 예시이며 실제 값은 평시 query latency와 batch 목표 시간에 맞춘다. Timeout이 발생한 batch는 전체 rollback한 뒤 크기를 줄여 재시도한다.

### Indexed column 처리

Update 대상 column이 index에 포함되면 각 row마다 heap과 index가 함께 갱신되고 HOT update를 사용하지 못할 수 있다. 새 column에 대한 신규 index라면 일반적으로 다음 순서를 우선 검토한다.

1. 신규 write가 새 column을 기록하도록 한다.
2. 기존 row를 backfill한다.
3. Index를 생성한다.
4. `ANALYZE`를 실행한다.

다만 backfill query 자체에 해당 index가 반드시 필요하거나 backfill 중 uniqueness를 강제해야 한다면 index를 먼저 생성할 수 있다. 기존 서비스 query가 사용 중인 index는 backfill 속도를 위해 임의로 제거하지 않는다.

## Physical replica 유의사항

Physical replica는 primary의 SQL을 다시 실행하지 않고 WAL을 재생한다. 대량 backfill이 primary에서 생성한 WAL은 replica에서도 유사한 I/O 작업을 발생시킨다.

### Replica lag 관측

Primary에서 다음 query로 직접 연결된 replica 상태를 확인한다.

```sql
SELECT
    application_name,
    state,
    sync_state,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn)
    ) AS replay_gap,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;
```

`write_lag`, `flush_lag`, `replay_lag`는 최근 WAL 처리 지연을 보여주지만 replica가 따라잡는 데 걸릴 예상 시간은 아니다. Replica가 완전히 따라잡고 WAL 활동이 없으면 이전 값이 잠시 남아 있거나 `NULL`이 될 수 있다. 시간 값과 LSN byte gap을 함께 관측한다.

### Standby query conflict

Backfill `UPDATE`는 dead tuple을 만든다. Primary의 vacuum cleanup WAL을 replica가 적용할 때 standby의 장기 조회가 과거 row version을 참조하고 있으면 다음 중 하나가 발생한다.

- WAL replay가 대기하면서 replica lag가 증가한다.
- `max_standby_streaming_delay`를 넘으면 standby query가 취소된다.
- `hot_standby_feedback=true`이면 cleanup conflict는 줄지만 primary가 dead tuple을 제거하지 못해 table bloat가 증가할 수 있다.

Replica에서 conflict 발생 현황을 확인한다.

```sql
SELECT *
FROM pg_stat_database_conflicts;
```

HA 전용 replica는 replay 지연을 짧게 유지하는 것이 우선이고, 장기 분석 query가 필요한 replica는 별도로 분리하는 것이 안전하다.

### Synchronous replica

Synchronous replication에서는 commit이 설정된 단계까지 replica 응답을 기다린다.

- `synchronous_commit=remote_write`: Replica OS write까지 기다린다.
- `synchronous_commit=on`: Replica WAL flush까지 기다린다.
- `synchronous_commit=remote_apply`: Replica replay와 query 가시성까지 기다린다.

Replica가 느려지면 backfill뿐 아니라 일반 application transaction의 commit latency와 lock 보유 시간도 증가할 수 있다. Backfill 때문에 durability 정책을 임의로 낮추기보다 batch 크기와 실행 속도를 먼저 조절한다.

### Replication slot과 WAL disk

Replica가 replication slot을 사용하면 아직 소비되지 않은 WAL이 primary에 보관된다.

```sql
SELECT
    slot_name,
    slot_type,
    active,
    wal_status,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)
    ) AS retained_wal,
    pg_size_pretty(safe_wal_size) AS safe_wal_size
FROM pg_replication_slots;
```

`safe_wal_size` 등 일부 column은 PostgreSQL 버전과 설정에 따라 사용할 수 없을 수 있다.

`max_slot_wal_keep_size=-1`이면 replication slot이 WAL을 제한 없이 보관할 수 있어 primary의 `pg_wal` disk를 채울 수 있다. 반대로 한도를 너무 작게 잡으면 replica가 따라잡기 전에 필요한 WAL이 제거되어 replica를 다시 구성해야 할 수 있다. Backfill 전에 실제 WAL 생성량과 replica 장애 시 복구 시간을 기준으로 안전 범위를 정한다.

## Logical replica 유의사항

Logical replication에서는 DDL이 자동으로 복제되지 않는다. Subscriber schema가 publisher의 신규 데이터를 수용하지 못하면 replication apply가 중단될 수 있다.

다음 순서를 사용한다.

1. 호환 가능한 additive schema를 subscriber에 먼저 적용한다.
2. Publisher schema와 application write를 변경한다.
3. Backfill을 실행한다.
4. Subscriber apply 상태와 데이터 정합성을 확인한다.
5. 호환성 제거 DDL은 모든 subscriber 전환 후 적용한다.

추가로 다음 항목을 확인한다.

- `UPDATE`와 `DELETE`를 복제하는 table에 primary key 또는 적절한 replica identity가 있어야 한다.
- `REPLICA IDENTITY FULL`은 전체 row를 식별에 사용하므로 대량 backfill에서 비효율적일 수 있다.
- Subscriber의 constraint, 권한, row-level security 위반은 replication을 중단시킬 수 있다.
- Sequence 값은 logical replication으로 자동 동기화되지 않는다.
- Subscriber를 failover 대상으로 사용한다면 승격 전에 sequence 값을 별도로 동기화해야 한다.
- Index DDL도 자동 복제되지 않으므로 subscriber에 별도로 생성해야 한다.

## Failover와 작업 중복 방지

Backfill 도중 failover가 발생할 수 있으므로 작업을 재개 가능하고 중복 실행에 안전하게 만든다.

- Backfill은 read endpoint가 아닌 현재 primary write endpoint에 연결한다.
- 실행 전 현재 연결이 primary인지 확인한다.

```sql
SELECT pg_is_in_recovery();
```

결과가 `false`인 서버에서만 backfill을 실행한다.

- Advisory lock 또는 job lease를 사용해 active worker를 하나로 제한한다.
- Checkpoint를 process memory가 아니라 DB에 영속화한다.
- Connection 종료와 failover 후 lock이 해제되더라도 새 primary에서 checkpoint부터 재개할 수 있게 한다.
- 동일 row를 다시 처리해도 같은 결과가 되도록 update 조건을 설계한다.
- Failover 직후에는 replica 승격과 routing 안정화를 확인한 뒤 작업을 재개한다.

## 완료 조건

Primary의 마지막 row를 처리한 시점만으로 완료 처리하지 않는다. 다음 조건을 모두 확인한다.

1. Primary에서 대상 row가 모두 처리됐다.
2. 누락 조건 query 결과가 0건이다.
3. 모든 필수 replica가 `streaming` 상태다.
4. Replica의 LSN gap이 평시 수준으로 돌아왔다.
5. Replica에서도 신규 값이 조회된다.
6. Logical subscriber에 apply error가 없다.
7. Table의 dead tuple과 autovacuum 상태가 안정적이다.
8. `ANALYZE target_table`로 planner 통계를 갱신했다.
9. Constraint validation이 완료됐다.
10. Application read 경로 전환 후 error rate와 DB latency가 정상이다.

대량 변경 후에는 데이터 분포가 달라질 수 있으므로 `ANALYZE`를 명시적으로 실행한다. 일반 `VACUUM`은 읽기와 쓰기를 병행할 수 있지만 `VACUUM FULL`은 table rewrite와 `ACCESS EXCLUSIVE` lock이 필요하므로 backfill 후 습관적으로 실행하지 않는다.

## Index DDL 선택 기준

일반 index 생성은 table write를 차단할 수 있지만 하나의 transaction으로 빠르게 실행되고 실패 시 rollback된다.

```sql
CREATE INDEX idx_target ON target_table (target_column);
```

`CREATE INDEX CONCURRENTLY`는 index 생성 중 `INSERT`, `UPDATE`, `DELETE`를 허용한다.

```sql
CREATE INDEX CONCURRENTLY idx_target
    ON target_table (target_column);
```

대신 다음 비용이 있다.

- Table을 여러 단계로 스캔해 일반 생성보다 오래 걸릴 수 있다.
- 추가 CPU와 I/O를 사용한다.
- 기존 장기 transaction이 종료되기를 기다릴 수 있다.
- PostgreSQL transaction block 안에서 실행할 수 없다.
- 중간 실패 시 invalid index가 남을 수 있다.
- Flyway에서는 migration별 non-transactional 실행 설정이 필요하다.

다음 조건을 모두 만족할 때 concurrent index DDL을 우선 검토한다.

- Table이 충분히 크다.
- 운영 중 write가 계속 발생한다.
- Index 생성 시간 동안 write 중단을 허용할 수 없다.
- Non-transactional DDL 실패와 재실행 절차가 준비돼 있다.

Table이 비어 있거나 해당 기능이 비활성화돼 write가 발생하지 않는다면 일반 transactional index DDL을 사용한다.

## Event Outbox 적용

Event Outbox는 `OutboxDomainEventPublisher`가 항상 활성화되는 필수 인프라다. 회귀 시에도 Spring local publisher로 우회하지 않고 `EVENT_OUTBOX_RELAY_ENABLED=false`로 poller만 중지해 추가 dispatch를 차단한 뒤 PENDING 적체와 DB 상태를 점검한다.

Outbox가 활성화되기 전에는 write가 발생하지 않았으므로 polling index 변경에는 일반 transactional index DDL을 사용한다. 활성 배포는 다음 순서로 진행한다.

1. Partial index migration을 먼저 배포한다.
2. Physical replica의 migration replay 또는 logical subscriber의 별도 index 생성을 확인한다.
3. 모든 replica가 따라잡고 DB 지표가 안정적인지 확인한다.
4. 통합 환경에서 Outbox write와 polling을 검증한다.
5. 기본 활성화 상태로 애플리케이션을 시작한다.
6. Pending 적체, FAILED row, polling lag, replica lag와 WAL 증가량을 함께 관측한다.

빈 Outbox table에 index를 먼저 생성하면 별도 data backfill 없이 가장 안전하게 활성화를 시작할 수 있다.

## 실행 체크리스트

### 시작 전

- [ ] Physical 또는 logical replication 구성을 확인했다.
- [ ] Synchronous replica와 failover 대상 replica를 확인했다.
- [ ] 평시 replica lag, WAL 생성량, DB latency를 기록했다.
- [ ] 중단 임계값과 담당자를 정했다.
- [ ] Batch query가 idempotent하고 checkpoint로 재개 가능하다.
- [ ] 신규 write가 backfill 대상 column을 함께 기록한다.
- [ ] Trigger, event, audit side effect를 확인했다.
- [ ] Replication slot의 WAL 보존량과 disk 여유 공간을 확인했다.

### 실행 중

- [ ] 작은 batch와 worker 하나로 시작했다.
- [ ] LSN byte gap과 시간 기준 lag를 함께 관측한다.
- [ ] Primary API latency와 lock wait를 관측한다.
- [ ] Replica query cancellation을 관측한다.
- [ ] Dead tuple과 autovacuum 상태를 관측한다.
- [ ] 임계값을 넘으면 즉시 일시 중지한다.

### 완료 후

- [ ] Primary 누락 row가 0건이다.
- [ ] 모든 필수 replica가 평시 lag로 복구됐다.
- [ ] Logical subscriber apply error가 없다.
- [ ] Primary와 replica에서 표본 데이터가 일치한다.
- [ ] `ANALYZE`를 실행했다.
- [ ] Constraint validation을 완료했다.
- [ ] Application read 전환 후 error와 latency가 정상이다.

## 참고 자료

- [PostgreSQL: Hot Standby](https://www.postgresql.org/docs/current/hot-standby.html)
- [PostgreSQL: Log-Shipping Standby Servers](https://www.postgresql.org/docs/current/warm-standby.html)
- [PostgreSQL: pg_stat_replication](https://www.postgresql.org/docs/current/monitoring-stats.html)
- [PostgreSQL: Logical Replication Restrictions](https://www.postgresql.org/docs/current/logical-replication-restrictions.html)
- [PostgreSQL: Publication and Replica Identity](https://www.postgresql.org/docs/current/logical-replication-publication.html)
- [PostgreSQL: ALTER TABLE](https://www.postgresql.org/docs/current/sql-altertable.html)
- [PostgreSQL: CREATE INDEX](https://www.postgresql.org/docs/current/sql-createindex.html)
- [PostgreSQL: Populating a Database](https://www.postgresql.org/docs/current/populate.html)
- [PostgreSQL: VACUUM](https://www.postgresql.org/docs/current/sql-vacuum.html)
