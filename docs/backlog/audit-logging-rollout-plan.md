# 감사 로그 rollout 현행화 및 후속 계획

> 기준일: 2026-07-13
>
> 이 문서는 2026-05 초안의 희망 상태를 현재 코드·테스트·설정과 대조해 다시 정리한 운영 backlog다. 구현된 것과 후속 과제를 분리한다. 실행 순서와 TODO 상태의 source of truth는 [audit-log-management Prometheus 계획](../../.omo/plans/audit-log-management.md)이며, 이 문서가 계획 체크박스나 ledger를 대신하지 않는다.

## 1. 감사 로그의 책임과 경계

감사 로그는 `actorMemberId`, `action`, `targetType`, `targetId`, `createdAt`을 중심으로 행위 사실을 남기고, `outcome`, `source`, `requestId`, `traceId`와 당시 snapshot을 더해 사후 판단을 돕는다. ID는 조회·상관관계 축이고, snapshot은 대상이 삭제되거나 변경된 뒤에도 당시 의미를 설명하는 근거다.

감사 로그는 현재 도메인 상태의 source of truth, 도메인 권한 결정기, 일반 장애 로그, metric, 원문 요청/응답 저장소가 아니다. audit 도메인이 다른 도메인의 repository를 직접 조회하지 않으며, 발생 도메인이 공개 Query UseCase로 필요한 값을 읽어 payload를 조립한다. 대상이 있는 이벤트는 `targetId`를 보존하고, 대상이 없는 이벤트의 `targetId`는 구현 계약상 `null`일 수 있다.

FE와 운영 도구는 삭제된 참조의 과거 표시를 현재 도메인 재조회로 재구성하지 않는다. 저장된 snapshot을 신뢰하고, snapshot이 없는 legacy row는 `과거 snapshot 없음`으로 표시한다.

## 2. 현재 구현 계약

### 2.1 저장 모델과 migration

| 계약 | 현재 구현 | 직접 근거 |
| --- | --- | --- |
| 기존 필드 | `domain`, `action`, `targetType`, `targetId`, `actorMemberId`, `description`, `details(jsonb)`, `ipAddress`, `createdAt` | [`AuditLog`](../../src/main/java/com/umc/product/audit/domain/AuditLog.java), [`V2026.03.12.01.00__create_audit_log.sql`](../../src/main/resources/db/migration/V2026.03.12.01.00__create_audit_log.sql) |
| 결과/출처 | `AuditOutcome.SUCCESS/FAILURE`, `AuditSource.ANNOTATION/EXPLICIT_RECORDER/AUTHENTICATION_SERVICE/AUTHORIZATION_ASPECT/SYSTEM` | [`AuditOutcome`](../../src/main/java/com/umc/product/audit/domain/AuditOutcome.java), [`AuditSource`](../../src/main/java/com/umc/product/audit/domain/AuditSource.java) |
| 추적 필드 | `request_id`, `trace_id` nullable, 최대 100자 | [`V2026.07.13.11.10__extend_audit_log_schema.sql`](../../src/main/resources/db/migration/V2026.07.13.11.10__extend_audit_log_schema.sql), [`AuditLogSchemaMigrationContractTest`](../../src/test/java/com/umc/product/audit/adapter/out/persistence/AuditLogSchemaMigrationContractTest.java) |
| legacy 기본값 | 기존 row/insert가 신규 필드를 생략하면 `outcome=SUCCESS`, `source=ANNOTATION`, `request_id/trace_id=null` | migration 및 위 schema contract test |
| 조회 인덱스 | outcome/source+createdAt, target+createdAt, request_id, trace_id와 기존 인덱스 | 신규 migration 및 schema contract test |

`details` 컬럼 타입은 바꾸지 않았다. Java와 HTTP 계약의 `details`는 `String`이며 값이 없으면 `null`이다. JSON object를 HTTP object로 바꾼 구현이 아니다.

### 2.2 details schema와 민감정보

