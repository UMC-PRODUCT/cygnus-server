# Global 테스트 케이스

- 테스트 파일: 86개
- 테스트 케이스: 320개 (`@Test` 317 + `@ParameterizedTest` 2 + `@RepeatedTest` 1)
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`
- 문서 범위: 아래 목록은 architecture와 운영에 영향이 큰 대표 계약을 추적한다. 전체 테스트 목록의
  source of truth는 `src/test/java/com/umc/product/global`이며, 파일/케이스 개수는 각각 `rg --files`와
  `@Test` 계열 annotation 기준이다.

## Issue #1147 outbox 신규·회귀 계약 map

아래 클래스는 이번 공용 outbox 일반화에서 추가되거나 계약을 확장한 실제 테스트다. 각 클래스명과
경로는 `src/test/java/com/umc/product/global`의 현재 파일을 기준으로 하며, 테스트 목적을 요약한다.

| 테스트 클래스 | 위치 | 검증 목적 |
|---|---|---|
| `EventOutboxTest` | [`event/domain/EventOutboxTest.java`](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java) | fingerprint/`availableAt` microsecond 절삭, immutable 예약 시각, 상태 전이·재시도·안전한 상태 DTO, 입력·conflict code를 검증한다. |
| `EventPayloadSerializerTest` | [`event/adapter/out/EventPayloadSerializerTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java) | metadata 제외 canonical fingerprint가 map 순서에 독립적이고 nested object는 정렬하며, 배열·null·숫자·Unicode 차이는 구분하고 full payload를 보존하는지 검증한다. |
| `EventPayloadDeserializerTest` | [`event/adapter/out/EventPayloadDeserializerTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java) | 저장된 event class/payload 복원 성공과 잘못된 class·payload의 실패 경계를 검증한다. |
| `OutboxDomainEventPublisherTest` | [`event/adapter/out/OutboxDomainEventPublisherTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java) | 기존 `publish`/`publishAll`이 직발행 없이 fingerprint·예약 시각과 traceparent를 outbox에 저장하는지 검증한다. |
| `OutboxDomainEventPublisherPublishOnceTest` | [`event/adapter/out/OutboxDomainEventPublisherPublishOnceTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherPublishOnceTest.java) | 신규·동일 identity dedupe, class/type/payload/`availableAt` mismatch conflict, legacy null conflict와 null 입력 거부를 검증한다. |
| `EventOutboxPublisherConfigurationTest` | [`event/adapter/out/EventOutboxPublisherConfigurationTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/EventOutboxPublisherConfigurationTest.java) | legacy enable property가 publisher를 끄지 않고 relay-only disable이 poller만 끄는지 검증한다. |
| `EventOutboxAtomicInsertJpaRepositoryTest` | [`event/adapter/out/persistence/EventOutboxAtomicInsertJpaRepositoryTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxAtomicInsertJpaRepositoryTest.java) | native atomic insert가 원본 payload·metadata·fingerprint·예약 시각·audit/version을 보존하고 duplicate를 무시하는지 검증한다. |
| `EventOutboxJpaRepositoryTest` | [`event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java) | due query 순서와 partial index, microsecond round-trip, `availableAt` 불변성, legacy null load, future 제외, schema constraint, lease fencing을 검증한다. |
| `EventOutboxPersistenceAdapterTest` | [`event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java) | save/saveAll, atomic insert 결과 매핑, event ID 조회와 publishable 조회 포트 위임을 검증한다. |
| `EventOutboxPublishOnceIntegrationTest` | [`event/application/service/EventOutboxPublishOnceIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxPublishOnceIntegrationTest.java) | PostgreSQL 동시 동일 요청에서 row 1개·writer 1개·나머지 deduplicated를 검증하고 class/type/nested payload/예약 시각/legacy 불일치 conflict가 원본을 보존하는지 검증한다. |
| `EventOutboxRelayServiceTest` | [`event/application/service/EventOutboxRelayServiceTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayServiceTest.java) | 기본 relay 성공·claim-one·bounded retry·최대 시도 실패와 `OutboxDispatchFailure(retryable=false)`의 첫 실패 즉시 `FAILED`를 검증한다. |
| `EventOutboxRetentionPropertiesBindingTest` | [`event/adapter/in/scheduler/EventOutboxRetentionPropertiesBindingTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/in/scheduler/EventOutboxRetentionPropertiesBindingTest.java) | 실제 `application.yml`에서 24시간/30일, batch 500, 최대 20 batch 기본값을 바인딩한다. |
| `EventOutboxRetentionSchedulerTest` | [`event/adapter/in/scheduler/EventOutboxRetentionSchedulerTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/in/scheduler/EventOutboxRetentionSchedulerTest.java) | `event_outbox_payload_retention`의 성공·실패·처리 건수 metric과 예외 전파를 검증한다. |
| `EventOutboxRetentionPersistenceTest` | [`event/adapter/out/persistence/EventOutboxRetentionPersistenceTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxRetentionPersistenceTest.java) | validated CHECK, concurrent partial index, redaction·safe-error marker, 24시간/30일 경계, terminal-only redaction과 batch 제한을 실제 PostgreSQL에서 검증한다. |
| `EventOutboxRetentionLockIntegrationTest` | [`event/adapter/out/persistence/EventOutboxRetentionLockIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxRetentionLockIntegrationTest.java) | 다른 connection이 잠근 terminal row를 `FOR UPDATE SKIP LOCKED`로 기다리지 않고 건너뛰는지 검증한다. |
| `EventOutboxRetentionFlywayMigrationIntegrationTest` | [`event/adapter/out/persistence/EventOutboxRetentionFlywayMigrationIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxRetentionFlywayMigrationIntegrationTest.java) | 7월 14일 schema에 legacy row를 넣은 뒤 최신 Flyway 전체를 적용해 row 보존, validated CHECK, redaction column과 concurrent index를 검증한다. |
| `EventOutboxRetentionServiceTest` | [`event/application/service/EventOutboxRetentionServiceTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRetentionServiceTest.java) | PUBLISHED/FAILED batch 공정 처리, 조기 소진 종료와 실행당 최대 batch 수를 검증한다. |
| `EventOutboxRelayTemplateEmailTracingTest` | [`event/application/service/EventOutboxRelayTemplateEmailTracingTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayTemplateEmailTracingTest.java) | cause-bearing provider 실패가 cause-less `EMAIL-0005`로 relay error span에 도달하고 이메일·지원자 변수를 노출하지 않는지 검증한다. |
| `EventOutboxRelayTracingTest` | [`event/application/service/EventOutboxRelayTracingTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayTracingTest.java) | 저장된 traceparent가 있는 경우의 span link와 traceparent 미설정 경계를 검증한다. |
| `EventOutboxRelayNonTransactionalTest` | [`event/application/service/EventOutboxRelayNonTransactionalTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayNonTransactionalTest.java) | `relay_non_transactional_dispatch`, `relay_non_transactional_listener_failure`, `relay_retries_failed_listener_on_next_due_run`으로 transaction 밖 동기 listener, 실패 기록·재시도를 검증한다. |
| `EventOutboxRelayLeaseFencingTest` | [`event/application/service/EventOutboxRelayLeaseFencingTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayLeaseFencingTest.java) | PUBLISHED 저장 실패의 재시도 전환과 lease 소유권을 잃은 stale worker의 상태 덮어쓰기 차단을 검증한다. |
| `EventOutboxRelayJdbcIntegrationTest` | [`event/application/service/EventOutboxRelayJdbcIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayJdbcIntegrationTest.java) | `application_context_uses_single_outbox_publisher`, `non_transactional_listener_releases_jdbc_connection`, `transactional_listener_keeps_jdbc_connection`으로 publisher 단일 bean과 JDBC connection 경계를 검증한다. |
| `EventOutboxRelayClaimJdbcIntegrationTest` | [`event/application/service/EventOutboxRelayClaimJdbcIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayClaimJdbcIntegrationTest.java) | `claims_next_outbox_only_after_non_transactional_listener_completes`로 첫 non-transactional listener 완료 전 다음 row를 claim하지 않는지 실제 PostgreSQL에서 검증한다. |
| `EventOutboxRelayFailureSanitizationJdbcIntegrationTest` | [`event/application/service/EventOutboxRelayFailureSanitizationJdbcIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxRelayFailureSanitizationJdbcIntegrationTest.java) | `stores_stable_failure_codes_without_raw_pii`로 provider business failure의 `EMAIL-0005` 또는 일반 runtime failure의 예외 class만 `last_error`에 남기고 raw PII를 DB/log에 기록하지 않는지 실제 PostgreSQL에서 검증한다. |
| `EventOutboxStatusQueryServiceTest` | [`event/application/service/EventOutboxStatusQueryServiceTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxStatusQueryServiceTest.java) | event ID 상태 조회가 payload를 노출하지 않고 not-found stable code와 `readOnly` transaction을 유지하는지 검증한다. |
| `EventOutboxStatusIntegrationTest` | [`event/application/service/EventOutboxStatusIntegrationTest.java`](../../../src/test/java/com/umc/product/global/event/application/service/EventOutboxStatusIntegrationTest.java) | PENDING/PROCESSING/PUBLISHED/FAILED별 `nextAttemptAt`·`leaseUntil`·`publishedAt` 의미와 JSON payload/PII 비노출을 검증한다. |

## UseCase / Application Service

### CacheServiceTest
- 테스트 설명: CacheService
- 위치: `src/test/java/com/umc/product/global/cache/application/service/CacheServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/global/cache/application/service/CacheServiceTest.java#L12) | get, put, evict 요청을 저장소 포트로 위임한다 | 호출 put(spec, key, "auth"); 호출 get(spec, key); 호출 evict(CacheNamespace.GOOGLE_JWKS, key) | 실패: 예외 CacheLookup.Hit, CacheLookup.Miss; 검증 assertThat(hit).isInstanceOf(CacheLookup.Hit.class); assertThat(((CacheLookup.Hit<String>) hit).value()).isEqualTo("auth"); assertThat(miss).isInstanceOf(CacheLookup.Miss.class); |

## Repository / Outbound Persistence

### EventOutboxPersistenceAdapterTest
- 테스트 설명: EventOutboxPersistenceAdapter
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [16](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java#L16) | save는 repository save로 위임한다 | 조건 save는 repository save로 위임한다 | 성공: save는 repository save로 위임한다 |
| [34](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java#L34) | EventOutboxPersistenceAdapter / saveAll은 repository saveAll로 위임한다 | 조건 EventOutboxPersistenceAdapter / saveAll은 repository saveAll로 위임한다 | 성공: EventOutboxPersistenceAdapter / saveAll은 repository saveAll로 위임한다 |
| [45](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxPersistenceAdapterTest.java#L45) | EventOutboxPersistenceAdapter / listPublishable은 repository의 lock 조회로 위임한다 | 조건 EventOutboxPersistenceAdapter / listPublishable은 repository의 lock 조회로 위임한다 | 성공: 검증 assertThat(result).isSameAs(expected); |

### EventOutboxJpaRepositoryTest
- 테스트 설명: EventOutbox JPA polling 및 lease fencing
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [39](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java#L39) | 발행 가능한 이벤트를 다음 시도 시각 순서로 조회한다 | PENDING/PUBLISHED 행 혼합 | 발행 대상 PENDING 행만 시각 순서로 반환 |
| [55](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java#L55) | 발행 대기 partial index를 사용한다 | PostgreSQL index metadata | PENDING/PROCESSING partial index 존재, legacy index 제거 |
| [81](../../../src/test/java/com/umc/product/global/event/adapter/out/persistence/EventOutboxJpaRepositoryTest.java#L81) | lease 재획득 후 이전 worker의 상태 덮어쓰기를 차단한다 | 두 EntityManager가 같은 outbox version으로 시작 | stale merge에서 `OptimisticLockException` 발생 |

## E2E / Integration

### SecurityConfigIntegrationTest
- 테스트 설명: SecurityConfig 통합 테스트
- 위치: `src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L13) | docs 진입 경로는 Scalar HTML로 리다이렉트한다 | HTTP GET /docs | 성공: is3xxRedirection |
| [24](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L24) | SecurityConfig 통합 테스트 / 인증된 요청이어도 Swagger UI 경로는 접근할 수 없다 | HTTP GET /swagger-ui/index.html | 실패: HTTP 403 Forbidden |
| [38](../../../src/test/java/com/umc/product/global/config/SecurityConfigIntegrationTest.java#L38) | SecurityConfig 통합 테스트 / 인증된 요청이어도 기존 OpenAPI JSON 경로는 접근할 수 없다 | HTTP GET /v3/api-docs | 실패: HTTP 403 Forbidden |

## Scheduler

### EventOutboxPollerTest
- 테스트 설명: EventOutboxPoller
- 위치: `src/test/java/com/umc/product/global/event/adapter/in/scheduler/EventOutboxPollerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [8](../../../src/test/java/com/umc/product/global/event/adapter/in/scheduler/EventOutboxPollerTest.java#L8) | poll은 relay service를 호출한다 | 조건 poll은 relay service를 호출한다 | 성공: poll은 relay service를 호출한다 |

## Domain

### CacheKeyTest
- 테스트 설명: CacheKey
- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheKeyTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [7](../../../src/test/java/com/umc/product/global/cache/domain/CacheKeyTest.java#L7) | 문자열 값으로 cache key를 생성한다 | 조건 문자열 값으로 cache key를 생성한다 | 성공: 검증 assertThat(key.value()).isEqualTo("comment-1"); |
| [18](../../../src/test/java/com/umc/product/global/cache/domain/CacheKeyTest.java#L18) | CacheKey / 빈 cache key는 허용하지 않는다 | 조건 CacheKey / 빈 cache key는 허용하지 않는다 | 실패: 예외 IllegalArgumentException |

### CacheLookupTest
- 테스트 설명: CacheLookup
- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheLookupTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [6](../../../src/test/java/com/umc/product/global/cache/domain/CacheLookupTest.java#L6) | Hit은 값을 변환할 수 있다 | 조건 Hit은 값을 변환할 수 있다 | 실패: 예외 CacheLookup.Hit; 검증 assertThat(lookup.hit()).isTrue(); assertThat(lookup).isInstanceOf(CacheLookup.Hit.class); assertThat(((CacheLookup.Hit<Integer>) lookup).value()).isEqualTo(3); |
| [19](../../../src/test/java/com/umc/product/global/cache/domain/CacheLookupTest.java#L19) | CacheLookup / Miss는 변환해도 Miss로 유지된다 | 조건 CacheLookup / Miss는 변환해도 Miss로 유지된다 | 실패: 예외 CacheLookup.Miss; 검증 assertThat(lookup.hit()).isFalse(); assertThat(lookup).isInstanceOf(CacheLookup.Miss.class); |

### CacheNamespaceTest
- 테스트 설명: CacheNamespace
- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheNamespaceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [6](../../../src/test/java/com/umc/product/global/cache/domain/CacheNamespaceTest.java#L6) | cache namespace 값은 중복되지 않는다 | 조건 cache namespace 값은 중복되지 않는다 | 실패: cache namespace 값은 중복되지 않는다 |
| [15](../../../src/test/java/com/umc/product/global/cache/domain/CacheNamespaceTest.java#L15) | CacheNamespace / Google JWKS 캐시는 기존 Prometheus metric name을 유지한다 | 조건 CacheNamespace / Google JWKS 캐시는 기존 Prometheus metric name을 유지한다 | 성공: 검증 .isEqualTo("authentication.google.jwks.l1"); |

### CacheSpecTest
- 테스트 설명: CacheSpec
- 위치: `src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [8](../../../src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java#L8) | namespace, valueType, ttl, maximumSize로 cache spec을 생성한다 | 조건 namespace, valueType, ttl, maximumSize로 cache spec을 생성한다 | 성공: 검증 assertThat(spec.namespace()).isEqualTo(CacheNamespace.GOOGLE_JWKS); assertThat(spec.valueType()).isEqualTo(String.class); assertThat(spec.ttl()).isEqualTo(Duration.ofMinutes(5)); assertThat(spec.maximumSize()... |
| [27](../../../src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java#L27) | CacheSpec / ttl은 양수여야 한다 | 조건 CacheSpec / ttl은 양수여야 한다 | 실패: 예외 IllegalArgumentException |
| [40](../../../src/test/java/com/umc/product/global/cache/domain/CacheSpecTest.java#L40) | CacheSpec / maximumSize는 양수여야 한다 | 조건 CacheSpec / maximumSize는 양수여야 한다 | 실패: 예외 IllegalArgumentException |

### EventOutboxTest
- 테스트 설명: EventOutbox
- 위치: `src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [9](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L9) | 도메인 이벤트와 payload로 pending outbox를 기록한다 | 조건 도메인 이벤트와 payload로 pending outbox를 기록한다 | 성공: 검증 assertThat(outbox.getEventId()).isEqualTo(event.eventId()); assertThat(outbox.getEventType()).isEqualTo("test.created"); assertThat(outbox.getEventClass()).isEqualTo(TestEvent.class.getName()); assertThat(outbox.getPa... |
| [28](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L28) | EventOutbox / payload는 비어 있을 수 없다 | 조건 EventOutbox / payload는 비어 있을 수 없다 | 실패: 예외 IllegalArgumentException |
| [38](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L38) | EventOutbox / 발행 성공 시 published 상태와 시간을 기록한다 | 조건 EventOutbox / 발행 성공 시 published 상태와 시간을 기록한다 | 성공: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED); assertThat(outbox.getPublishedAt()).isNotNull(); |
| [49](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L49) | EventOutbox / 처리 시작 시 processing 상태와 lease 만료 시간을 기록한다 | 조건 EventOutbox / 처리 시작 시 processing 상태와 lease 만료 시간을 기록한다 | 실패: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PROCESSING); assertThat(outbox.getNextAttemptAt()).isEqualTo(leaseUntil); |
| [61](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L61) | EventOutbox / 발행 실패 시 attempts를 증가시키고 다음 시도 시간을 기록한다 | 조건 EventOutbox / 발행 실패 시 attempts를 증가시키고 다음 시도 시간을 기록한다 | 실패: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.PENDING); assertThat(outbox.getAttempts()).isEqualTo(1); assertThat(outbox.getNextAttemptAt()).isEqualTo(nextAttemptAt); assertThat(outbox.getLastError()).isE... |
| [75](../../../src/test/java/com/umc/product/global/event/domain/EventOutboxTest.java#L75) | 최대 시도 횟수에 도달하면 failed 상태로 전환한다 | 조건 최대 시도 횟수에 도달하면 failed 상태로 전환한다 | 성공: 검증 assertThat(outbox.getStatus()).isEqualTo(EventOutboxStatus.FAILED); assertThat(outbox.getAttempts()).isEqualTo(2); assertThat(outbox.getLastError()).isEqualTo("second"); |

## External Adapter

### CacheKeyFormatterTest
- 테스트 설명: CacheKeyFormatter
- 위치: `src/test/java/com/umc/product/global/cache/adapter/out/CacheKeyFormatterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [8](../../../src/test/java/com/umc/product/global/cache/adapter/out/CacheKeyFormatterTest.java#L8) | 환경, namespace, key를 조합해 최종 cache key를 만든다 | 조건 환경, namespace, key를 조합해 최종 cache key를 만든다 | 성공: 검증 assertThat(result).isEqualTo("umc:local:authentication.google.jwks:google"); |

### CaffeineCacheStoreAdapterTest
- 테스트 설명: CaffeineCacheStoreAdapter
- 위치: `src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [11](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L11) | 저장되지 않은 key는 Miss를 반환한다 | 조건 저장되지 않은 key는 Miss를 반환한다 | 실패: 예외 CacheLookup.Miss; 검증 assertThat(result).isInstanceOf(CacheLookup.Miss.class); |
| [25](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L25) | CaffeineCacheStoreAdapter / put한 값은 Hit로 조회된다 | 조건 CaffeineCacheStoreAdapter / put한 값은 Hit로 조회된다 | 실패: 예외 CacheLookup.Hit; 검증 assertThat(result).isInstanceOf(CacheLookup.Hit.class); assertThat(((CacheLookup.Hit<String>) result).value()).isEqualTo("auth"); |
| [38](../../../src/test/java/com/umc/product/global/cache/adapter/out/CaffeineCacheStoreAdapterTest.java#L38) | CaffeineCacheStoreAdapter / evict하면 다음 조회는 Miss가 된다 | 조건 CaffeineCacheStoreAdapter / evict하면 다음 조회는 Miss가 된다 | 실패: 예외 CacheLookup.Miss; 검증 assertThat(adapter.get(spec, key)).isInstanceOf(CacheLookup.Miss.class); |

### EventPayloadDeserializerTest
- 테스트 설명: EventPayloadDeserializer
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [13](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L13) | eventClass와 payload로 DomainEvent를 복원한다 | 조건 eventClass와 payload로 DomainEvent를 복원한다 | 실패: 예외 TestEvent; 검증 assertThat(result).isInstanceOf(TestEvent.class); assertThat(((TestEvent) result).message()).isEqualTo("hello"); |
| [31](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L31) | EventPayloadDeserializer / eventClass가 DomainEvent 타입이 아니면 예외를 던진다 | 조건 EventPayloadDeserializer / eventClass가 DomainEvent 타입이 아니면 예외를 던진다 | 실패: 예외 IllegalStateException |
| [44](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L44) | EventPayloadDeserializer / eventClass를 찾을 수 없으면 예외를 던진다 | 조건 EventPayloadDeserializer / eventClass를 찾을 수 없으면 예외를 던진다 | 실패: 예외 IllegalStateException |
| [57](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadDeserializerTest.java#L57) | EventPayloadDeserializer / payload JSON을 복원할 수 없으면 예외를 던진다 | 조건 EventPayloadDeserializer / payload JSON을 복원할 수 없으면 예외를 던진다 | 실패: 예외 IllegalStateException |

### EventPayloadSerializerTest
- 테스트 설명: EventPayloadSerializer
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [14](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java#L14) | 도메인 이벤트를 JSON payload로 직렬화한다 | 조건 도메인 이벤트를 JSON payload로 직렬화한다 | 성공: 검증 assertThat(payload).contains("\"eventType\":\"test.created\""); assertThat(payload).contains("\"message\":\"hello\""); |
| [29](../../../src/test/java/com/umc/product/global/event/adapter/out/EventPayloadSerializerTest.java#L29) | EventPayloadSerializer / 직렬화 실패 시 eventType을 포함한 예외를 던진다 | 조건 EventPayloadSerializer / 직렬화 실패 시 eventType을 포함한 예외를 던진다 | 실패: 예외 IllegalStateException |

### OutboxDomainEventPublisherTest
- 테스트 설명: OutboxDomainEventPublisher
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [15](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java#L15) | publish는 도메인 이벤트를 직발행하지 않고 event outbox로 저장한다 | 조건 publish는 도메인 이벤트를 직발행하지 않고 event outbox로 저장한다 | 성공: 검증 assertThat(savePort.saved).hasSize(1); assertThat(outbox.getEventId()).isEqualTo(event.eventId()); assertThat(outbox.getEventType()).isEqualTo("test.created"); assertThat(outbox.getPayload()).contains("\"message\":\"h... |
| [37](../../../src/test/java/com/umc/product/global/event/adapter/out/OutboxDomainEventPublisherTest.java#L37) | OutboxDomainEventPublisher / publishAll은 입력 순서대로 모든 이벤트를 일괄 저장한다 | 조건 OutboxDomainEventPublisher / publishAll은 입력 순서대로 모든 이벤트를 일괄 저장한다 | 성공: 검증 assertThat(savePort.saved); .containsExactly(first.eventId(), second.eventId()); assertThat(savePort.saveAllCalled).isTrue(); |

### EventOutboxPublisherConfigurationTest
- 위치: `src/test/java/com/umc/product/global/event/adapter/out/EventOutboxPublisherConfigurationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [27](../../../src/test/java/com/umc/product/global/event/adapter/out/EventOutboxPublisherConfigurationTest.java#L27) | 과거 비활성화 property가 있어도 outbox publisher와 relay poller를 사용한다 | `app.event-outbox.enabled=false` | `OutboxDomainEventPublisher`와 `EventOutboxPoller` 단일 bean 등록 |
| [40](../../../src/test/java/com/umc/product/global/event/adapter/out/EventOutboxPublisherConfigurationTest.java#L40) | relay를 중지해도 outbox publisher는 유지한다 | `app.event-outbox.relay-enabled=false` | `OutboxDomainEventPublisher`는 등록하고 `EventOutboxPoller`는 등록하지 않음 |

## Support / Config / Utility

### ApiAccessDeniedHandlerTest
- 위치: `src/test/java/com/umc/product/global/security/ApiAccessDeniedHandlerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [18](../../../src/test/java/com/umc/product/global/security/ApiAccessDeniedHandlerTest.java#L18) | 인가 실패 응답은 내부 AccessDeniedException 메시지를 노출하지 않는다 | 조건 인가 실패 응답은 내부 AccessDeniedException 메시지를 노출하지 않는다 | 실패: 에러코드 CommonErrorCode.FORBIDDEN; 검증 assertThat(response.getStatus()).isEqualTo(403); assertThat(response.getContentAsString()); .contains("\"success\":false"); .contains("\"code\":\"" + CommonErrorCode.FORBIDDEN.getCode() + "\"") |

### CustomErrorControllerTest
- 위치: `src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [19](../../../src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java#L19) | fallback error controller도 BusinessException 상세 메시지를 유지한다 | 조건 fallback error controller도 BusinessException 상세 메시지를 유지한다 | 성공: 에러코드 AuthorizationErrorCode.PERMISSION_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.PERMISSION_DENIED.getHttpStatus()); assertThat(response.getBody()).satisfies(body -> {; assertThat(body.getCode()).isEqualTo(AuthorizationErrorCode.P... |
| [38](../../../src/test/java/com/umc/product/global/exception/CustomErrorControllerTest.java#L38) | fallback error controller도 RESOURCE_ACCESS_DENIED 기본 메시지를 detail로 내려준다 | 조건 fallback error controller도 RESOURCE_ACCESS_DENIED 기본 메시지를 detail로 내려준다 | 성공: 에러코드 AuthorizationErrorCode.RESOURCE_ACCESS_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getHttpStatus()); assertThat(response.getBody()).satisfies(body -> {; assertThat(body.getCode()).isEqualTo(AuthorizationErrorC... |

### EmailMaskerTest
- 테스트 설명: EmailMasker — 이메일 마스킹 유틸
- 위치: `src/test/java/com/umc/product/global/util/EmailMaskerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [6](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L6) | EmailMasker — 이메일 마스킹 유틸 | 조건 EmailMasker — 이메일 마스킹 유틸 | 성공: 검증 assertThat(EmailMasker.mask(null)).isNull(); |
| [14](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L14) | EmailMasker — 이메일 마스킹 유틸 / 빈 문자열은 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 빈 문자열은 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("")).isEqualTo(""); assertThat(EmailMasker.mask(" ")).isEqualTo(" "); |
| [20](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L20) | EmailMasker — 이메일 마스킹 유틸 / 골뱅이가 없는 입력은 원문 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 골뱅이가 없는 입력은 원문 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("notAnEmail")).isEqualTo("notAnEmail"); |
| [26](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L26) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 비어있으면 원문 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 비어있으면 원문 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("@domain.com")).isEqualTo("@domain.com"); |
| [31](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L31) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 1은 그대로 반환한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 1은 그대로 반환한다 | 성공: 검증 assertThat(EmailMasker.mask("a@umc.com")).isEqualTo("a@umc.com"); |
| [36](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L36) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 2는 앞 1글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 2는 앞 1글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("ab@umc.com")).isEqualTo("a*@umc.com"); |
| [41](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L41) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 3은 앞 1글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 3은 앞 1글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("abc@umc.com")).isEqualTo("a**@umc.com"); |
| [46](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L46) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 4는 앞 3글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트 길이 4는 앞 3글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("abcd@umc.com")).isEqualTo("abc*@umc.com"); |
| [51](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L51) | EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 긴 경우 앞 3글자만 남기고 마스킹한다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 로컬 파트가 긴 경우 앞 3글자만 남기고 마스킹한다 | 성공: 검증 assertThat(EmailMasker.mask("donggukcd200@gmail.com")); .isEqualTo("don*********@gmail.com"); |
| [57](../../../src/test/java/com/umc/product/global/util/EmailMaskerTest.java#L57) | EmailMasker — 이메일 마스킹 유틸 / 도메인은 절대 마스킹되지 않는다 | 조건 EmailMasker — 이메일 마스킹 유틸 / 도메인은 절대 마스킹되지 않는다 | 성공: 검증 assertThat(masked).endsWith("@hanyang.ac.kr"); |

### ExternalApiCallLoggerTest
- 위치: `src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [45](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L45) | 성공 호출은 INFO + result=SUCCESS + durationMs 로 기록되고 반환값을 그대로 돌려준다 | 조건 성공 호출은 INFO + result=SUCCESS + durationMs 로 기록되고 반환값을 그대로 돌려준다 | 성공: 검증 assertThat(result).isEqualTo("pr-list"); assertThat(event.getLevel()).isEqualTo(Level.INFO); assertThat(event.getMessage()).isEqualTo("external_api_called"); assertThat(kvOf(event, "provider")).isEqualTo("GITHUB"); |
| [67](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L67) | RuntimeException 발생 시 WARN + result=FAILURE + errorClass 가 기록되고 예외는 재던져진다 | 조건 RuntimeException 발생 시 WARN + result=FAILURE + errorClass 가 기록되고 예외는 재던져진다 | 실패: 예외 발생; 검증 assertThat(event.getLevel()).isEqualTo(Level.WARN); assertThat(event.getMessage()).isEqualTo("external_api_called"); assertThat(kvOf(event, "provider")).isEqualTo("OPENAI"); assertThat(kvOf(event, "operation")).isEqua... |
| [94](../../../src/test/java/com/umc/product/global/logging/ExternalApiCallLoggerTest.java#L94) | Runnable 오버로드도 동일한 이벤트 스키마로 기록한다 | 조건 Runnable 오버로드도 동일한 이벤트 스키마로 기록한다 | 성공: 검증 assertThat(event.getLevel()).isEqualTo(Level.INFO); assertThat(kvOf(event, "provider")).isEqualTo("APPLE"); assertThat(kvOf(event, "operation")).isEqualTo("EXCHANGE_TOKEN"); assertThat(kvOf(event, "result")).isEqualTo... |

### GlobalExceptionHandlerTest
- 위치: `src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [33](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L33) | RESOURCE_ACCESS_DENIED 기본 예외도 message가 null로 내려가지 않는다 | 조건 RESOURCE_ACCESS_DENIED 기본 예외도 message가 null로 내려가지 않는다 | 실패: 에러코드 AuthorizationErrorCode.RESOURCE_ACCESS_DENIED; 검증 assertThat(response.getStatusCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_DENIED.getHttpStatus()); assertThat(response.getBody()); assertThat(body.getCode()).isEqualTo(AuthorizationErrorCode.RESOURCE_ACCESS_... |
| [53](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L53) | Spring Security AccessDeniedException은 MVC 경로에서도 403으로 응답한다 | HTTP GET /access-denied | 실패: HTTP 403 Forbidden; 에러코드 CommonErrorCode.FORBIDDEN |
| [70](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L70) | JSON 파싱 오류는 내부 파서 상세 메시지를 응답에 노출하지 않는다 | HTTP POST /body | 실패: HTTP 400 Bad Request; 검증 assertThat(result.getResponse().getContentAsString()) |
| [94](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L94) | 요청 본문이 없으면 사용자가 이해할 수 있는 다음 행동을 안내한다 | HTTP POST /body | 실패: HTTP 400 Bad Request; 에러코드 CommonErrorCode.BAD_REQUEST |
| [111](../../../src/test/java/com/umc/product/global/exception/GlobalExceptionHandlerTest.java#L111) | 요청 값 형식이 맞지 않으면 사용자가 확인할 값을 안내한다 | HTTP GET /number; param value="not-number" | 실패: HTTP 400 Bad Request; 에러코드 CommonErrorCode.BAD_REQUEST |

### JwtTokenProviderEmailVerificationTest
- 위치: `src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [44](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L44) | REGISTER 로 발급한 토큰은 REGISTER 로 파싱 시 이메일을 반환한다 | 조건 REGISTER 로 발급한 토큰은 REGISTER 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [57](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L57) | PASSWORD_RESET 로 발급한 토큰은 PASSWORD_RESET 로 파싱 시 이메일을 반환한다 | 조건 PASSWORD_RESET 로 발급한 토큰은 PASSWORD_RESET 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [70](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L70) | CHANGE_EMAIL 로 발급한 토큰은 CHANGE_EMAIL 로 파싱 시 이메일을 반환한다 | 조건 CHANGE_EMAIL 로 발급한 토큰은 CHANGE_EMAIL 로 파싱 시 이메일을 반환한다 | 성공: 검증 assertThat(parsedEmail).isEqualTo(EMAIL); |
| [83](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L83) | REGISTER 토큰을 PASSWORD_RESET 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 REGISTER 토큰을 PASSWORD_RESET 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [97](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L97) | PASSWORD_RESET 토큰을 REGISTER 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 PASSWORD_RESET 토큰을 REGISTER 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [111](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L111) | REGISTER 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 REGISTER 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [125](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L125) | PASSWORD_RESET 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 조건 PASSWORD_RESET 토큰을 CHANGE_EMAIL 로 파싱하면 INVALID_EMAIL_VERIFICATION 예외를 던진다 | 실패: 예외 AuthenticationDomainException; 에러코드 AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION; 검증 .isEqualTo(AuthenticationErrorCode.INVALID_EMAIL_VERIFICATION); |
| [139](../../../src/test/java/com/umc/product/global/security/JwtTokenProviderEmailVerificationTest.java#L139) | RefreshToken 발급 시 jti와 만료시각을 포함하고 파싱 결과로 반환한다 | 조건 RefreshToken 발급 시 jti와 만료시각을 포함하고 파싱 결과로 반환한다 | 실패: 검증 assertThat(claims.memberId()).isEqualTo(memberId); assertThat(claims.jti()).isNotNull(); assertThat(claims.expiresAt()).isNotNull(); |

### LoggingInterceptorTest
- 위치: `src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [57](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L57) | preHandle 시 method / path 가 MDC 에 들어가고 traceId 가 있으면 X-Trace-Id 헤더가 채워진다 | HTTP PUT traceId; HTTP GET method; HTTP GET path; HTTP GET requestId | 성공: 검증 assertThat(result).isTrue(); assertThat(MDC.get("method")).isEqualTo("GET"); assertThat(MDC.get("path")).isEqualTo("/forms/123/answers"); assertThat(response.getHeader("X-Trace-Id")).isEqualTo("test-trace-abc123"); |
| [78](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L78) | traceId 가 없으면 X-Trace-Id 헤더도 비어 있어야 한다 | 조건 traceId 가 없으면 X-Trace-Id 헤더도 비어 있어야 한다 | 성공: 검증 assertThat(response.getHeader("X-Trace-Id")).isNull(); |
| [92](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L92) | afterCompletion 의 finally 에서 MDC 가 반드시 비워진다 | HTTP GET method | 성공: 검증 assertThat(MDC.get("method")).isEqualTo("GET"); assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); |
| [110](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L110) | api_request_completed 로그의 MDC 스냅샷에 uriTemplate / statusCode / durationMs 가 포함된다 | 조건 api_request_completed 로그의 MDC 스냅샷에 uriTemplate / statusCode / durationMs 가 포함된다 | 성공: 검증 assertThat(snapshot).isNotNull(); assertThat(snapshot).containsEntry("event", "api_request_completed"); assertThat(snapshot).containsEntry("uriTemplate", "/forms/{formId}/answers"); assertThat(snapshot).containsEntry(... |
| [136](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L136) | preHandle 이 호출되지 않은 상태에서 afterCompletion 이 호출되어도 안전하게 종료된다 | 조건 preHandle 이 호출되지 않은 상태에서 afterCompletion 이 호출되어도 안전하게 종료된다 | 성공: 검증 assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); |
| [148](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L148) | 인증된 MemberPrincipal 의 clientType 이 없으면 MDC clientType 을 UNKNOWN 으로 채운다 | HTTP GET memberId; HTTP GET clientType; HTTP GET userId | 성공: 검증 assertThat(MDC.get("memberId")).isEqualTo("42"); assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN"); assertThat(MDC.get("userId")).isNull(); |
| [171](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L171) | 인증된 MemberPrincipal 의 clientType 이 있으면 해당 값을 MDC 에 채운다 | HTTP GET memberId; HTTP GET clientType | 성공: 검증 assertThat(MDC.get("memberId")).isEqualTo("42"); assertThat(MDC.get("clientType")).isEqualTo("IOS"); |
| [192](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L192) | 익명 사용자는 MDC memberId 가 채워지지 않는다 | HTTP GET memberId; HTTP GET clientType | 성공: 검증 assertThat(MDC.get("memberId")).isNull(); assertThat(MDC.get("clientType")).isEqualTo("UNKNOWN"); |
| [207](../../../src/test/java/com/umc/product/global/config/LoggingInterceptorTest.java#L207) | X-Forwarded-For 헤더가 있으면 첫 번째 IP 가 clientIp 로 채워진다 | 조건 X-Forwarded-For 헤더가 있으면 첫 번째 IP 가 clientIp 로 채워진다 | 성공: 검증 assertThat(snapshot).isNotNull(); assertThat(snapshot).containsEntry("clientIp", "203.0.113.7"); |

### PasswordEncoderConfigTest
- 위치: `src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [37](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L37) | 기본 인코딩은 {argon2} prefix 가 붙는다 | 조건 기본 인코딩은 {argon2} prefix 가 붙는다 | 성공: 검증 assertThat(encoded).startsWith("{argon2}"); |
| [47](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L47) | argon2 로 인코딩된 해시는 같은 평문에 대해 matches=true 를 반환한다 | 조건 argon2 로 인코딩된 해시는 같은 평문에 대해 matches=true 를 반환한다 | 성공: 검증 assertThat(matches).isTrue(); |
| [60](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L60) | argon2 해시에 대해 다른 평문은 matches=false 를 반환한다 | 조건 argon2 해시에 대해 다른 평문은 matches=false 를 반환한다 | 성공: 검증 assertThat(encoder.matches("Wrong-Pw-2026", encoded)).isFalse(); |
| [70](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L70) | bcrypt prefix 가 붙은 기존 해시도 검증할 수 있다 | 조건 bcrypt prefix 가 붙은 기존 해시도 검증할 수 있다 | 성공: 검증 assertThat(matches).isTrue(); |
| [83](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L83) | 기본(argon2) 으로 인코딩된 해시는 upgradeEncoding=false | 조건 기본(argon2) 으로 인코딩된 해시는 upgradeEncoding=false | 성공: 검증 assertThat(needsUpgrade).isFalse(); |
| [96](../../../src/test/java/com/umc/product/global/config/PasswordEncoderConfigTest.java#L96) | 기본이 아닌 알고리즘(bcrypt) 의 해시는 upgradeEncoding=true 로 점진적 rehash 대상이 된다 | 조건 기본이 아닌 알고리즘(bcrypt) 의 해시는 upgradeEncoding=true 로 점진적 rehash 대상이 된다 | 성공: 검증 assertThat(needsUpgrade).isTrue(); |

### QueryStatsJdbcEventListenerTest
- 위치: `src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [44](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L44) | DB 쿼리 실행을 child span으로 남기고 요청 단위 쿼리 통계를 기록한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 12_500_000L, null) | 성공: 검증 assertThat(QueryStatsHolder.getQueryCount()).isEqualTo(1L); assertThat(QueryStatsHolder.getTotalTimeMs()).isEqualTo(12L); |
| [65](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L65) | SQL 앞에 주석이 있어도 실제 DB operation을 기록한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 1_000_000L, null) | 성공: SQL 앞에 주석이 있어도 실제 DB operation을 기록한다 |
| [77](../../../src/test/java/com/umc/product/global/config/QueryStatsJdbcEventListenerTest.java#L77) | DB 쿼리 실패 시 span에 예외를 기록하고 요청 통계에는 성공 쿼리만 반영한다 | 호출 onBeforeExecuteQuery(info); 호출 onAfterExecuteQuery(info, 3_000_000L, exception) | 실패: 검증 assertThat(QueryStatsHolder.getQueryCount()).isZero(); |

### SecurityPathConfigTest
- 위치: `src/test/java/com/umc/product/global/config/SecurityPathConfigTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/global/config/SecurityPathConfigTest.java#L12) | 문서 공개 경로는 Scalar와 문서 카탈로그에 필요한 경로만 포함한다 | 조건 문서 공개 경로는 Scalar와 문서 카탈로그에 필요한 경로만 포함한다 | 성공: 검증 assertThat(SecurityPathConfig.DOCUMENTATION_PATHS); .contains( |
| [35](../../../src/test/java/com/umc/product/global/config/SecurityPathConfigTest.java#L35) | Swagger 경로는 인증 여부와 무관하게 차단 대상이다 | 조건 Swagger 경로는 인증 여부와 무관하게 차단 대상이다 | 성공: 검증 assertThat(SecurityPathConfig.SWAGGER_BLOCKED_PATHS); .contains( |
| [56](../../../src/test/java/com/umc/product/global/config/SecurityPathConfigTest.java#L56) | Springdoc은 Swagger UI를 끄고 Scalar가 사용할 OpenAPI JSON만 제공한다 | 조건 Springdoc은 Swagger UI를 끄고 Scalar가 사용할 OpenAPI JSON만 제공한다 | 성공: 검증 assertThat(properties); .containsEntry("springdoc.swagger-ui.enabled", Boolean.FALSE); .containsEntry("springdoc.api-docs.path", "/docs-json"); .containsEntry("springdoc.api-docs.enabled", "${OPENAPI_ENABLE:${SWAGGER_ENA... |

### TraceFlowAspectTest
- 위치: `src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [40](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L40) | UseCase 구현체 호출을 UseCase 이름의 span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("result"); |
| [58](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L58) | adapter.out 호출을 adapter span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("entity"); |
| [75](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L75) | 동일한 target class와 method의 trace metadata를 캐시한다 | 호출 traceUseCaseAndAdapter(joinPoint(method, target, "first")); 호출 traceUseCaseAndAdapter(joinPoint(method, target, "second")) | 성공: 검증 assertThat(metadataCache).hasSize(1); |
| [91](../../../src/test/java/com/umc/product/global/observability/TraceFlowAspectTest.java#L91) | 상위 클래스가 구현한 UseCase interface도 UseCase span으로 감싼다 | 호출 traceUseCaseAndAdapter(joinPoint) | 성공: 검증 assertThat(result).isEqualTo("result"); |
