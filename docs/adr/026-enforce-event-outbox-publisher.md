# ADR-026: Domain Event 발행을 Event Outbox로 고정한다

## Status

Proposed

[ADR-019](./019-introduce-transactional-event-outbox.md)의 publisher 선택 feature flag와
Spring local publisher rollback 정책을 대체한다.

## Context

2026년 7월 기준 `DomainEventPublisher`는 `app.event-outbox.enabled` 값에 따라
`OutboxDomainEventPublisher` 또는 `SpringDomainEventPublisher`를 선택한다. 기본값은 `true`지만
운영 설정 한 줄로 이벤트 전달 보장이 영속 outbox에서 JVM 내부 동기 호출로 바뀔 수 있다.

Spring local publisher가 선택되면 일반 `@EventListener`는 발행 스레드에서 즉시 실행된다. 외부 I/O
listener는 비즈니스 트랜잭션과 지연 및 실패를 공유하고, 프로세스 종료 후 복구와 공용 재시도를
제공하지 않는다. 반면 API와 application service는 동일한 `DomainEventPublisher`를 호출하므로 설정
차이가 호출부에서 드러나지 않는다.

FCM처럼 외부 네트워크 호출의 실패를 outbox 재시도로 연결해야 하는 이벤트는 listener 예외를 relay가
동기적으로 관찰해야 한다. 동시에 네트워크 대기 중 JDBC connection을 점유해서는 안 된다.

## Decision

우리는 모든 `DomainEventPublisher` 발행을 공용 `event_outbox` 영속화로 고정하기로 결정한다.

1. `OutboxDomainEventPublisher`와 `EventOutboxPoller`를 조건 없이 등록한다.
2. `SpringDomainEventPublisher`와 `app.event-outbox.enabled` 설정을 제거한다.
3. Spring `ApplicationEventPublisher`는 최초 발행 수단이 아니라 relay 내부의 local dispatch bus로만 사용한다.
4. 기본 이벤트는 relay 트랜잭션에서 dispatch한다.
5. 외부 I/O 이벤트는 `OutboxDispatchMode.NON_TRANSACTIONAL`을 선택해 listener를 트랜잭션 밖에서
   동기 실행하고, 성공 상태와 실패 상태는 각각 짧은 별도 트랜잭션으로 기록한다.

### 단계적 진행 / PR 분할

- **Phase 1**: 공용 event outbox 고정과 `NON_TRANSACTIONAL` dispatch 계약을 도입한다.
- **Phase 2**: FCM 요청과 batch 이벤트가 `NON_TRANSACTIONAL` 계약을 사용하도록 적용한다.
- **Phase 3**: 외부 broker 도입 시 outbox 저장 계약은 유지하고 relay 목적지만 교체한다.

## Alternatives Considered

### 대안 A: Feature flag 유지

장점:

- 장애 시 Spring local publisher로 즉시 전환할 수 있다.

단점:

- 설정에 따라 이벤트의 내구성, 재시도, 트랜잭션 동작이 달라진다.
- rollback이 기능 복구가 아니라 전달 보장 약화로 이어진다.

선택하지 않은 이유:

- outbox 장애는 poller 중지와 적체 관찰로 격리해야 하며, 이벤트 유실 가능 경로를 운영 옵션으로
  유지해서는 안 된다.

### 대안 B: Spring event와 `@Async`만 사용

장점:

- 구현과 운영 구성이 단순하다.

단점:

- executor queue가 JVM 메모리에만 존재해 재시작 시 복구되지 않는다.
- 공용 재시도와 처리 상태 추적을 제공하지 않는다.

선택하지 않은 이유:

- 비동기 실행은 제공하지만 durable delivery 요구사항을 충족하지 않는다.

### 대안 C: 모든 listener를 relay 트랜잭션 안에서 실행

장점:

- listener dispatch와 `PUBLISHED` 저장을 하나의 DB 트랜잭션으로 묶을 수 있다.

단점:

- Firebase, SMTP 같은 외부 호출 중 JDBC connection을 점유한다.
- 외부 시스템 지연이 DB connection pool과 relay 처리량에 영향을 준다.

선택하지 않은 이유:

- DB 작업과 외부 I/O의 실행 경계를 이벤트별로 분리할 필요가 있다.

## Consequences

### Positive

- 모든 환경에서 domain event가 같은 내구성과 재시도 의미를 가진다.
- 비즈니스 데이터와 event outbox 저장을 같은 트랜잭션에 포함할 수 있다.
- 외부 I/O 중 DB connection 점유를 피하면서 listener 실패를 relay 재시도로 연결할 수 있다.

### Negative

- outbox DB와 poller가 domain event 발행의 필수 인프라가 된다.
- `NON_TRANSACTIONAL` listener 성공 후 `PUBLISHED` 저장이 실패하면 동일 이벤트가 중복 처리될 수 있다.
- Spring local publisher로 즉시 우회하는 운영 rollback 경로가 사라진다.

### Neutral / Trade-offs

- 전달 보장은 at-least-once이며 consumer의 멱등성 책임은 유지된다.
- Spring event는 제거되지 않고 relay 이후 동일 JVM listener를 찾는 dispatch mechanism으로 제한된다.

## Implementation Notes

### 변경 영역 요약

1. **도메인**: `DomainEvent`에 `OutboxDispatchMode` 기본 계약을 추가한다.
2. **응용 / Port**: `DomainEventPublisher`는 항상 event outbox 영속화를 의미한다.
3. **어댑터 (in)**: `EventOutboxPoller`를 항상 등록한다.
4. **어댑터 (out)**: `SpringDomainEventPublisher`를 제거하고 `OutboxDomainEventPublisher`만 등록한다.
5. **설정 / 환경**: `EVENT_OUTBOX_ENABLED`를 제거하고 poll interval, batch size, max attempts만 유지한다.
6. **테스트**: publisher 고정 구성과 non-transactional dispatch의 transaction/connection 경계를 검증한다.

### 기타 참고

- `NON_TRANSACTIONAL`은 최초 outbox 저장을 비트랜잭션으로 바꾸는 옵션이 아니다. relay listener 실행
  구간만 DB 트랜잭션 밖으로 이동한다.
- 과거 `EVENT_OUTBOX_ENABLED=false` 환경 변수는 더 이상 동작에 영향을 주지 않는다.

## References

- 이슈: `#1035`
- [ADR-018](./018-abstract-spring-event-publisher-for-future-broker.md)
- [ADR-019](./019-introduce-transactional-event-outbox.md)