새 표준 root는 `schemaVersion: 1`, `actor`, `target`, `context`, `before`, `after`다. [`AuditDetailsPolicy`](../../src/main/java/com/umc/product/audit/domain/AuditDetailsPolicy.java)와 [`AuditLegacyDetailsSanitizer`](../../src/main/java/com/umc/product/audit/domain/AuditLegacyDetailsSanitizer.java)가 legacy Map을 표준 shape으로 정규화하고, 허용 section/key만 남긴다. `null`·blank는 제거되고 문자열은 255자로 제한된다. 허용 key의 값이라도 `token`/`password`/`body`/`authorization`/`bearer`/`secret` marker 또는 CR/LF·로그 위조용 개행을 포함하면 원문 대신 고정 `[REDACTED]`로 대체하며, 일반 표시 snapshot은 보존한다.

허용 key는 다음과 같다.

| section | allowlist |
| --- | --- |
| `actor` | `type`, `memberId`, `name`, `nickname`, `schoolName` |
| `target` | `type`, `id`, `memberId`, `name`, `nickname`, `schoolName`, `status`, `title`, `roleName`, `period`, `term`, `round`, `recordType` |
| `context` | `requestId`, `traceId`, `outcome`, `source`, `reason`, `resourceType`, `resourceId`, `permission` |
| `before`, `after` | target와 같은 snapshot/state key |

다음 key는 표기 변형을 포함해 `details`의 어떤 section에서도 금지한다: `email`, `password`, `token`, `accessToken`, `refreshToken`, `idToken`, `authorization`, `authorizationHeader`, `providerId`, `oauthSubject`, `code`, `verificationCode`, `authCode`, `oneTimeCode`, `otpCode`, `rawBody`, `content`, `body`.

`description`은 `details`와 별개의 `TEXT` 표시 필드지만 무보호 원문이 아니다. 호출부는 동적 회원명·nickname·학교명·credential·token·body를 결합하지 않고 정적 문장을 사용한다. 저장 경계의 [`AuditDescriptionPolicy`](../../src/main/java/com/umc/product/audit/domain/AuditDescriptionPolicy.java)는 NFKC/공백 정규화와 500자 제한을 적용하고, 줄바꿈 또는 `name`/`nickname`/`school`/`password`/`token`/`authorization`/`body`/`content` 계열 marker가 있으면 안전한 기본 문장으로 대체한다. 반면 제한된 운영자 화면에서 필요한 당시 name/nickname/school snapshot은 구조화된 `details` allowlist 안에서만 보존한다.

직렬화 실패는 [`AuditLogCommandService`](../../src/main/java/com/umc/product/audit/application/service/command/AuditLogCommandService.java)가 warning을 남기고 details를 null로 격리한다. 저장 실패는 recorder/listener가 metric과 error log를 남기고 호출자의 비즈니스 결과를 깨뜨리지 않는다.

### 2.3 두 기록 경로의 선택

| 상황 | 선택 | 현재 동작 |
| --- | --- | --- |
| request/response에 이미 있는 target ID, result ID, 짧은 설명 같은 단순 성공 메타데이터 | `@Audited` + `AuditAspect` | `@AfterReturning`에서 `SUCCESS/ANNOTATION`; details SpEL 속성은 없고 숨은 조회를 하지 않음 |
| rich snapshot, 삭제 전 snapshot, 실패 자체를 보존해야 하는 이벤트 | `RecordAuditLogUseCase` + `RecordAuditLogCommand` | `SUCCESS`는 transaction-bound event로 발행하고 `FAILURE`만 `REQUIRES_NEW`로 저장 |

신규 코드는 두 번째 상황에 `@Audited`의 SpEL을 확장해 해결하지 않는다. snapshot은 삭제·롤백 전에 조립하고, ID와 당시 표시값을 함께 전달한다. 명시 recorder의 `source=ANNOTATION`은 command validation에서 거부된다.

커밋 경계는 성공과 실패를 의도적으로 분리한다. 트랜잭션 안에서 발행된 `@Audited` 이벤트와 명시 recorder의 `SUCCESS`는 비즈니스 변경과 함께 `event_outbox`에 저장되므로 외부 비즈니스 트랜잭션이 롤백되면 발행 대상이 남지 않는다. 커밋된 `AuditLogEvent`는 `NON_TRANSACTIONAL` relay에서 감사 행을 저장하며, 저장이 성공한 뒤에만 outbox를 완료한다. 명시 recorder의 `FAILURE`만 `AuditLogNewTransactionWriter(REQUIRES_NEW)`로 저장되어 외부 예외·롤백과 독립적으로 보존된다.

