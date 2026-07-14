# Logging Onboarding

이 문서는 일반 로그, metric, audit log의 책임을 구분하는 온보딩 기준이다. 감사 로그의 현재 계약은 [감사 로그 관리 계획](../../.omo/plans/audit-log-management.md)과 실제 코드·테스트를 함께 기준으로 한다. 기준일은 2026-07-13이다.

## 읽는 순서

1. [domains.md](./domains.md): 도메인별 일반 로그 인벤토리
2. 이 문서: 감사 로그를 기록할 때의 책임, snapshot, 민감정보, 운영 기준
3. [감사 로그 FE API 가이드](../../docs/guides/감사_로그_FE_API_가이드.md): 관리자 조회 API 계약
4. [감사 로깅 rollout 후속 계획](../../docs/backlog/audit-logging-rollout-plan.md): 구현 현황과 남은 과제

## 일반 로그와 audit log의 경계

일반 애플리케이션 로그는 장애 원인, 외부 연동, 배치 실행 상태를 진단한다. 반복 집계가 목적이면 `OperationalMetrics`를 사용한다. audit log는 다음과 같은 **사후 설명에 필요한 행위 사실**을 보존한다.

### Audit log가 책임지는 것

- 누가(`actorMemberId`), 무엇을(`action`), 어떤 대상에(`targetType` + `targetId`), 언제(`createdAt`) 했는지 기록한다.
- 결과(`outcome`), 기록 경로(`source`), 요청/분산 추적 식별자(`requestId`, `traceId`)를 보존한다.
- 대상 ID와 함께 그 시점의 표시명·소속·상태 등 허용된 snapshot을 `details`에 저장한다.
- 중앙운영사무국 국원이 감사 로그를 조건별로 조회할 수 있게 한다.

### Audit log가 책임지지 않는 것

- 현재 도메인 상태의 source of truth가 아니다. 과거 행위를 설명하는 증거이며 현재 회원·일정 데이터를 대체하지 않는다.
- 감사 로그가 다른 도메인의 repository를 조회하거나, 현재 데이터를 재구성하거나, 도메인 권한 정책을 결정하지 않는다.
- 구조화된 `details`에는 원문 요청/응답 body, 비밀번호·token, 게시글/신고 본문을 넣지 않는다. `description`은 정적 문장만 생성하고 저장 경계의 별도 sanitizer를 거친다.
- 현재 구현은 DB role 수준의 append-only 강제, 암호학적 hash chain, WORM 저장소, 외부 SIEM 연동, `audit_log` retention/archival을 제공하지 않는다.
- 비동기 이벤트 경로의 즉시 조회 또는 exactly-once 전달을 보장한다고 가정하지 않는다.

## 감사 기록 경로 선택 기준

### `@Audited`는 단순 성공 메타데이터에만 사용한다

[`Audited`](../../src/main/java/com/umc/product/audit/application/port/in/annotation/Audited.java)는 application service의 정상 종료 성공 이벤트에 붙인다. 현재 어노테이션 속성은 `domain`, `action`, `targetType`, `targetId`, `description`이며 `details` SpEL 속성은 없다.

- `targetId`처럼 request/response에 이미 있는 단순 값을 SpEL로 참조한다. `description`에는 name/nickname/school/body/token 같은 동적 값을 결합하지 않고 정적 문장을 사용한다.
- `@auditRepository.getById(...)` 같은 bean/repository 조회, 숨은 쿼리, rich object 조립을 SpEL에 넣지 않는다. 평가 실패는 요청을 깨뜨리지 않고 audit event를 발행하지 않는다.
- [`AuditAspect`](../../src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java)는 성공 이벤트에 `outcome=SUCCESS`, `source=ANNOTATION`을 기본 적용한다. 실패 분기는 `@Audited`가 자동 기록하지 않는다.

트랜잭션 안에서 발행된 annotation event와 명시 recorder `SUCCESS`는 비즈니스 변경과 함께 `event_outbox`에 저장되고, 커밋된 event만 relay된다. `AuditLogEvent`는 `NON_TRANSACTIONAL` dispatch를 사용하며 listener가 감사 행 저장을 완료한 뒤 outbox를 `PUBLISHED` 처리한다. 저장 실패는 relay 재시도 대상으로 남고 원 비즈니스 트랜잭션에는 영향을 주지 않는다. 명시 recorder `FAILURE`만 `REQUIRES_NEW`로 외부 롤백과 독립 보존한다.

