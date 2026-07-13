# Event Outbox 및 Cache Adapter 초기 검토 계획

> 이 문서는 구현 당시 계획의 기록이다. Spring local publisher 결정은
> [ADR-026](../../adr/026-enforce-event-outbox-publisher.md)으로 대체되었다.

> 작성일: 2026-05-21
> 상태: superseded

이 문서는 event pub/sub, outbox, cache adapter의 최초 진단과 실행 방향을 정리한 초안이다. 실제 구현 계획과 커밋 분할은 [2026-05-21-global-cache-local-pubsub-outbox.md](./2026-05-21-global-cache-local-pubsub-outbox.md)를 기준으로 한다.

## 최초 판정

### Event Pub/Sub 및 Broker Adapter

현재 구조는 내부 pub/sub adapter는 기본 틀이 있으나 broker adapter 구성은 되어 있지 않았다.

- `DomainEventPublisher`가 outbound port로 정의되어 application/service가 Spring `ApplicationEventPublisher`에 직접 의존하지 않는 구조는 이미 있었다.
- `SpringDomainEventPublisher`는 같은 JVM 안에서 동작하는 in-process adapter이다.
- Redis Pub/Sub, Redis Streams, Kafka/RabbitMQ 같은 외부 broker adapter, routing abstraction, broker message 직렬화 정책은 없었다.

### Outbox Pattern

범용 event outbox pattern은 적용되어 있지 않았다.

- `fcm_outbox` 테이블과 `FcmOutbox` aggregate는 있었지만 FCM 토픽 subscribe/unsubscribe 전용 outbox였다.
- `DomainEvent` 발행 자체는 DB outbox에 기록되지 않고 Spring event bus로 바로 publish되었다.
- 프로세스 종료, listener 예외, 다중 인스턴스 환경에서의 재처리/전달 보장을 범용 outbox로 해결하지 못했다.

### Cache Adapter

Caffeine은 adapter 구조가 아니라 Figma 서비스 내부 구현 세부사항으로 직접 결합되어 있었다.

- `FigmaCommentDomainClassifier`가 Caffeine cache를 직접 생성하고 조회/저장을 수행했다.
- metric config도 classifier 내부 cache instance를 꺼내 등록했다.
- Redis 등으로 전환하려면 classifier와 metric config를 함께 수정해야 했다.

## 실행 방향

1. 전역 `CacheUseCase`, `CacheStorePort`, `CacheNamespace`, `CacheSpec`를 추가한다.
2. Figma classification cache는 전역 cache usecase를 직접 쓰지 않고 `FigmaClassificationCachePort` wrapper를 통해 사용한다.
3. `SpringDomainEventPublisher` local pub/sub의 fan-out 및 transaction commit/rollback 동작을 테스트로 고정한다.
4. ADR-019에 맞춰 `event_outbox` 테이블, `OutboxDomainEventPublisher`, relay poller를 feature flag 뒤에 추가한다.
5. 작업은 cache/local pubsub PR과 outbox PR 두 개로 나눈다.
