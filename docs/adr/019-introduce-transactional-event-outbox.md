# ADR-019: 이벤트 손실 방지를 위한 Transactional Event Outbox 도입

## Status

Accepted

`app.event-outbox.enabled` feature flag와 Spring local publisher rollback 정책은
[ADR-026](./026-enforce-event-outbox-publisher.md)으로 대체되었다.

## Context

[ADR-018](./018-abstract-spring-event-publisher-for-future-broker.md)에서 Spring
`ApplicationEventPublisher` 의존을 `DomainEventPublisher` Port Out으로 추상화했다 (Phase 1).
이 단계는 헥사고날 의존 방향 정리와 도메인 이벤트 메타데이터(`eventId`, `occurredAt`,
`eventType`) 표준화에 집중했고, 발행 메커니즘 자체는 여전히 Spring의
`@TransactionalEventListener(AFTER_COMMIT) + @Async` 조합이다.

### 현재 메커니즘의 위험

`@TransactionalEventListener(AFTER_COMMIT)`로 발행된 이벤트는 **JVM 메모리 안에서만 디스패치**된다.
비즈니스 트랜잭션이 commit된 직후 listener가 비동기 처리를 마치기 전에 JVM이 비정상 종료되면
이벤트가 영구 손실된다. 구체적 시나리오:

- **회원가입**: `member` INSERT는 commit됐는데, `AuditLogEvent` listener가 `audit_log` INSERT를
  하기 전에 JVM kill → 가입은 완료됐는데 감사 로그 누락. 컴플라이언스/포렌식 추적 실패.
- **이메일 인증**: `email_verification` INSERT는 commit됐는데, `SendVerificationEmailEvent`
  listener가 SMTP 호출 전에 JVM kill → 사용자는 세션 ID를 받았지만 메일은 영영 안 옴 →
  "인증 코드 어디 갔어요?" CS 발생, 재시도 불가.
- **향후 도메인 확장**: 결제 완료 → 알림 발송, 회원 탈퇴 → 외부 시스템 정리 등 이벤트 손실이
  실제 사용자 피해로 이어지는 시나리오가 도메인 확장과 함께 증가한다.

### 분산 시스템의 근본 제약

이는 흔히 **Dual-Write Problem**이라 부르는 분산 시스템의 근본 제약이다. "DB 변경 + 외부 시스템
알림"을 같이 해야 할 때 두 작업이 서로 다른 트랜잭션 경계에 속하면 원자성을 보장할 수 없다.
분산 트랜잭션(XA/2PC)이 이론적 해법이지만 외부 broker(Kafka 등)가 제대로 지원하지 않거나
성능 페널티가 크다.

### 무엇이 Phase 2 결정을 가속하는가

1. 위 시나리오들이 도메인 확장과 함께 빈도가 증가한다.
2. ADR-018에서 도입한 `DomainEventPublisher` 추상화가 어댑터 교체만으로 메커니즘 전환을 가능하게 만들었다.
3. 향후 Kafka/RabbitMQ 도입 시 outbox는 broker로 가는 "안전한 다리" 역할을 한다. 미리 도입하면
   broker 전환이 점진적이 된다.

### ADR-018 표현 보정

ADR-018 본문에 "Phase 2는 `FcmOutbox` 패턴을 일반화한다"라고 적었으나 이는 부정확하다.
`FcmOutbox`(현재 dead code)는 Firebase **토픽 subscribe/unsubscribe API 호출의 트랜잭션-안전
분리**를 목적으로 만들어진 단일 용도 outbox다. Phase 2의 EventOutbox는 **모든 도메인 이벤트의
보장된 전달**이 목적이라 도메인이 다르다. 메커니즘(DB 테이블 + poller + 상태 머신)은 비슷하지만
재사용/일반화 관계가 아니다. EventOutbox는 별도 신규 메커니즘으로 도입하며, FcmOutbox는
[ADR-020](./020-remove-fcm-outbox-dead-code.md)에 따라 별도 정리한다.

## Decision

우리는 다음 구조의 **Transactional Event Outbox**를 도입하기로 결정한다.