### `RecordAuditLogUseCase`는 rich/delete/failure snapshot에 사용한다

새 코드에서 다음 조건이면 [`RecordAuditLogUseCase`](../../src/main/java/com/umc/product/audit/application/port/in/command/RecordAuditLogUseCase.java)를 사용한다.

- 표시명·소속·상태·이전/이후 값처럼 request/response의 단순 값만으로 부족한 rich snapshot이 필요한 경우
- 삭제 전에 대상을 읽어 두어야 하는 경우. 삭제 후 현재 도메인을 조회해 과거 표시를 만들 수 없으므로 snapshot을 먼저 확보한다.
- 로그인 실패·접근 거부처럼 예외/롤백과 독립적으로 남겨야 하는 failure event인 경우

[`RecordAuditLogService`](../../src/main/java/com/umc/product/audit/application/service/command/RecordAuditLogService.java)는 `SUCCESS`를 transaction-bound event로 발행하고 `FAILURE`만 [`AuditLogNewTransactionWriter`](../../src/main/java/com/umc/product/audit/application/service/command/AuditLogNewTransactionWriter.java)의 `REQUIRES_NEW`로 저장한다. 처리 실패는 호출자에게 전파하지 않고 metric/log로 남긴다. 명시 recorder는 `ANNOTATION` source를 사용할 수 없다.

회원 가입/탈퇴, 역할 변경, 일정·출석을 포함한 rich 성공 이벤트도 같은 outbox commit-bound 경로를 따른다. relay는 [`AuditLogEventListener`](../../src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java)를 동기 호출하고, listener가 감사 행을 저장한 뒤에만 outbox event를 완료한다.

일부 메서드는 `@Audited`와 명시 recorder를 모두 호출한다. 정상 커밋 뒤 두 성공 행이 relay를 통해 순차적으로 나타날 수 있고 저장 순서는 계약하지 않는다. 자동 중복 또는 일대일 행위 증거로 단정하지 말고 `source`, `details`, 추적값, 대상·행위자·시각을 함께 비교한다.

요청 context는 [`AuditRequestContextProvider`](../../src/main/java/com/umc/product/global/logging/AuditRequestContextProvider.java) 한 곳에서 추출한다. `requestId`는 서버 MDC `traceId`, `traceId`는 현재 OpenTelemetry span, IP는 wrapper를 벗긴 servlet request의 raw remote address를 사용한다. client가 보낸 `X-Request-Id`와 `X-Forwarded-For`는 spoof 가능한 값이므로 감사 식별자나 IP로 신뢰하지 않는다.

직접 recorder가 적용된 현재 사례는 인증 실패([`CredentialAuthenticationService`](../../src/main/java/com/umc/product/authentication/application/service/CredentialAuthenticationService.java)), 접근 거부([`AccessControlAspect`](../../src/main/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspect.java)), 챌린저 기록 생성/코드 사용([`ChallengerRecordCommandService`](../../src/main/java/com/umc/product/challenger/application/service/ChallengerRecordCommandService.java)), 커뮤니티 신고([`ReportCommandService`](../../src/main/java/com/umc/product/community/application/service/command/ReportCommandService.java))다.

## ID와 당시 snapshot 원칙

대상이 있는 이벤트는 `targetId`를 필터링과 상관관계를 위한 안정적인 참조로 유지한다. 대상 자체가 없는 이벤트에서는 구현 계약상 `targetId`가 `null`일 수 있으므로 임의의 ID를 만들지 않는다. ID만으로는 대상이 삭제되거나 이름·소속·상태가 바뀐 뒤 설명력이 사라지므로, 감사 판단에 필요한 당시 값을 `details.target`, `details.actor`, `before`, `after`에 함께 저장한다.

```json
{
  "schemaVersion": 1,
  "actor": { "type": "USER", "memberId": 73, "name": "운영자", "schoolName": "한국대학교" },
  "target": { "type": "Schedule", "id": "10", "name": "정기 세션", "status": "OPEN" },
  "context": { "requestId": "request-123" },
  "before": { "status": "DRAFT" },
  "after": { "status": "OPEN" }
}
```

