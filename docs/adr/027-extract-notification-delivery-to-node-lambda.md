# ADR-027: Notification 전달을 Node.js Lambda로 분리한다

## Status

Accepted

## Context

2026년 7월 기준 UMC PRODUCT의 FCM, 인증 이메일, 채용 이메일, business Webhook 전달은
Java/Spring Boot 프로세스와 같은 배포 단위에서 실행된다. 공개 API
`POST /api/v1/notifications/fcm/installations`,
`DELETE /api/v1/notifications/fcm/installations/{installationId}`,
`POST /api/v1/notifications/admin/fcm/messages`도 같은 서버가 제공한다.

[ADR-019](./019-introduce-transactional-event-outbox.md)와
[ADR-026](./026-enforce-event-outbox-publisher.md)에 따라 최초 이벤트는 PostgreSQL
`event_outbox`에 내구성 있게 저장되지만, relay 이후 provider 호출과 재시도 정책은 Java listener에
결합되어 있다. 알림 트래픽 급증이 API 서버의 thread, memory, 배포 주기에 영향을 주며 채널별
timeout, concurrency, DLQ 정책을 독립적으로 운영하기 어렵다.

다음 제약을 동시에 지켜야 한다.

1. 인증·권한·대상자 결정은 기존 Java 도메인의 책임으로 유지한다.
2. 공개 API 경로와 응답 형식을 변경하지 않는다.
3. `adapter/in -> application/port/in`과 도메인 간 public UseCase 경계를 유지한다.
4. integration event는 at-least-once 전달이므로 producer와 consumer 모두 멱등성을 고려한다.
5. 기존 PostgreSQL FCM token은 DynamoDB로 backfill하지 않는다. 앱 재등록률이 전환 조건이다.
6. production 전환 전까지 Java provider를 실제 전달 경로로 유지할 수 있어야 한다.

### 문제점

1. **확장 단위 결합**: FCM 2만 명 이상의 fan-out이 Java API 인스턴스와 같은 자원을 사용한다.
   provider 지연이나 재시도가 API latency와 배포 안정성에 영향을 줄 수 있다.
2. **채널별 실패 격리 부족**: FCM, SES, Webhook은 서로 다른 timeout과 retryable error를 가지지만
   공통 process lifecycle 안에서 운영된다. 채널별 queue age와 DLQ를 독립적으로 제어하기 어렵다.
3. **token ownership 경쟁**: 여러 installation이 같은 raw FCM token을 등록할 때 PostgreSQL 갱신
   순서만으로 현재 owner를 원자적으로 보장하기 어렵다.
4. **점진 전환 필요**: 앱이 installation API를 다시 호출하지 않으면 새 DynamoDB에는 전달 대상이
   없다. 서버 전환만 먼저 수행하면 기존 사용자에게 알림이 누락된다.

### 결정이 필요한 이유

앱 선배포, token 재등록 관찰, shadow 검증, ASG 호환 버전 배포가 순서대로 필요하다. event 계약과
인프라 경계를 먼저 고정하지 않으면 각 단계가 서로 다른 payload와 rollback 방식을 사용하게 된다.

## Decision

우리는 Notification provider 전달을 `services/notification`의 Node.js 24/TypeScript Lambda로
분리하고, Java 서버를 인증된 Gateway와 integration event producer로 유지하기로 결정한다.

1. `IntegrationEvent`만 EventBridge custom bus로 전달한다. 일반 `DomainEvent`는 기존 Spring
   dispatch를 유지한다. EventBridge `PutEvents`가 수락한 후에만 outbox를 `PUBLISHED` 처리한다.
2. Java는 FCM 대상 `memberIds`를 확정하고 500명 단위
   `notification.fcm.requested.v1` event를 기록한다. Lambda는 member, organization,
   challenger 도메인을 동기 조회하지 않는다.
3. FCM installation 등록·해제는 JWT 검증 후 Java가 IAM `RequestResponse` 방식으로
   `fcm-installation-command` Lambda를 호출한다. Notification Lambda는 공개 HTTP endpoint를
   제공하지 않는다.
4. EventBridge는 event type을 채널별 SQS로 분류한다. 각 queue는 별도 DLQ와
   `ReportBatchItemFailures`를 사용한다.
