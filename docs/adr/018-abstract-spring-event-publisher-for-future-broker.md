# ADR-018: Spring Event Publisher를 추상화하여 미래 메시지 브로커 도입을 대비한다

## Status

Accepted

기본 어댑터로 `SpringDomainEventPublisher`를 선택한 결정은
[ADR-026](./026-enforce-event-outbox-publisher.md)으로 대체되었다. `DomainEventPublisher` Port Out
추상화 결정은 계속 유효하다.

## Context

UMC PRODUCT 서버는 현재 도메인 간 비동기 협력을 위해 Spring의 `ApplicationEventPublisher`와
`@TransactionalEventListener(AFTER_COMMIT) + @Async` 조합을 사용한다. 사용 현황은 다음과 같다.

- `AuditAspect`, `MemberService`, `IdPwMemberRegisterService`가 `ApplicationEventPublisher`를
  직접 주입받아 `AuditLogEvent`를 발행한다.
- `AuditLogEventListener`, `FcmOutboxEventListener`가
  `@TransactionalEventListener(AFTER_COMMIT)`로 이벤트를 수신해 비동기 처리한다.
- `notification` 도메인에는 이미 풀(폴링) 기반 outbox(`FcmOutbox*`)가 부분 구현되어 있다.
- `ServerLifecycleAlarmListener`는 Spring lifecycle 이벤트(`ApplicationReadyEvent`)에
  반응한다.

현재 구조에는 다음 문제가 있다.

1. **헥사고날 의존 방향 위반**: 응용/도메인 레이어가 Spring 인프라 API
   (`ApplicationEventPublisher`)를 직접 참조한다. CLAUDE.md에 명시된 hexagonal
   원칙(`application/service`는 인프라에 직접 의존하지 않음)을 깨고 있다.
2. **이벤트 메타데이터 부재**: 이벤트에 `eventId`, `occurredAt`, `eventType` 같은 식별자/시각
   정보가 없다. 멱등성(idempotency, 같은 이벤트가 두 번 들어와도 한 번만 처리되는 성질)을
   보장하기 어렵고, 추적성도 떨어진다.
3. **트랜잭션 일관성 보장 부재**: `AFTER_COMMIT` 이벤트는 in-memory 디스패치이므로 JVM이
   비정상 종료되면 손실된다. 향후 Kafka/RabbitMQ로 옮길 때 이 문제는 더 커진다.
4. **브로커 교체 비용**: 추상화 계층이 없어, 향후 외부 메시지 브로커(Kafka, RabbitMQ 등)를
   도입할 때 모든 발행/수신 지점을 수정해야 한다.

본 ADR은 위 문제 중 (1), (2), (4)를 1차로 해결하고, (3)은 후속 ADR로 분리하기 위한 결정을
기록한다.

## Decision

우리는 Spring `ApplicationEventPublisher` 의존을 **응용 레이어의 Port Out 인터페이스로
추상화**하기로 결정한다. 구체적인 결정은 아래와 같다.

1. **Port Out 도입**: 응용 레이어에 `DomainEventPublisher` 인터페이스를 정의하고, 도메인
   서비스/Aspect는 이 인터페이스에만 의존한다.
2. **기본 어댑터**: `SpringDomainEventPublisher`가 Spring `ApplicationEventPublisher`로
   위임하는 기본 구현을 제공한다.
3. **도메인 이벤트 표준화**: 모든 도메인 이벤트는 `DomainEvent` 인터페이스를 구현하고
   `eventId(UUID)`, `occurredAt(Instant)`, `eventType(String)` 메타데이터를 제공한다.
4. **Listener 추상화 범위 제한 (Phase 1)**: 이번 단계에서는 Listener 측은
   Spring `@TransactionalEventListener`를 그대로 유지한다. 브로커 도입 시 별도 bridge
   어댑터(`adapter/in/event/Kafka*EventBridge` 등)를 추가하는 방식으로 확장한다.
5. **단계적 진행 + PR 분리**:
   - **Phase 1 (이 PR)**: Publisher 추상화 + 도메인 이벤트 메타데이터 표준화.
   - **Phase 2 (별도 PR)**: `FcmOutbox` 패턴을 일반화한 `EventOutbox`로 트랜잭션 일관성 확보.
   - **Phase 3 (브로커 도입 시점)**: Kafka/RabbitMQ 어댑터 추가.

   각 Phase는 독립된 PR로 분리하여 리뷰 단위와 롤백 단위를 작게 유지한다.

