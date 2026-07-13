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
| global event | `EventOutboxPoller` | `app.event-outbox.poll-interval-ms` | 항상 | persistent event outbox relay. 외부 broker 전환 전까지 허용되는 polling 작업이다. |

## Project 매칭 데드라인

`MatchingRoundDeadlineScheduler`는 일반 `@Scheduled`가 아니다. 매칭 차수 create/update/delete lifecycle에 맞춰 decision deadline 이후 1회성 task를 등록하거나 취소한다.

- Config: `MatchingDeadlineSchedulerConfig`
- Adapter: `MatchingRoundDeadlineScheduler`
- Handler: `MatchingRoundDeadlineHandler`
- 활성 조건: `scheduler.matching-round-deadline.enabled=true` 또는 property 미설정
- 전용 pool size: 2

이 풀은 project 자동 선발 발화를 위해서만 사용한다. 일반 batch, polling, notification 작업을 이 풀에 올리면 안 된다.

## Webhook 알림

기존 webhook buffer scheduler는 제거했다. `sendBuffered(...)`는 더 이상 JVM 메모리 큐에 적재하지 않고 `WebhookAlarmEvent`를 발행한다.

흐름:

1. application/controller 흐름에서 `SendWebhookAlarmUseCase.sendBuffered(command)` 호출
2. `WebhookAlarmService`가 `WebhookAlarmEvent` 발행
3. `WebhookAlarmEventListener`가 `@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`로 수신
4. listener가 `@Async("webhookTaskExecutor")`에서 기존 `send(...)` usecase 호출

이 구조는 트랜잭션 rollback 후 유령 알림이 나가는 문제와 scheduler thread에서 외부 webhook I/O를 수행하는 문제를 줄인다. 여러 알림을 하나의 메시지로 묶는 기능이 다시 필요하면 메모리 큐가 아니라 event outbox 또는 별도 persistent aggregation 테이블을 사용한다.

## 추가 기준

새 스케줄러를 추가할 때는 다음 기준을 따른다.

- `@Scheduled` 진입점은 `adapter/in/scheduler`에 둔다.
- disabled 상태에서 no-op polling만 반복하는 작업은 만들지 않는다. `@ConditionalOnProperty`로 bean 등록 자체를 막는다.
- 외부 API, webhook, LLM처럼 지연 시간이 긴 I/O는 scheduler thread에서 직접 오래 점유하지 않는다. 필요하면 event listener나 전용 executor로 분리한다.
- outbox listener에서 외부 I/O를 동기 실행해야 하면 `NON_TRANSACTIONAL` dispatch를 사용해 DB connection 점유를 피하고 예외를 relay 재시도로 연결한다.
- 도메인별 1회성 동적 task가 필요하면 전역 `taskScheduler`를 공유할지 전용 `TaskScheduler`가 필요한지 먼저 판단한다.
- 다중 인스턴스에서 중복 실행되면 안 되는 작업은 DB lease, unique constraint, outbox claim, ShedLock 중 하나로 방어한다.
- 새 작업은 실행 주기, 활성 property, 멱등성 전략, 실패 재시도 전략을 이 문서에 추가한다.
