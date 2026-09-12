# Scheduler 현황

이 문서는 UMC PRODUCT 서버에서 사용하는 스케줄러와 비동기 실행 자원을 정리한다. 새 스케줄러를 추가하기 전에 이 문서를 먼저 확인한다.

## 실행 풀

| 풀 | Bean | Thread prefix | 용도 |
| --- | --- | --- | --- |
| 전역 scheduled pool | `taskScheduler` | `scheduled-` | 일반 `@Scheduled` 작업 실행 |
| Project 매칭 데드라인 pool | `matchingDeadlineTaskScheduler` | `matching-deadline-` | project 매칭 차수 결정 마감 1회성 task |
| Webhook async executor | `webhookTaskExecutor` | `webhook-` | webhook alarm event listener의 외부 webhook I/O |
| Email async executor | `emailTaskExecutor` | `email-` | 인증 메일 발송 |
| Audit async executor | `auditTaskExecutor` | `audit-` | 감사 로그 저장 |

전역 `@Scheduled` 작업은 반드시 `taskScheduler`를 사용한다. Project 매칭 데드라인은 `@Scheduled`가 아니라 `TaskScheduler.schedule(...)`로 동적 등록되며, `@Qualifier("matchingDeadlineTaskScheduler")`로 전용 풀을 주입받는다.

## 등록된 Scheduled 작업

| 도메인 | 클래스 | 주기 | 활성 조건 | 판단 |
| --- | --- | --- | --- | --- |
| maintenance | `MaintenanceStateRefreshScheduler` | `fixedDelay = 10s` | scheduling enabled profile | 다중 인스턴스 점검 상태 동기화를 위한 짧은 polling. 실제 DB 조회는 `MaintenanceStateHolder.refresh()`에 위임한다. |
| authentication | `EmailVerificationRetentionScheduler` | 매일 03:00 KST | scheduling enabled profile | 만료 인증 세션 회수. 저빈도 정리 잡으로 적절하다. |
| curriculum | `WorkbookAutoReleaseScheduler` | 매일 00:00 KST | scheduling enabled profile | 워크북 자동 배포. 저빈도 도메인 batch로 적절하다. |
| notification | `FcmOutboxScheduler` | `app.fcm.outbox-interval-ms` | `app.fcm.enabled=true` | FCM outbox polling. FCM 미사용 환경에서는 scheduler bean 자체를 등록하지 않는다. |
| notification | `FcmTokenValidationScheduler` | `app.fcm.token-validation-interval-ms` | `app.fcm.enabled=true`, `app.fcm.token-validation-enabled=true` | 오래 검증되지 않은 활성 토큰을 batch dry-run으로 검증한다. 다중 인스턴스에서는 전용 batch 인스턴스 한 곳에서만 활성화한다. |
| global event | `EventOutboxPoller` | `app.event-outbox.poll-interval-ms` | `app.event-outbox.relay-enabled=true` (기본값) | persistent event outbox relay. 중지해도 publisher는 outbox 적재를 계속하며, 외부 broker 전환 전까지 허용되는 polling 작업이다. |

## Project 매칭 데드라인

`MatchingRoundDeadlineScheduler`는 일반 `@Scheduled`가 아니다. 매칭 차수 create/update/delete lifecycle에 맞춰 decision deadline 이후 1회성 task를 등록하거나 취소한다.

- Config: `MatchingDeadlineSchedulerConfig`
- Adapter: `MatchingRoundDeadlineScheduler`
- Handler: `MatchingRoundDeadlineHandler`
- 활성 조건: `scheduler.matching-round-deadline.enabled=true` 또는 property 미설정
- 비활성 시 대체: `NoOpMatchingRoundDeadlineScheduler`
- 전용 pool size: 2

이 풀은 project 자동 선발 발화를 위해서만 사용한다. 일반 batch, polling, notification 작업을 이 풀에 올리면 안 된다.

`ScheduleMatchingRoundDeadlinePort`는 `ProjectMatchingRoundCommandService`의 생성자 필수 의존이다. 스케줄러를 끌 때 실제 adapter만 없애면 주입 대상이 사라져 컨텍스트 기동이 실패하므로, `enabled=false`에서는 `NoOpMatchingRoundDeadlineScheduler`가 대신 등록된다. 두 구현의 조건은 배타적이며, 끈 상태에서 자동 선발은 운영진 수동 호출로만 실행된다.

### 테스트 프로필

`@Scheduled` 작업은 `SchedulingConfig`가 `@Profile("!test")`라 test 프로필에서 잠든다. 매칭 데드라인 스케줄러는 `@ConditionalOnProperty`로만 제어되므로 별도로 `src/test/resources/application-test.yml`에서 `scheduler.matching-round-deadline.enabled=false`로 끈다.