## Alternatives Considered

### 대안 A: Phase 1만 수행 (Publisher 추상화만, outbox 없음)

장점:
- 변경 범위가 약 10개 파일로 작아 도입 비용이 낮다.
- Listener는 변경하지 않으므로 회귀 위험이 낮다.
- 향후 Kafka 도입 시 호출부 수정 없이 어댑터 교체만으로 전환 가능하다.

단점:
- AFTER_COMMIT 이벤트의 손실 가능성은 그대로 남는다.
- 멱등성 처리는 추가 작업이 필요하다 (Phase 2에서 outbox와 함께 도입).

선택한 이유:
이번 PR의 목표는 헥사고날 의존 방향 정리와 메타데이터 표준화다. 트랜잭션 일관성은 별도의
설계 트레이드오프(테이블 스키마, poller 동시성, 모니터링)가 필요하므로 분리하는 것이 안전하다.

### 대안 B: Phase 1 + Phase 2를 하나의 PR로 묶기

장점:
- 한 번에 전체 그림이 완성된다.
- 트랜잭션 일관성 문제도 동시에 해결된다.

단점:
- 변경 범위가 약 20개 파일 이상으로 커진다.
- DB 마이그레이션(`event_outbox` 테이블 추가), 폴러 동시성 처리, 운영 메트릭까지 한 번에
  검증해야 하므로 리뷰가 어려워진다.
- 문제 발생 시 롤백 단위가 너무 크다.

선택하지 않은 이유:
추상화 도입과 트랜잭션 일관성 보장은 서로 다른 변경 동기다. 한 PR에 묶으면 리뷰 집중도가
떨어지고, 한쪽에서 문제가 생겼을 때 다른 쪽까지 함께 되돌려야 한다.

### 대안 C: Listener까지 한 번에 추상화 (`@EventHandler("audit.log.created")` 같은 custom annotation)

장점:
- Publisher와 Listener가 대칭적으로 추상화된다.
- 브로커 도입 시 listener 코드도 거의 변경되지 않는다.

단점:
- Spring AOT/reflection을 다루어야 하므로 Phase 1 범위가 커진다.
- 현재 `@TransactionalEventListener`가 잘 동작하고 있어 즉시 얻는 이득이 작다.
- 브로커 도입 시점에는 어차피 broker별 listener 어댑터(예: `@KafkaListener`)가 필요하므로
  중복 추상화가 된다.

선택하지 않은 이유:
Phase 1의 가치는 **응용 레이어의 인프라 의존 제거**다. Listener는 이미 `adapter/in/event/`
패키지에 위치하므로 헥사고날 관점에서 문제가 없다. 브로커 도입 시점에 broker별 bridge
어댑터를 추가하는 것이 더 자연스럽다.

### 대안 D: 메타데이터(`eventId`, `occurredAt`)는 Phase 2에서 도입

장점:
- Phase 1 변경량이 약간 더 작아진다.

단점:
- Phase 2에서 모든 이벤트 record를 다시 수정해야 한다.
- 그 사이 새로 추가되는 이벤트들은 메타데이터 없이 작성된다.
- 브로커 도입 후 멱등성 처리가 무료로 따라오지 못한다.

선택하지 않은 이유:
이벤트 record 수정은 한 번에 모아두는 것이 마이그레이션 비용이 작다. 또한 `eventId`/
`occurredAt`은 단순 record 필드 추가이므로 Phase 1에 포함해도 변경 비용이 거의 없다.

## Consequences

### Positive

- 응용 레이어가 Spring 인프라 API에 의존하지 않으므로, 헥사고날 의존 방향이 회복된다.
- 모든 도메인 이벤트가 식별자/시각 메타데이터를 갖게 되어 추적성이 개선된다.
- 향후 Kafka/RabbitMQ 등 외부 브로커로 전환할 때 호출부를 수정하지 않고 어댑터 교체만으로
  대응할 수 있다.
- 멱등성 처리, DLQ, 재시도 정책 등 후속 기능을 도입할 때 메타데이터가 이미 준비되어 있다.
- `FcmOutbox`처럼 부분적으로 존재하는 outbox 패턴을 Phase 2에서 일반화할 수 있는 토대가
  마련된다.