일부 메서드는 annotation과 명시 recorder를 함께 사용한다. 정상 커밋 뒤 `SUCCESS/EXPLICIT_RECORDER`와 `SUCCESS/ANNOTATION` 행이 relay를 통해 순차적으로 나타날 수 있어 저장 순서는 계약하지 않는다. 대량 생성은 명시 행이 여러 개일 수 있다. 이를 자동 중복 또는 일대일 행위로 단정하지 말고 `source`, `details`, `requestId`/`traceId`, 대상·행위자·시각을 함께 비교한다. 현재 persisted eventId/deduplication key는 없다.

현재 직접 recorder를 사용하는 코드는 인증 실패, 접근 거부, 챌린저 기록 생성/대량 생성/코드 사용, 커뮤니티 신고다. 회원 가입/탈퇴, 챌린저 역할, 일정·참여·출석의 rich 성공 이벤트는 현재 일부가 `DomainEventPublisher`로 `AuditLogEvent`를 발행한다. 이 호환 경로는 코드에 존재하지만, 신규 구현의 기준은 위 표의 `RecordAuditLogUseCase`다.

### 2.4 현재 적용 범위

다음은 코드와 테스트로 확인한 적용 범위다. 이 표에 없는 모든 state-changing method가 자동으로 rich audit된다고 해석하지 않는다.

| 영역 | 현재 확인된 동작 | 근거 |
| --- | --- | --- |
| 자동 성공 audit | 여러 command service의 `@Audited` 선언, 서버 MDC `traceId`를 requestId로 사용, 현재 OpenTelemetry trace와 raw socket remote IP 추출. client `X-Request-Id`/`X-Forwarded-For`는 신뢰하지 않음 | [`AuditAspect`](../../src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java), [`AuditRequestContextProvider`](../../src/main/java/com/umc/product/global/logging/AuditRequestContextProvider.java), 관련 contract tests |
| 인증 실패 | `LOGIN`, `FAILURE`, `AUTHENTICATION_SERVICE`, direct recorder, 민감정보 없는 details | [`CredentialAuthenticationService`](../../src/main/java/com/umc/product/authentication/application/service/CredentialAuthenticationService.java), [`AuthenticationFailureAuditIntegrationTest`](../../src/test/java/com/umc/product/authentication/application/service/AuthenticationFailureAuditIntegrationTest.java) |
| 접근 거부 | `ACCESS_DENIED`, `FAILURE`, `AUTHORIZATION_ASPECT`, resource/permission snapshot | [`AccessControlAspect`](../../src/main/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspect.java), [`AuthorizationFailureAuditIntegrationTest`](../../src/test/java/com/umc/product/authorization/adapter/in/aspect/AuthorizationFailureAuditIntegrationTest.java) |
| 회원/역할 | 회원 가입·탈퇴 및 역할 create/update/delete에 당시 member/role snapshot을 만드는 코드가 있음 | [`MemberAuditEventFactory`](../../src/main/java/com/umc/product/member/application/service/MemberAuditEventFactory.java), [`ChallengerRoleAuditEventFactory`](../../src/main/java/com/umc/product/authorization/application/service/command/ChallengerRoleAuditEventFactory.java), 관련 rich audit tests |
| 일정/출석 | 일정 create/update/delete/forceDelete, 참여, 출석 요청·공결·approve/reject event factory가 있음 | [`ScheduleAuditEventFactory`](../../src/main/java/com/umc/product/schedule/application/service/command/ScheduleAuditEventFactory.java), [`ScheduleCommandService`](../../src/main/java/com/umc/product/schedule/application/service/command/ScheduleCommandService.java), [`ScheduleParticipantCommandService`](../../src/main/java/com/umc/product/schedule/application/service/command/ScheduleParticipantCommandService.java) |
| 챌린저/신고 | 챌린저 기록 create/bulk/consume와 신고는 snapshot을 조립해 명시 recorder를 호출함. 챌린저 기록 delete는 현재 annotation ID 로그임 | [`ChallengerRecordCommandService`](../../src/main/java/com/umc/product/challenger/application/service/ChallengerRecordCommandService.java), [`ReportCommandService`](../../src/main/java/com/umc/product/community/application/service/command/ReportCommandService.java) |
| 조회 | admin endpoint가 기존 필터와 target/outcome/source/request/trace 필터를 모두 전달하고 exact AND 조건으로 조회 | [`AuditLogController`](../../src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java), [`AuditLogQueryRepository`](../../src/main/java/com/umc/product/audit/adapter/out/persistence/AuditLogQueryRepository.java), controller/query integration tests |