끄지 않으면 테스트 fixture의 `decisionDeadline`이 모두 과거 시각이라 등록 즉시 발화하고, `matching-deadline-` 스레드의 자동 선발 트랜잭션이 `@DatabaseIsolation`의 `TRUNCATE`와 겹친다. 락 경합이 나거나 TRUNCATE 이후 커밋된 행이 다음 테스트로 새어 나가, 단독 실행은 통과하고 전체 실행에서만 깨지는 간헐 실패가 된다.

이 격리는 `BackgroundSchedulerIsolationTest`가 고정한다. 특정 테스트에서 스케줄러 동작을 검증해야 하면 전역으로 켜지 말고 `@TestPropertySource`로 그 테스트에서만 켜고, 발화한 task가 끝난 뒤 테스트를 종료한다.

## Webhook 알림

기존 webhook buffer scheduler는 제거했다. `sendBuffered(...)`는 더 이상 JVM 메모리 큐에 적재하지 않고 `WebhookAlarmEvent`를 발행한다.

흐름:

1. application/controller 흐름에서 `SendWebhookAlarmUseCase.sendBuffered(command)` 호출
2. `WebhookAlarmService`가 `WebhookAlarmEvent` 발행
3. `WebhookAlarmEventListener`가 `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`로 수신
4. listener가 `@Async("webhookTaskExecutor")`에서 기존 `send(...)` usecase 호출

이 구조는 트랜잭션 rollback 후 유령 알림이 나가는 문제와 scheduler thread에서 외부 webhook I/O를 수행하는 문제를 줄인다. 여러 알림을 하나의 메시지로 묶는 기능이 다시 필요하면 메모리 큐가 아니라 event outbox 또는 별도 persistent aggregation 테이블을 사용한다.

## FCM 배치 발송

`FcmNotificationRequestedEvent`는 `TRANSACTIONAL` mode로 대상 조회와 batch outbox 저장을 현재 요청 outbox의 `PUBLISHED` 변경과 함께 commit한다. 실제 Firebase I/O를 수행하는 `FcmSendBatchRequestedEvent`만 `NON_TRANSACTIONAL` mode를 사용한다. batch listener는 DB 트랜잭션 밖에서 동기 실행되어 transient 발송 실패를 공용 event outbox 재시도로 연결하고, Firebase 응답 대기 중 JDBC connection을 점유하지 않는다. 무효 토큰의 `saveAll`과 event outbox 상태 변경만 각각 짧은 쓰기 트랜잭션으로 처리한다.

발송 보장은 at-least-once다. Firebase 발송 성공 후 event outbox의 `PUBLISHED` 커밋이 실패하면 같은 batch 전체가 재시도되어 최대 500개 토큰에 중복 푸시가 발생할 수 있다. FCM API가 batch 요청의 멱등성 키를 제공하지 않으므로 현재 `requestId`는 서버 추적 용도로만 사용하며, 중복보다 누락 방지를 우선한다.

Firebase multicast가 전체 예외를 던지면 현재 batch outbox가 `PENDING`으로 돌아가 공용 backoff 정책에 따라 재시도된다. 응답 안에서 일부 token만 `INTERNAL`, `UNAVAILABLE`, `QUOTA_EXCEEDED`로 실패하면 성공 token을 다시 보내지 않고 해당 token ID만 새 batch outbox에 저장한다. `UNREGISTERED` token은 재시도하지 않고 비활성화한다.

## 추가 기준

Event Outbox의 생성부터 listener 소비, 실패 재시도까지의 상세 흐름은
[Event Outbox 발행 및 소비 흐름](event-outbox-flow.md)을 참고한다.

새 스케줄러를 추가할 때는 다음 기준을 따른다.

- `@Scheduled` 진입점은 `adapter/in/scheduler`에 둔다.
- disabled 상태에서 no-op polling만 반복하는 작업은 만들지 않는다. `@ConditionalOnProperty`로 bean 등록 자체를 막는다.
- 외부 API, webhook, LLM처럼 지연 시간이 긴 I/O는 scheduler thread에서 직접 오래 점유하지 않는다. 필요하면 event listener나 전용 executor로 분리한다.
- outbox listener에서 외부 I/O를 동기 실행해야 하면 `NON_TRANSACTIONAL` dispatch를 사용해 DB connection 점유를 피하고 예외를 relay 재시도로 연결한다.
- 도메인별 1회성 동적 task가 필요하면 전역 `taskScheduler`를 공유할지 전용 `TaskScheduler`가 필요한지 먼저 판단한다.
- 다중 인스턴스에서 중복 실행되면 안 되는 작업은 DB lease, unique constraint, outbox claim, ShedLock 중 하나로 방어한다.
- 새 작업은 실행 주기, 활성 property, 멱등성 전략, 실패 재시도 전략을 이 문서에 추가한다.