### Negative

- 신규 모듈(`global/event/`)이 추가되어 패키지 수가 늘어난다.
- 모든 이벤트 record에 `eventId`/`occurredAt` 필드를 추가해야 하므로 builder default 설정
  또는 정적 팩토리 메서드로 자동 주입을 일관되게 처리해야 한다.
- Phase 1만으로는 트랜잭션 일관성 문제가 해결되지 않으므로, AFTER_COMMIT 이후 JVM 비정상
  종료 시 이벤트 손실 가능성은 그대로 남는다. (Phase 2에서 해결)

### Neutral / Trade-offs

- Listener는 여전히 `@TransactionalEventListener` 기반이라 broker 도입 시 Listener 측은
  별도 bridge 어댑터를 추가해야 한다. Publisher만 추상화한 비대칭 구조다. 다만 이는
  의도된 분할이며, broker 도입 시점에 broker별 listener를 추가하는 것이 자연스러운
  헥사고날 패턴이다.
- `eventId`/`occurredAt`을 record builder default로 주입하므로 명시적으로 지정하지 않으면
  `UUID.randomUUID()` / `Instant.now()`가 들어간다. 테스트에서 이를 고정하려면 builder에
  값을 직접 넣어야 한다.

## Implementation Notes

### 패키지 구조

```
src/main/java/com/umc/product/global/event/
├── domain/
│   └── DomainEvent.java                  # 마커 인터페이스
├── application/port/out/
│   └── DomainEventPublisher.java         # 포트
└── adapter/out/
    └── SpringDomainEventPublisher.java   # Spring 어댑터 (default)
```

### DomainEvent 인터페이스

```java
public interface DomainEvent {
    UUID eventId();        // 멱등성 처리용 고유 식별자
    Instant occurredAt();  // 이벤트 발생 시각
    String eventType();    // 예: "audit.log.register", "fcm.outbox.created"
}
```

### Phase 1 변경 범위

| 영역 | 변경 내용 |
|------|-----------|
| 신규 (`global/event/`) | 포트, 어댑터, 마커 인터페이스 (3 파일) |
| 수정 (`AuditLogEvent`, `FcmOutboxEvent`) | `implements DomainEvent`, 메타데이터 필드 추가 |
| 수정 (`AuditAspect`, `MemberService`, `IdPwMemberRegisterService`) | `ApplicationEventPublisher` → `DomainEventPublisher` 교체 |
| 미변경 | Listener (`AuditLogEventListener`, `FcmOutboxEventListener`, `ServerLifecycleAlarmListener`) |

### Listener 미변경 정책

Spring은 `@TransactionalEventListener`가 메서드 시그니처의 파라미터 타입(record 타입)으로
이벤트를 라우팅한다. `DomainEventPublisher`가 내부적으로
`ApplicationEventPublisher.publishEvent(event)`로 위임하므로, 기존 Listener는 수정 없이
동일하게 동작한다.

### 메타데이터 자동 주입

이벤트 record는 Lombok `@Builder` default 또는 정적 팩토리 메서드를 통해 `eventId`와
`occurredAt`을 자동 주입한다. 명시적으로 지정하지 않으면 다음 값이 사용된다.

- `eventId`: `UUID.randomUUID()`
- `occurredAt`: `Instant.now()`

테스트에서 결정론적 값이 필요하면 builder에 직접 값을 지정한다.

### 향후 Phase 2 진입 조건

다음 중 하나라도 발생하면 Phase 2(EventOutbox 일반화)를 우선순위 상위로 올린다.

- `AFTER_COMMIT` 이후 이벤트 손실로 인한 운영 이슈가 1회 이상 발생
- 새로운 도메인에서 outbox 패턴이 필요한 경우 (`FcmOutbox`와 동일한 동기로)
- 멱등성 처리가 필요한 컨슈머가 추가되는 경우

### 향후 Phase 3 진입 조건

- 실제 Kafka/RabbitMQ 브로커 도입이 결정되는 시점
- 도메인 간 트래픽이 단일 JVM 처리 한계에 근접하는 시점

## References

- CLAUDE.md: Architecture & Domain Rules (Hexagonal Architecture)
- `src/main/java/com/umc/product/notification/domain/FcmOutbox.java` - 기존 부분 outbox 구현
- `src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java` - 현재
  listener 패턴 참조