## 3. 운영 rollout

### 3.1 outbox 저장, 장애, 재처리

`OutboxDomainEventPublisher`는 항상 활성화되며 local publisher 우회 설정은 없다. `app.event-outbox.relay-enabled=${EVENT_OUTBOX_RELAY_ENABLED:true}`로 poller만 제어하며, 기본 poll interval은 1초, batch는 100, max attempts는 5다. relay는 processing lease 5분, 5초부터 최대 5분 backoff를 사용한다.

annotation과 명시 recorder `SUCCESS`는 모두 outbox를 사용한다. `relay-enabled=false`여도 write는 계속되어 `PENDING`이 적체된다. 명시 recorder `FAILURE`의 `REQUIRES_NEW` 저장만 outbox를 우회한다.

운영 판단은 다음과 같이 한다.

1. audit 저장 실패 alert가 오면 application log, DB 연결/constraint, `audit_log` 최근 row를 먼저 확인한다.
2. `event_outbox`의 `PENDING`, lease가 지난 `PROCESSING`, `FAILED`, `last_error`를 함께 확인한다.
3. `FAILED`는 max attempts에 도달한 상태다. 현재 audit 전용 replay/reset endpoint와 outbox backlog metric은 없으므로, 임의 SQL 수정 대신 승인된 재처리 절차를 마련하기 전에는 원인·event id를 보존하고 운영자 판단을 받는다.
4. `RecordAuditLogUseCase` 직접 경로는 outbox 설정과 무관하게 `REQUIRES_NEW`로 실패 이벤트를 남긴다. 실패 로그 보존을 outbox 기본값에 의존하지 않는다.

### 3.2 metric과 alert

현재 metric은 다음처럼 low-cardinality tag만 사용한다.

- `operational.audit.log.total{domain,action,outcome,source}`
- `operational.audit.log.failure.total{domain,action,reason}`

`targetId`, `requestId`, `traceId`는 tag가 아니다. [`AuditLogSaveFailures`](../../infra/monitoring/grafana/config/prometheus/rules/default-alerts.yml)는 최근 5분 failure counter 증가를 즉시 warning으로 알린다. 이 rule은 audit 저장 실패를 관측하며, outbox relay 자체의 backlog·재처리 성공률까지 관측하는 rule은 아니다.

처리 단계 실패 reason은 `details_serialization`, `spel_evaluation`, `event_publish`, `context_extraction`의 고정값만 사용한다. request/target/trace/exception message를 reason tag로 넣지 않는다.

### 3.3 retention·권한·무결성의 현재 범위

| 주제 | 현재 범위 | 후속 과제 |
| --- | --- | --- |
| retention | `audit_log` retention 기간, archive, cleanup scheduler 없음. monitoring stack retention과 audit row retention은 별개 | 법적/업무 retention 결정, archive 대상·삭제 승인·파티셔닝 설계 |
| 권한 | `AUDIT/READ`만 [`AuditLogPermissionEvaluator`](../../src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java)가 중앙운영사무국 국원인지 평가. audit 조회 write API 없음 | DB application role의 UPDATE/DELETE 차단, 역할 분리, audit 조회 자체의 audit 여부 결정 |
| 무결성 | entity는 public setter가 없고 `createdAt`은 JPA `updatable=false`지만 DB-level append-only는 아님. hash chain/WORM/SIEM 없음 | DB 권한, tamper evidence, hash/WORM/외부 전송 필요성 검토 |