삭제된 참조에 대해서는 **저장된 snapshot을 당시 표시의 신뢰 가능한 근거로 사용한다.** FE나 운영 도구가 현재 도메인을 재조회해 과거 이름·소속·상태를 다시 만들면 안 된다. snapshot이 없거나 legacy `null`이면 `과거 snapshot 없음`으로 표시하고 현재 값으로 채우지 않는다.

도메인 간 데이터가 필요하면 발생 도메인의 application service가 공개 Query UseCase로 필요한 최소 정보를 읽어 snapshot을 조립한다. audit 도메인이 member/schedule repository를 직접 조회하지 않는다.

## `details` 저장 계약과 민감정보 정책

### 저장·호환 계약

- DB 컬럼은 기존 `jsonb`를 유지한다. Java domain event의 `details`는 Map이고, API 응답 [`AuditLogInfo.details`](../../src/main/java/com/umc/product/audit/application/port/in/query/dto/AuditLogInfo.java)는 `String`이다.
- 표준 JSON root는 `schemaVersion`, `actor`, `target`, `context`, `before`, `after`이며 현재 `schemaVersion`은 `1`이다([`AuditDetails`](../../src/main/java/com/umc/product/audit/domain/AuditDetails.java)).
- 저장 JSON은 API에서 JSON object가 아니라 **JSON 문자열**로 전달될 수 있다. 기존/빈 details는 `null`이며, Jackson 설정에 따라 응답 필드가 생략될 수 있으므로 FE는 `null`과 absent를 동일하게 처리한다.
- [`AuditDetailsPolicy`](../../src/main/java/com/umc/product/audit/domain/AuditDetailsPolicy.java)는 legacy Map도 표준 version 1 shape으로 정규화한다. 직렬화 실패는 details를 `null`로 저장하고 warning을 남기며 원 요청을 실패시키지 않는다.
- 알 수 없는 `schemaVersion`은 현재 파서로 억지 변환하지 말고 원문을 보존하거나 `지원하지 않는 schemaVersion`으로 표시한다.

### 허용 allowlist

| section | 허용 key |
| --- | --- |
| `actor` | `type`, `memberId`, `name`, `nickname`, `schoolName` |
| `target` | `type`, `id`, `memberId`, `name`, `nickname`, `schoolName`, `status`, `title`, `roleName`, `period`, `term`, `round`, `recordType` |
| `context` | `requestId`, `traceId`, `outcome`, `source`, `reason`, `resourceType`, `resourceId`, `permission` |
| `before`/`after` | `target`와 같은 상태·표시 key |

허용 key라도 값은 감사 판단에 필요한 scalar만 넣는다. `null`·blank는 버리고 문자열은 최대 255자로 제한한다. name/nickname/school snapshot은 이 구조화된 allowlist 안에서만 보존한다. 값에 `token`/`password`/`body`/`authorization`/`bearer`/`secret` marker 또는 CR/LF·로그 위조용 개행이 있으면 원문 대신 고정 `[REDACTED]`로 대체하고, 일반 표시값은 그대로 보존한다. 별도 [`AuditDescriptionPolicy`](../../src/main/java/com/umc/product/audit/domain/AuditDescriptionPolicy.java)는 description을 NFKC/공백 정규화하고 500자로 제한하며, 줄바꿈이나 name/nickname/school/password/token/authorization/body/content marker가 있으면 안전한 기본 문장으로 대체한다. 호출부도 정적 description만 사용한다.

### 금지 denylist

`email`, `password`, `token`, `accessToken`, `refreshToken`, `idToken`, `authorization`, `authorizationHeader`, `providerId`, `oauthSubject`, `code`, `verificationCode`, `authCode`, `oneTimeCode`, `otpCode`, `rawBody`, `content`, `body`는 어떤 section에도 저장하지 않는다. 표기 변형(`provider_id`, `Pass-Word`, `raw_body` 등)도 정규화 후 차단된다. 자유 텍스트 안의 JSON처럼 보이는 문자열은 구조화된 key로 승격하지 않는다.

## 운영·장애 대응

### Outbox 저장과 재처리