```
┌──────────────────────────────────────────┐
│  비즈니스 트랜잭션 (단일 PostgreSQL)      │
│  ──────────────────────────────────────  │
│  BEGIN                                   │
│    INSERT INTO members (...)             │  ← 비즈니스 변경
│    INSERT INTO event_outbox (...)        │  ← 이벤트 영속화
│  COMMIT                                  │  ← 둘 다 원자적
└──────────────────────────────────────────┘
                  │
                  ▼
┌──────────────────────────────────────────┐
│  EventOutboxPoller (@Scheduled)          │
│  ──────────────────────────────────────  │
│  LOOP:                                   │
│    SELECT * FROM event_outbox            │
│      WHERE status IN                     │
│        ('PENDING', 'PROCESSING')         │
│        AND next_attempt_at <= now()      │
│      ORDER BY next_attempt_at, id        │
│      FOR UPDATE SKIP LOCKED              │
│      LIMIT N;                            │
│    → status = 'PROCESSING'               │
│    → next_attempt_at = lease deadline    │
│    COMMIT                                │
│    for each claimed row:                 │
│      → dispatch transaction 시작          │
│      → 역직렬화 → ApplicationEventPublisher │
│        .publishEvent(event)              │
│      → status = 'PUBLISHED'             │
│      → dispatch transaction commit       │
│      → 기존 @TransactionalEventListener  │
│        listener 가 받아 처리              │
│      → dispatch 실패 시 별도 transaction  │
│        attempts++, next_attempt_at       │
│        지수 백오프 적용                    │
└──────────────────────────────────────────┘
```

### 구체적 결정 항목

1. **신규 어댑터 추가**: `global/event/adapter/out/OutboxDomainEventPublisher`가
   `DomainEventPublisher`를 구현한다. `publish(event)` 호출 시 외부 큐에 보내지 않고 outbox
   테이블에 INSERT만 수행한다.
2. **기존 어댑터(`SpringDomainEventPublisher`)는 유지한다.** 두 어댑터 중 어느 쪽을 활성화할지는
   `app.event-outbox.enabled` feature flag로 선택한다. 점진 롤아웃 + 즉시 롤백 가능.
3. **Listener 코드는 변경하지 않는다.** Poller가 outbox row를 처리할 때 내부적으로 Spring
   `ApplicationEventPublisher.publishEvent(event)`로 디스패치하므로, 기존 `@TransactionalEventListener`
   리스너들이 그대로 받아 처리한다. 단, `AFTER_COMMIT` 단계 의미가 더 이상 필요하지 않으므로
   `@EventListener`로 점진 단순화 가능 (별도 후속 작업).
   현재 `PUBLISHED` 상태는 “local Spring event bus dispatch transaction이 예외 없이 commit됨”을
   의미한다. 기존 `@TransactionalEventListener(AFTER_COMMIT)` listener의 실제 side effect는 dispatch
   transaction commit 이후 실행되므로, downstream side effect까지 재시도 범위에 넣으려면 listener를
   동기 listener 또는 broker consumer로 별도 전환해야 한다.
4. **트랜잭션 동작 변경**: 발행 시점이 "비즈니스 commit 직후"가 아닌 "비즈니스 commit과 동시에
   outbox INSERT 영속화 + poller 다음 주기에 처리"가 된다. 즉 발행 가시성에 **poller 주기만큼
   지연**이 생긴다 (현재 기본 설정 1초).
5. **멱등성**: outbox row와 발행 이벤트는 `eventId(UUID)`로 1:1 대응된다. Phase 3 broker 도입
   시점에 consumer가 `eventId` 기반 dedup으로 exactly-once-effect를 달성할 수 있다.
6. **단계적 적용**: SendVerificationEmailEvent부터 우선 적용 (가장 임팩트 큼). AuditLogEvent,
   FcmOutboxEvent(살아있다면)는 검증 후 합류.

## Alternatives Considered

### 대안 A: Phase 1만 유지하고 outbox는 broker 도입 시점까지 미루기

장점:
- 추가 인프라/스키마 변경 불필요. 코드베이스 단순함 유지.

단점:
- 이벤트 손실 시나리오는 그대로 잔존.
- Kafka 등 broker를 직접 도입할 때 outbox와 broker 변경을 한꺼번에 해야 함 → 변경 폭과 리스크 증가.

