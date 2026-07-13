# Global Cache, Local Pub/Sub, Event Outbox 실행 계획

> 작성일: 2026-05-21
> 기준 브랜치: `origin/develop`
> 목표 PR 수: 2개

## 목표

1. 전역 `CacheUseCase`와 `CacheStorePort`를 도입해 현재 Caffeine 단일 인스턴스 캐시를 추후 Redis 등으로 교체 가능한 adapter 구조로 만든다.
2. Figma 댓글 분류 캐시는 전역 캐시를 직접 쓰지 않고 `FigmaClassificationCachePort` wrapper를 둔다. positive/negative cache, TTL, key schema, metric 이름 같은 도메인 정책을 wrapper가 소유하게 한다.
3. Spring `ApplicationEventPublisher` 기반 local pub/sub는 Redis Pub/Sub, Redis Streams, Kafka 도입 전 테스트 가능한 adapter로 유지하고 commit/rollback 동작을 검증한다.
4. 범용 transactional event outbox를 feature flag로 도입해 broker 도입 전에도 이벤트 손실 위험을 줄인다.

## 설계 결정

### Cache

전역 캐시는 공통 기술 기능만 책임진다.

- `CacheUseCase`: application layer에서 접근하는 inbound port.
- `CacheStorePort`: Caffeine, Redis 등 실제 저장소를 숨기는 outbound port.
- `CacheNamespace`: namespace 값과 metric 이름을 중앙 등록한다.
- `CacheSpec`: namespace, value type, TTL, maximum size를 함께 가진다.

도메인 의미가 있는 캐시는 wrapper를 둔다.

```text
FigmaCommentDomainClassifier
  -> FigmaClassificationCachePort
      -> FigmaClassificationCacheAdapter
          -> CacheUseCase
              -> CacheStorePort
                  -> CaffeineCacheStoreAdapter
```

이 구조를 택한 이유는 `figma.classification` 캐시가 단순 key-value가 아니라 positive/negative cache 구분, TTL, 최대 크기, key schema, `ClassificationCacheValue` 직렬화 정책을 가진 도메인 정책이기 때문이다. 반대로 도메인 정책이 없는 순수 기술 캐시는 `CacheUseCase`를 직접 사용할 수 있다.

### Local Pub/Sub

`SpringDomainEventPublisher`는 기본값으로 유지한다.

- `app.event-outbox.enabled=false` 또는 미설정이면 Spring local event bus를 사용한다.
- 같은 JVM 안의 listener fan-out을 검증할 수 있다.
- `@TransactionalEventListener(AFTER_COMMIT)`의 commit/rollback 동작을 테스트할 수 있다.
- durable delivery, multi-instance fan-out, ack/retry는 제공하지 않는다.

### Outbox

`app.event-outbox.enabled=true`일 때만 `OutboxDomainEventPublisher`가 활성화된다.

```text
CommandService @Transactional
  -> DomainEventPublisher.publish(event)
      -> OutboxDomainEventPublisher
          -> event_outbox INSERT

EventOutboxPoller
  -> PENDING / lease 만료 PROCESSING row를 짧은 transaction에서 claim
  -> PROCESSING 상태와 lease deadline 저장
  -> event_class + payload로 DomainEvent 역직렬화
  -> 별도 dispatch transaction에서 ApplicationEventPublisher.publishEvent(event)
  -> 별도 status transaction에서 PUBLISHED / retry / FAILED 상태 전환
```

현재 `PUBLISHED`는 “local Spring event bus dispatch transaction이 예외 없이 commit됨”을 의미한다. 기존 listener가 `@TransactionalEventListener(AFTER_COMMIT)`이면 listener의 실제 side effect는 dispatch transaction commit 이후에 실행된다. downstream side effect까지 outbox retry 범위에 넣으려면 listener를 동기 `@EventListener` 또는 broker consumer 형태로 별도 전환해야 한다.

## PR 1: 전역 캐시와 local pub/sub 검증

PR title: `[Refactor] 전역 캐시 유즈케이스와 local pub/sub 검증 추가`