5. installation과 token owner는 DynamoDB
   `NotificationInstallations`, `NotificationTokenOwners`에 저장한다. raw token이 아닌
   SHA-256 `tokenHash`를 owner key로 사용하고 `TransactWriteItems`로 이전 owner 비활성화와
   신규 owner 등록을 원자화한다.
6. `NotificationDeliveries`는 `eventId` 기준 처리 lease, 상태, attempt, provider message ID만
   30일간 저장한다. SES 수락 뒤 결과 event 발행이 실패하면 `RESULT_PENDING`으로 남겨 provider를
   다시 호출하지 않고 결과만 재발행한다. 원문 payload와 raw token은 저장하지 않는다.
7. 이메일은 `verification`, `recruiting-interview-availability` 두 고정 template만 허용한다.
   채용 메일의 SES 수락/최종 실패 결과는 result SQS를 통해 Java
   `ManageRecruitingInterviewMailDeliveryUseCase`로 반영한다.
8. Java 설정 `app.notification.transport`는 `local`, `shadow`, `external` 세 값을 가진다.
   `shadow`는 Java가 실제 발송하고 Lambda는 `VALIDATE`, `external`은 Lambda만 사용한다.
9. Firebase service account와 Webhook credential은 Secrets Manager에 두고, EventBridge, SQS,
   DynamoDB, SNS는 stack별 KMS key로 암호화한다.

### 단계적 진행 / PR 분할

- **Phase 1 (본 ADR/구현)**: AsyncAPI 계약, Node handlers, SAM stack, Java integration event relay,
  transport mode, 공통 fixture와 단위 테스트를 추가한다. 모든 기본값은 `local`과
  `DeliveryEnabled=false`다.
- **Phase 2 (앱 선배포)**: 앱 실행·로그인·token refresh 시 installation API를 호출한다. DB
  backfill은 하지 않는다.
- **Phase 3 (shadow)**: token Lambda는 DynamoDB에 실제 기록하고 provider worker는 `VALIDATE`로
  계약, template, Webhook payload만 검증한다.
- **Phase 4 (production cutover)**: 재등록률과 부하 조건을 충족한 후 Java를 `external`로 바꾸고
  FCM, 이메일, Webhook event source mapping을 한 번에 활성화한다.
- **Phase 5 (정리)**: 14일 안정화 후 Java provider 구현과 PostgreSQL FCM token 경로를 별도 ADR/PR로
  제거한다.

## Alternatives Considered

### 대안 A: Java 모놀리스에서 provider thread pool만 분리

장점:

- 신규 runtime과 AWS 리소스가 필요 없다.
- 기존 JPA token 데이터와 template을 그대로 사용한다.

단점:

- 배포, 확장, 장애 격리 단위가 여전히 API 서버다.
- 채널별 DLQ와 queue age를 별도 운영하기 어렵다.

선택하지 않은 이유:

- thread pool 분리는 process 장애와 배포 결합을 해소하지 못하고, fan-out 트래픽을 API ASG와 함께
  확장해야 한다.

### 대안 B: 장기 실행 ECS/Fargate Notification 서버

장점:

- gRPC와 long-lived connection, in-memory batching을 자연스럽게 사용할 수 있다.
- Lambda timeout과 cold start 제약이 없다.

단점:

- 현재 notification workload는 event-driven이며 idle 시간에도 task 비용과 운영 책임이 발생한다.
- queue consumer autoscaling, 배포, health check를 별도로 구성해야 한다.

선택하지 않은 이유:

- 현재 범위에는 bidirectional streaming이나 장기 연결이 필요하지 않다. SQS fan-out worker는
  Lambda 실행 모델과 더 잘 맞는다.

### 대안 C: 기존 PostgreSQL token을 DynamoDB로 일괄 backfill

장점:

- 앱 재등록률을 기다리지 않고 빠르게 전환할 수 있다.

단점:

- 오래되었거나 무효인 token과 installation ownership까지 복사한다.
- 동일 token의 현재 owner를 결정하는 migration 규칙과 개인정보 이동 절차가 필요하다.

선택하지 않은 이유:

- 앱의 인증된 재등록을 source of truth로 삼으면 활성 installation만 자연스럽게 누적된다. 대신
  95% 재등록률을 production 전환의 필수 조건으로 둔다.

### 대안 D: Lambda가 member/organization 서비스를 gRPC로 조회

장점:

- event payload가 작고 대상 조건을 consumer에서 재사용할 수 있다.

단점:

- VPC/network 장애와 도메인 서비스 가용성이 Notification fan-out의 동기 의존성이 된다.
- 발행 시점과 소비 시점 사이 대상 집합이 달라져 요청 의미가 불안정해진다.

선택하지 않은 이유:

- 알림 수신 대상은 권한과 조직 규칙을 가진 producer가 요청 시점에 확정해야 한다. integration
  event에는 확정된 ID만 담는다.

## Consequences

### Positive

- provider 지연과 burst가 SQS에 흡수되어 API 서버와 격리된다.
- FCM, 이메일, Webhook이 독립 concurrency, retry, DLQ, alarm을 가진다.
- token ownership과 installation 갱신이 DynamoDB transaction으로 원자화된다.
- Java API 계약과 인증/권한 정책을 변경하지 않고 전달 runtime만 교체할 수 있다.
- `local/shadow/external`과 비활성 event source mapping으로 rollback 단위를 분리한다.

### Negative

- Node.js와 SAM, EventBridge, SQS, DynamoDB, KMS를 추가로 운영해야 한다.
- 전달 보장은 at-least-once이며 provider 수락 직후 장애 시 중복 가능성이 남는다.
- 앱 재등록률이 95%에 도달하기 전에는 production 전환할 수 없다.
- result SQS를 polling하는 Java consumer와 해당 IAM 권한을 ASG role에 추가해야 한다.
- local과 external 계약을 동시에 이해하는 기간 동안 코드와 운영 복잡도가 증가한다.

### Neutral / Trade-offs

- FCM API 요청 완료는 provider 단말 도달이 아니라 queue 접수를 의미한다.
- Lambda 간 동기 호출은 사용하지 않지만, 인증된 installation command만 Java Gateway에서
  동기 호출한다.
- EventBridge archive는 활성화하지 않는다. replay는 SQS 격리 queue와 delivery ledger를 기준으로
  수행한다.

## Implementation Notes

### 변경 영역 요약

1. **응용 / Port**: `IntegrationEvent`, `PublishIntegrationEventPort`,
   `ManageExternalFcmInstallationPort`, 채널별 integration event를 추가한다.
2. **어댑터 (in)**: Java result SQS poller와 기존 공개 FCM controller를 사용한다.
3. **어댑터 (out)**: EventBridge publisher와 IAM Lambda installation adapter를 추가한다.
4. **설정 / 환경**: `app.notification.transport`, EventBridge, Lambda, result consumer 설정을
   `application.yml`에 추가한다.
5. **Node 서비스**: `services/notification/src/handlers`의 6개 plain handler가 FCM installation,
   resolver, batch sender, validator, SES, Webhook을 담당한다.
6. **인프라**: `infra/notification/template.yaml`이 bus, queue/DLQ, DynamoDB, KMS, Lambda, schedule,
   log retention, alarm을 생성한다.
7. **계약**: `contracts/notification/asyncapi.yaml`과
   `contracts/notification/fixtures/fcm-requested.v1.json`을 Java/Node가 함께 검증한다.

### 기타 참고

- 배포와 cutover 절차는
  [Notification serverless cutover](../onboarding/notification-serverless-cutover.md)를 따른다.
- Java ASG role에는 notification event bus `events:PutEvents`, installation function
  `lambda:InvokeFunction`, result queue `sqs:ReceiveMessage/DeleteMessage/GetQueueAttributes`,
  관련 KMS `Decrypt/GenerateDataKey` 권한이 필요하다.
- secret 값, raw token, 이메일 주소, 인증 코드, Webhook URL, 원문 payload를 로그에 기록하지 않는다.

## References

- [ADR-018](./018-abstract-spring-event-publisher-for-future-broker.md)
- [ADR-019](./019-introduce-transactional-event-outbox.md)
- [ADR-026](./026-enforce-event-outbox-publisher.md)
- `contracts/notification/asyncapi.yaml`
- `infra/notification/template.yaml`