선택하지 않은 이유:
이벤트 손실 시나리오 중 일부(예: 이메일 인증 메일 누락)는 broker 없이도 사용자 피해를
일으킨다. broker 도입은 시기상조이지만 outbox는 broker와 무관한 단독 가치가 있다.

### 대안 B: Spring Modulith의 `event_publication` 메커니즘 사용

[Spring Modulith](https://spring.io/projects/spring-modulith)는 Spring 팀이 공식 지원하는
모듈러 모놀리스 라이브러리로, `@ApplicationModuleListener` + `event_publication` 테이블 기반
outbox를 기본 제공한다.

장점:
- 구현 비용 없음. 의존성 추가로 즉시 활용 가능.
- Spring 공식 메인테넌스 → 장기적으로 안정.
- 우리가 직접 만들려는 메커니즘과 거의 동일한 구조.

단점:
- Spring Modulith의 모듈 경계 검증, `ApplicationModuleListener` 어노테이션 등 부가 기능까지
  도입해야 함. 우리 헥사고날 구조와 미묘하게 충돌할 수 있음.
- `DomainEventPublisher` Port Out 추상화와 어떻게 결합할지 추가 설계 필요.
- 버전 안정성: 1.x 초기 (2024-).

선택하지 않은 이유:
첫 도입에서는 의존성과 추상화 결합 방식을 검토할 시간이 필요하다. 우리가 직접 만드는 outbox는
약 200~300 라인 수준이고, 기존 `DomainEventPublisher` 추상화에 자연스럽게 어댑터로 들어맞는다.
단, **운영 6개월 후 Spring Modulith로 마이그레이션할 가능성은 열어둔다** — 추상화 덕분에 어댑터
교체로 가능하다.

### 대안 C: Debezium CDC 도입

[Debezium](https://debezium.io/)은 DB의 transaction log (PostgreSQL WAL)를 직접 읽어 Kafka로
publish하는 CDC 도구다. Outbox 테이블 변경을 자동으로 broker로 routing할 수 있다.

장점:
- Push 기반이라 latency 압도적으로 낮음 (수 ms~).
- Poller 코드 불필요.
- 업계 표준급 도구.

단점:
- Kafka Connect 인프라 운영 부담.
- broker(Kafka) 도입이 선행 조건. 우리는 아직 broker 없음.
- 학습 곡선.

선택하지 않은 이유:
broker 도입 전이라 Debezium의 본 가치를 활용할 수 없다. Phase 3 broker 도입 시점에 다시 검토.
지금 도입한 단순 poller는 broker 도입 시점에 Debezium으로 교체하기 용이하다 (outbox 테이블
스키마는 그대로, 디스패치 메커니즘만 교체).

### 대안 D: Redis 기반 outbox

Redis에 outbox queue를 두고 비즈니스 트랜잭션 commit 후 Redis에 push.

장점:
- 처리량 높음. Latency 낮음.

단점:
- **트랜잭션 일관성 보장 불가**. PostgreSQL 트랜잭션과 Redis 쓰기는 서로 다른 시스템 →
  outbox 패턴이 풀려는 dual-write 문제를 그대로 재현.
- Redis 데이터 유실 위험 (메모리 기반, 영속화 옵션은 디스크지만 fsync 지연 가능).
- Redis 운영 부담 추가.

선택하지 않은 이유:
outbox 패턴의 본질은 "비즈니스 데이터와 같은 트랜잭션에서 영속화"인데 Redis는 이를 만족하지
못한다. Redis는 Phase 3에서 broker 또는 분산 락 용도로 별도 검토 가능 (이 경우는 outbox 자체가
아닌 broker 또는 보조 인프라).

### 대안 E: Event Sourcing 도입

도메인 상태를 "이벤트 시퀀스"로 저장하는 근본적 모델 전환 (Axon Framework, EventStoreDB 등).

장점:
- 감사 추적, 시간 여행, 복잡한 도메인 모델링에 강함.
- 이벤트가 곧 진실이므로 dual-write 문제 자체가 사라짐.

단점:
- 도메인 모델 전면 재설계 필요.
- 학습 곡선 매우 큼. 팀 전체 합의 필요.
- 대부분 도메인은 CRUD가 적절하고 event sourcing은 과도.

선택하지 않은 이유:
범위와 비용이 outbox 도입과 비교 불가능하게 크다. 현재 우리 도메인(회원/감사/알림)은 CRUD로
충분히 표현되고, event sourcing의 강점(시간 여행, 도메인 복잡도)을 필요로 하지 않는다.

## Consequences

### Positive

- **이벤트 손실 방지**: 비즈니스 commit과 이벤트 영속화가 같은 트랜잭션이므로 JVM 비정상
  종료에도 이벤트는 outbox에 남아 재시작 후 처리된다.
- **자동 재시도**: local Spring event bus dispatch 이전의 일시적 실패는 `attempts` 컬럼과
  지수 백오프로 재시도된다. `AFTER_COMMIT` listener의 downstream side effect 실패는 재시도 범위 밖이다.
- **At-least-once relay 시도**: outbox row를 local event bus에 전달하는 단계까지 재시도한다.
  이메일·웹훅 등 downstream side effect의 end-to-end 전달 보장은 consumer별 별도 설계가 필요하다.
- **멱등성 hooks**: `eventId(UUID)`가 영속화되므로 consumer가 dedup 가능. Phase 3에서
  exactly-once-effect 달성의 토대.
- **점진적 broker 전환**: Phase 3에서 KafkaRelay가 outbox를 폴링하도록 어댑터만 추가하면 됨.
  큰 마이그레이션 없음.
- **운영 가시성**: outbox 테이블 자체가 "처리 중인 이벤트의 단일 진실의 원천(SSOT)"이라
  Prometheus 메트릭(PENDING 적체, FAILED 수) 추출이 직관적이다.

### Negative

- **지연 추가**: 발행 시점이 "commit 직후 즉시"가 아닌 "다음 poller 주기"가 된다 (현재 기본 1초).
  대부분 use case는 영향 없지만, 즉시 발행이 필요한 use case가 있다면 별도 설계 필요.
- **DB 테이블 hot path**: `event_outbox` INSERT/SELECT/UPDATE가 빈번해진다. Index 설계와
  cleanup 정책이 필수.
- **운영 부담 추가**: poller 모니터링, 누적 row 정리 job, FAILED row 알람 등.
- **트랜잭션 동작 변경**: 기존 `AFTER_COMMIT` 의미가 약간 달라진다. 일부 listener가 commit과
  발행 사이의 즉시성에 의존한다면 회귀 가능 (현재 listener들은 모두 비동기/지연 허용이라 안전).
- **DB 크기 증가**: 처리된 이벤트가 정리 전까지 쌓인다. cleanup 정책이 없으면 무한 증가.

### Neutral / Trade-offs

- **Exactly-once는 여전히 환상**: relay와 consumer side effect 사이에는 원자적 transaction이 없으므로
  중복과 유실 가능성이 남는다. DB side effect는 `(consumer, eventId)` unique inbox 기록과 실제 변경을
  같은 transaction으로 묶고, 외부 API는 `eventId` 기반 idempotency key를 지원해야
  exactly-once-effect에 가깝게 만들 수 있다.
- **`@TransactionalEventListener(AFTER_COMMIT)`의 진화**: outbox 도입 후 listener의
  `AFTER_COMMIT` phase가 사실상 불필요해진다 (poller가 commit 이후의 상태에서만 row를 읽으니까).
  단순화 가능하지만 호환성 유지를 위해 즉시 제거하지 않는다.
- **Spring Modulith 도입 미루기**: 직접 구현이 200~300 라인 수준이라 비용이 작지만, 장기적으로
  Spring 공식 메커니즘으로 교체할 의향은 유지한다.
- **Phase 3 broker 도입 시 어댑터 교체 vs poller 교체**: 향후 Kafka 도입 시 두 가지 길이 있다.
  (a) OutboxDomainEventPublisher는 그대로 두고 KafkaRelay를 poller로 교체, (b) Debezium
  도입해서 outbox 변경을 자동 Kafka publish. 시기에 따라 선택.

## Implementation Notes

### 패키지 구조 (Phase 2 신규)

```
src/main/java/com/umc/product/global/event/
├── domain/
│   ├── DomainEvent.java                       # (기존, ADR-018)
│   └── EventOutbox.java                       # (신규) outbox 엔티티
├── application/
│   ├── port/out/
│   │   ├── DomainEventPublisher.java          # (기존, ADR-018)
│   │   ├── LoadEventOutboxPort.java           # (신규)
│   │   └── SaveEventOutboxPort.java           # (신규)
│   └── service/
│       └── EventOutboxRelayService.java       # (신규) poller가 호출
└── adapter/
    ├── in/scheduler/
    │   └── EventOutboxPoller.java             # (신규)
    └── out/
        ├── EventPayloadSerializer.java        # (신규) 이벤트 JSON 직렬화
        ├── EventPayloadDeserializer.java      # (신규) event_class 기반 역직렬화
        ├── SpringDomainEventPublisher.java    # (기존, ADR-018)
        ├── OutboxDomainEventPublisher.java    # (신규) 기본 어댑터
        └── persistence/
            ├── EventOutboxPersistenceAdapter.java
            └── EventOutboxJpaRepository.java
```

### Flyway 마이그레이션

```sql
-- V2026.NN.NN.NN.NN__create_event_outbox.sql
CREATE TABLE event_outbox (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID NOT NULL,
    event_type      VARCHAR(150) NOT NULL,
    event_class     VARCHAR(300) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL,    -- PENDING | PROCESSING | PUBLISHED | FAILED
    attempts        INT NOT NULL DEFAULT 0,
    last_error      TEXT,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

-- 멱등성을 위한 UNIQUE: 같은 eventId가 두 번 들어오지 않음
CREATE UNIQUE INDEX uix_event_outbox_event_id ON event_outbox(event_id);

-- 폴링 효율을 위한 partial index. PUBLISHED/FAILED 누적 row는 polling index에서 제외한다.
CREATE INDEX idx_event_outbox_publishable
    ON event_outbox(next_attempt_at, id)
    WHERE status IN ('PENDING', 'PROCESSING');

-- cleanup scheduler를 도입할 때 status/published_at 인덱스를 추가로 검토한다.
```

### Poller 동시성 및 transaction 경계

멀티 인스턴스 환경(여러 JVM이 동시에 poller 동작)에서는 다음 둘 중 하나로 race를 방지한다:

- (선호) `SELECT ... FOR UPDATE SKIP LOCKED` — PostgreSQL이 row 단위로 자연스럽게 분배.
- (대안) Redisson `RLock` 기반 leader election — poller가 한 인스턴스에서만 동작.

초기 도입은 `SKIP LOCKED`로 충분. 적체가 보이면 leader election 추가 검토.

Relay는 claim transaction과 이벤트별 결과 transaction을 분리한다.

1. **claim transaction**: publishable row를 조회하고 `PROCESSING` 상태와 lease deadline을 저장한 뒤 즉시 commit한다.
2. **dispatch transaction**: `ApplicationEventPublisher.publishEvent(event)` 호출과 `PUBLISHED` 상태 변경을
   같은 transaction에서 수행한다. commit 이후 기존 `@TransactionalEventListener(AFTER_COMMIT)` listener가 실행된다.
3. **failure transaction**: dispatch transaction이 실패하면 별도 transaction에서 `PENDING` 또는 `FAILED`와
   failure metadata를 저장한다.

이 구조는 row lock을 listener 실행 시간 동안 잡지 않도록 하고, dispatch transaction이 rollback-only가 되더라도
failure metadata가 별도 transaction에서 보존되도록 하기 위한 것이다. `PROCESSING` 상태의 `next_attempt_at`은
lease deadline으로 사용하며, lease가 만료된 row는 다시 claim 대상이 된다. 다만 `PUBLISHED` commit 이후 실행되는
listener side effect의 성공은 이 상태에 반영되지 않는다.

### 직렬화 / 역직렬화

- `EventPayloadSerializer`는 Jackson `ObjectMapper.writeValueAsString(event)`로 payload JSON을 만든다.
- `EventOutbox.record`는 `event.getClass().getName()`을 `event_class`에 저장한다.
- `EventPayloadDeserializer`는 `event_class`를 `Class.forName()`으로 복원하고, 해당 클래스가
  `DomainEvent`를 구현하는지 확인한 뒤 payload를 읽는다.
- 이 방식은 `global/event` 모듈이 `SendVerificationEmailEvent` 같은 구체 이벤트 타입에 직접 의존하지
  않게 하기 위한 초기 구현이다. broker 도입 시에는 Java class name 대신 안정적인 external type
  registry를 별도로 둘 수 있다.
- Listener 측은 직렬화/역직렬화를 신경 쓰지 않도록 envelope 처리 책임을
  `OutboxDomainEventPublisher`와 `EventOutboxRelayService`에 한정한다.

### 백오프 정책

- 첫 실패: 즉시 재시도 (5초 후 poller 다음 주기).
- 이후 실패: `2^attempts * 5s` 백오프 (5s, 10s, 20s, 40s, ...).
- `attempts >= app.event-outbox.max-attempts` 도달 시 `FAILED` 상태로 마킹하고 알람.
- 운영 중 정책 변경이 잦을 것이므로 상수가 아닌 `app.event-outbox.*` 프로퍼티로 노출.

### Cleanup 정책

- PUBLISHED row는 24시간 후 별도 Scheduler가 DELETE (또는 archive 테이블로 이관).
- FAILED row는 영구 보존 (운영자 수동 처리 대상).
- 정책은 `app.event-outbox.cleanup.*` 프로퍼티로 조정.

### Feature Flag 및 롤아웃

```yaml
app:
  event-outbox:
    enabled: true         # 기본 OutboxDomainEventPublisher 활성
    poll-interval-ms: 1000
    batch-size: 100
    max-attempts: 5
    cleanup:
      published-retention-hours: 24
      run-interval-minutes: 60
```

활성화 및 롤백 절차:

1. 애플리케이션 시작 전 Flyway가 Outbox 테이블과 polling index migration을 적용한다.
2. 기본값 `enabled=true`로 `OutboxDomainEventPublisher`와 poller를 활성화한다.
3. PENDING 적체, FAILED row, polling lag와 DB 부하를 감시한다.
4. 회귀 시 `EVENT_OUTBOX_ENABLED=false`로 `SpringDomainEventPublisher`에 롤백한다.

### 모니터링 메트릭 (Prometheus)

- `event_outbox_pending_total{event_type}`
- `event_outbox_failed_total{event_type}`
- `event_outbox_publish_duration_seconds{event_type}` (히스토그램)
- `event_outbox_poll_lag_seconds` (가장 오래된 PENDING의 `now() - created_at`)

### Phase 2 PR 분할 전략 (참고)

1. PR 1: outbox 엔티티 + 마이그레이션 + 포트 정의 (실제 동작 없음, 인프라만)
2. PR 2: `OutboxDomainEventPublisher` + poller + relay service (feature flag off)
3. PR 3: cleanup 스케줄러 + 모니터링 메트릭
4. PR 4: feature flag on 전환 + 운영 가이드 문서

각 PR은 독립적 머지 + 롤백 가능하도록 설계한다.

### 진입 / 진출 조건

**Phase 2 진입 조건** (이 ADR 머지 후):
- [ADR-020](./020-remove-fcm-outbox-dead-code.md)에 따라 `FcmOutbox` dead code 정리 선행 권장
  (이름/스키마 충돌 방지).

**Phase 3(외부 broker 도입) 진입 조건**:
- 다음 중 하나가 충족될 때 검토.
  - 도메인 간 트래픽이 단일 JVM 처리 한계 근접.
  - 외부 시스템(검색 색인, 분석 파이프라인 등)이 도메인 이벤트를 소비할 필요 발생.
  - Outbox 테이블 부하가 운영 부담이 될 때 Debezium 검토.

## References

- [ADR-018: Spring Event Publisher 추상화](./018-abstract-spring-event-publisher-for-future-broker.md)
- [ADR-020: FcmOutbox dead code 정리](./020-remove-fcm-outbox-dead-code.md)
- Microservices.io: Transactional Outbox Pattern (Chris Richardson)
- Confluent: "Reliable Microservices Data Exchange With the Outbox Pattern"
- Spring Modulith: `event_publication` 메커니즘 (Spring 공식)
- Debezium: Outbox Event Router (CDC 기반 outbox 자동화)