이 PR은 DB migration 없이 내부 구조와 테스트만 바꾼다.

### Commit 1

`feat: add global cache contracts`

- `global/cache/domain/CacheNamespace`
- `global/cache/domain/CacheKey`
- `global/cache/domain/CacheSpec`
- `global/cache/domain/CacheLookup`
- `global/cache/application/port/in/CacheUseCase`
- `global/cache/application/port/out/CacheStorePort`
- namespace/key/spec/lookup 단위 테스트

### Commit 2

`feat: add caffeine cache store adapter`

- `global/cache/application/service/CacheService`
- `global/cache/adapter/out/CacheKeyFormatter`
- `global/cache/adapter/out/CaffeineCacheStoreAdapter`
- Caffeine adapter는 namespace별 cache를 lazily 생성하고 `CacheSpec.maximumSize()`와 `CacheSpec.ttl()`을 따른다.

### Commit 3

`refactor: route figma classification cache through global cache`

- `FigmaClassificationCachePort`
- `ClassificationCacheValue`
- `FigmaClassificationCacheAdapter`
- `FigmaCommentDomainClassifier`의 Caffeine 직접 의존 제거
- 기존 `figma.classifier.l1.*` metric 이름은 `CacheNamespace.FIGMA_CLASSIFICATION.metricName()`으로 유지

### Commit 4

`test: cover local domain event pubsub behavior`

- `SpringDomainEventPublisherIntegrationTest`
- 트랜잭션 commit 후 fan-out 검증
- rollback 시 `@TransactionalEventListener(AFTER_COMMIT)`가 실행되지 않음을 검증

## PR 2: 범용 이벤트 outbox 도입

PR title: `[Feat] 범용 이벤트 outbox 릴레이 도입`

이 PR은 Flyway migration과 feature flag 기반 publisher 전환을 포함한다.

### Commit 5

`feat: add event outbox persistence model`

- `V2026.05.21.01.00__create_event_outbox.sql`
- `EventOutbox`
- `EventOutboxStatus`
- `event_class varchar(300)` 컬럼 포함
- `EventOutbox.record`, `markPublished`, `recordFailure` 단위 테스트

### Commit 6

`feat: add outbox domain event publisher`

- `SaveEventOutboxPort`
- `LoadEventOutboxPort`
- `EventPayloadSerializer`
- `OutboxDomainEventPublisher`
- `EventOutboxJpaRepository`
- `EventOutboxPersistenceAdapter`
- `SpringDomainEventPublisher` 조건부 활성화

### Commit 7

`feat: add event outbox relay poller`

- `EventPayloadDeserializer`
- `EventOutboxRelayService`
- `EventOutboxPoller`
- `app.event-outbox.*` 설정 추가
- `PROCESSING` claim, relay 성공/실패/최대 재시도 단위 테스트

### Commit 8

`test: verify email event outbox flow`

- `SendVerificationEmailOutboxFlowTest`
- 이메일 인증 세션 생성 시 `SendVerificationEmailEvent`가 outbox row로 기록되는지 검증

### Commit 9

`docs: document global cache and event outbox rollout`

- Figma LLM 캐시 구조 문서 갱신
- ADR-019 구현 세부사항 보정
- ADR-021 전역 캐시 유즈케이스 결정 기록 추가

## 검증 명령

커밋별 targeted test를 통과시킨 뒤 마지막에 전체 테스트를 실행한다.

```bash
./gradlew test
```

## 후속 작업

- event outbox cleanup scheduler와 Prometheus metric 추가
- FAILED row 운영 플레이북 추가
- audit event의 outbox 전환 여부 검토
- Redis cache adapter 도입 시 `CacheStorePort` 구현체만 교체하고, 도메인 wrapper는 유지
- 외부 broker 도입 시 outbox relay를 Redis Streams/Kafka/Debezium relay로 교체
> 이 문서는 구현 당시 계획의 기록이다. publisher 선택 feature flag는
> [ADR-026](../../adr/026-enforce-event-outbox-publisher.md)으로 대체되었다.