`OutboxDomainEventPublisher`는 항상 활성화되며 성공 감사 event를 `event_outbox`에 저장한다. local publisher로 우회하는 비활성화 설정은 없다.

- `app.event-outbox.relay-enabled=${EVENT_OUTBOX_RELAY_ENABLED:true}`가 기본값이다. `true`이면 poller가 기본 1초 간격, batch 100으로 relay한다.
- `false`이면 outbox write는 유지하고 poller만 중지한다. 이때 성공 감사 event는 `PENDING`으로 적체되므로 점검 후 relay를 재개해야 한다.
- 상태는 `PENDING`/`PROCESSING`/`PUBLISHED`/`FAILED`이며 최대 5회 재시도, 5분 lease, 5초부터 최대 5분 backoff가 현재 코드 계약이다. 명시 `FAILURE`의 direct 저장은 outbox를 거치지 않는다.
- 감사 listener 저장이 실패하면 예외를 relay에 전달해 outbox를 재시도한다. 저장 성공 전에 `PUBLISHED`로 완료하지 않는다.
- relay가 최대 시도 후 `FAILED`가 되면 현재 자동 복구 API나 audit 전용 replay 도구는 없다. `event_outbox`의 상태·`last_error`, application log, audit metric을 확인하고 승인된 운영 절차로 재처리 여부를 결정해야 한다. outbox backlog/relay 자체의 별도 metric·alert는 후속 과제다.

### 저장 실패 metric과 alert

[`AuditLogRecordingMonitor`](../../src/main/java/com/umc/product/audit/application/service/command/AuditLogRecordingMonitor.java)는 저장 성공을 `operational.audit.log.total{domain,action,outcome,source}`에, 실패를 `operational.audit.log.failure.total{domain,action,reason}`에 기록한다. 처리 reason은 `details_serialization`, `spel_evaluation`, `event_publish`, `context_extraction` 고정값을 사용한다. `targetId`, `requestId`, `traceId`는 metric tag로 사용하지 않는다.

Prometheus의 [`AuditLogSaveFailures`](../../infra/monitoring/grafana/config/prometheus/rules/default-alerts.yml) rule은 최근 5분의 failure counter 증가를 즉시 warning으로 알린다. 알림 발생 시 application log의 `감사 로그 저장 실패`, DB 연결/constraint, `audit_log` 최근 row, outbox를 차례로 확인한다. 이 alert가 감사 이벤트 유실이 없음을 보장하는 것은 아니다.

### 현재 범위와 후속 과제

| 주제 | 현재 코드에서 확인되는 범위 | 후속 과제 |
| --- | --- | --- |
| retention | `audit_log` 정리/보관 scheduler와 기간 정책 없음 | 법적 근거·업무 요구를 정하고 archive, 삭제, 파티셔닝 절차를 설계 |
| 권한 | [`AuditLogPermissionEvaluator`](../../src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java)가 `AUDIT/READ`를 중앙운영사무국 국원 여부로 평가. 조회 controller만 공개 | DB role로 UPDATE/DELETE를 제한하고 audit 조회 자체를 기록할지 결정 |
| 무결성 | entity에 공개 setter·쓰기 controller는 없지만 DB-level INSERT-only, hash chain, WORM, SIEM은 없음 | application DB role 분리, append-only 권한, 암호학적 무결성/외부 보관 필요성 검토 |

위 후속 기능은 현재 구현으로 서술하거나 FE가 의존해서는 안 된다.

## 신규 audit checklist

- [ ] 이 행위가 일반 log/metric이 아니라 사후 책임 추적이 필요한가?
- [ ] 단순 request/response 메타데이터면 `@Audited`를 사용했는가?
- [ ] rich/delete/failure면 삭제·롤백 전에 snapshot을 확보하고 `RecordAuditLogUseCase`를 선택했는가?
- [ ] `targetId`와 당시 표시 snapshot을 모두 저장했는가?
- [ ] allowlist 밖 key와 denylist 값을 제거했는가?
- [ ] FE가 현재 도메인 재조회 없이 legacy `null`, plain/legacy String, 숫자 및 알 수 없는 `schemaVersion`을 fallback 처리하고 지원 버전 `1`만 구조화하는가?
- [ ] 저장 실패가 요청 결과를 깨뜨리지 않으며 metric/log/alert로 관측되는가?