### 3.4 FE 계약

FE는 canonical `GET /api/v1/audit/admin/audit-logs`의 exact filter를 사용한다. legacy alias는 제공하지 않는다. 응답의 `requestId`, `traceId`, `details`는 legacy row에서 null/생략 가능하며, 지원 버전 `schemaVersion === 1`만 구조화한다. 과거 표시를 위해 현재 회원·일정 API를 재조회하지 않는다. 상세한 payload와 파싱 예시는 [감사 로그 FE API 가이드](../guides/감사_로그_FE_API_가이드.md)를 따른다.

## 4. 후속 작업 우선순위

### P0: 운영 안전선

- rich/delete/failure 신규 기록을 `RecordAuditLogUseCase` 기준으로 통일하고, 현재 `DomainEventPublisher` rich 경로의 전달/rollback 및 `@Audited`와 명시 recorder가 공존하는 메서드의 행 수·source 해석 테스트를 유지한다.
- `event_outbox` backlog, relay exception, `FAILED` row에 대한 metric·alert·승인된 replay runbook을 추가한다.
- details allowlist/denylist와 `schemaVersion` contract test를 모든 신규 snapshot factory에 적용한다.

### P1: coverage와 조회 운영성

- 아직 annotation ID만 남는 삭제/상태 변경을 도메인별로 inventory하고 삭제 전 snapshot을 추가한다.
- 현재 적용되지 않은 batch/member state change를 자동으로 completed로 표시하지 말고, 각 use case의 성공·실패·rollback을 별도 테스트한다.
- FE가 `schemaVersion`을 분기하고 snapshot 부재를 명시적으로 표시하도록 가이드와 화면 계약을 함께 검토한다.

### P2: 보존과 무결성

- retention 기간과 법적 근거를 확정한 뒤 archive/cleanup 권한과 복구 절차를 구현한다.
- application DB role의 INSERT-only 권한, 별도 archive/admin role, tamper-evident hash/WORM/SIEM 필요성을 ADR로 결정한다.
- audit log 자체 조회를 다시 audit할지, export·본문 검색·실시간 stream을 제공할지 사용 사례를 확인한다.

## 5. 구현되지 않은 제안의 명시적 제외

다음은 과거 초안에 있었지만 현재 코드가 제공한다고 쓰면 안 되는 항목이다.

- `@Audited(details = "...")`, `logFailure`, `LOGIN_FAILURE` 같은 별도 action enum 기반 annotation 확장
- 3년 보관, cold archive, hard delete scheduler
- PostgreSQL `REVOKE UPDATE, DELETE`를 통한 DB-level append-only
- 암호학적 hash chain, WORM, 외부 SIEM
- outbox `FAILED` row를 위한 audit 전용 replay/reset API
- description like 검색, CSV export, 실시간 audit stream

이 항목들은 필요성이 확인된 뒤 별도 구현·테스트·운영 승인으로 진행한다.

## 6. 완료 판정

문서와 운영자가 “현재 무엇이 보장되는가”를 판단할 때 다음을 기준으로 한다.

- `targetId`와 당시 snapshot을 분리해 저장한다.
- 삭제된 참조의 과거 표시에는 저장된 snapshot만 사용한다.
- `@Audited`는 단순 성공 메타데이터, rich/delete/failure는 명시 recorder라는 선택 기준을 지키되, 기존 일부 메서드의 두 경로 공존을 알고 `outcome`만으로 rollback 생존 여부나 행 중복을 추론하지 않는다.
- details는 schemaVersion 1 표준 JSON 문자열 또는 legacy null/생략을 허용한다.
- allowlist 밖 key와 민감 denylist를 저장하지 않는다.
- 조회 필터와 응답 필드는 실제 controller/query/DTO 계약과 일치한다.
- outbox 필수 저장과 relay 제어, 장애·재처리·metric·alert의 한계를 운영 문서에 명시한다.
- retention, 권한, 무결성 기능의 현재 미구현 상태와 후속 과제를 구분한다.
