# API 감사 로깅 전면 적용 계획서

> 작성일: 2026-05-07
> 대상 도메인: `audit` + 모든 state-changing API 보유 도메인
> 핵심 코드:
> - [AuditLog.java](src/main/java/com/umc/product/audit/domain/AuditLog.java)
> - [AuditAction.java](src/main/java/com/umc/product/audit/domain/AuditAction.java)
> - [@Audited](src/main/java/com/umc/product/audit/application/port/in/annotation/Audited.java)
> - [AuditAspect.java](src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java)
> - [AuditLogEventListener.java](src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java)
> - [AuditLogController.java](src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java)
> - [V2026.03.12.01.00__create_audit_log.sql](src/main/resources/db/migration/V2026.03.12.01.00__create_audit_log.sql)

---

## 1. 배경 및 문제 정의

### 1.1 현재 상태

audit 도메인은 **인프라 완성도 100%, 실 적용 커버리지 ~5%** 상태다.

- **인프라**: `audit_log` 테이블, `AuditLog` 엔티티(immutable 지향), `AuditAction`/`Domain` enum, `@Audited` annotation + AOP(`AuditAspect`), `@TransactionalEventListener(AFTER_COMMIT) + @Async("auditTaskExecutor")` 비동기 저장기, `/api/v1/admin/audit-logs` 검색 API + 권한 가드(`@CheckAccess(AUDIT, READ)`, 중앙운영사무국 국원 이상)까지 모두 구현되어 있다.
- **실 적용**: `@Audited`가 붙은 메서드는 `ScheduleCommandService` 2건 + `TestController` 1건. 운영 코드에서 의미 있는 적용은 `ScheduleCommandService.create`/`update` 단 2개 메서드뿐이다(`TestController`는 테스트용).

  ```bash
  $ grep -rn "@Audited" src/main/java | grep -v "^.*Audited.java:"
  src/main/java/com/umc/product/schedule/application/service/command/ScheduleCommandService.java:62:    @Audited(...)   # 일정 생성
  src/main/java/com/umc/product/schedule/application/service/command/ScheduleCommandService.java:127:   @Audited(...)   # 일정 수정
  src/main/java/com/umc/product/test/controller/TestController.java:92:                @Audited(...)   # 테스트용
  ```

- 즉 회원 가입/탈퇴, 로그인/로그아웃, 권한 변경, 공지 발송, 챌린저 등록/제명, 프로젝트 매칭, 결제·환불 성격의 admin 작업 등 **보안·운영 감사가 필요한 거의 모든 상태 변경 API에 audit 흔적이 남지 않는다.**

### 1.2 문제

1. **보안 사고 추적 불가** — 누가 언제 어떤 회원을 탈퇴시켰는지, 누가 권한을 부여했는지, 로그인 실패가 몇 번 있었는지 알 수 없다.
2. **운영 사고 분석 비용** — "왜 이 일정이 사라졌는가?" 같은 질문에 답하려면 git log + grep + 운영자 인터뷰에 의존하게 된다.
3. **컴플라이언스/감사 대응 어려움** — 외부 감사(개인정보보호법, 위탁계약 등) 시 "관리자 행위 로그 보존" 증빙이 부재.
4. **적용 컨벤션 부재** — 두 사례만으로는 "어디에 어떻게 붙이는지"가 팀에 전파되지 않는다. 신규 기능을 만드는 사람이 적용을 잊는다.

### 1.3 목표

1. **모든 state-changing API**(POST/PUT/PATCH/DELETE 또는 그에 준하는 mutation)에 audit 로그를 일관되게 남긴다.
2. **로그인/로그아웃/권한 변경/탈퇴 등 보안 핵심 액션은 누락 없이** 기록한다.
3. **`@Audited` 적용 컨벤션을 한 줄로 답할 수 있게** 정리한다(어느 layer, 어느 메서드, SpEL 어떻게 쓰나).
4. **실패한 요청도 보안 감사 대상으로 흔적**을 남긴다(현재는 성공만 기록).
5. **운영 정책**(보관 기간, 무결성, PII 마스킹, 모니터링)을 함께 결정한다.

---

## 2. 현황 인벤토리

### 2.1 audit 도메인 인프라 (현재)

| 영역          | 구현물                                                                              | 비고                                                                                  |
|-------------|----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| 도메인         | `AuditLog`, `AuditAction`, `AuditLogEvent`                                       | 이벤트 record 기반, 엔티티는 immutable 지향(BaseEntity 미상속)                                    |
| 어노테이션       | `@Audited(domain, action, targetType, targetId, description)`                    | `targetId`/`description`은 SpEL(`#result`, `#command.x()`)                           |
| AOP         | `AuditAspect.publishAuditEvent`                                                  | `@AfterReturning` 만 처리. 예외 발생 메서드는 무시                                               |
| 이벤트 리스너     | `AuditLogEventListener.handle`                                                   | `@Async("auditTaskExecutor") + @TransactionalEventListener(AFTER_COMMIT)`           |
| Persistence | `AuditLogPersistenceAdapter`, `AuditLogJpaRepository`, `AuditLogQueryRepository` | jsonb `details` 컬럼, `@JdbcTypeCode(SqlTypes.JSON)`                                  |
| 조회 API      | `GET /api/v1/admin/audit-logs`                                                   | domain/action/actorMemberId/from/to + Pageable                                      |
| 권한          | `@CheckAccess(AUDIT, READ)`                                                      | "중앙운영사무국 국원 이상"만 조회 가능                                                              |
| 마이그레이션      | `V2026.03.12.01.00__create_audit_log.sql`                                        | 인덱스 4개: (domain, action), (target_type, target_id), (actor_member_id), (created_at) |

### 2.2 `AuditAction` enum (현재)

```java
CREATE, UPDATE, DELETE, APPROVE, REJECT, CHECK, SUBMIT, REGISTER, WITHDRAW
```

→ 인증/권한 부여/조회 액션이 **부재**. 후속 §6에서 확장안을 제시한다.

### 2.3 `Domain` enum (현재, audit 분류 키로 재사용 중)

`COMMON, AUTHENTICATION, AUTHORIZATION, MEMBER, CHALLENGER, ORGANIZATION, CURRICULUM, SCHEDULE, COMMUNITY, NOTICE, FCM, SURVEY, RECRUITMENT, TERMS, EMAIL, STORAGE, WEBHOOK, AUDIT_LOG, PROJECT, FIGMA, LLM`

원래 예외 분류용이지만 `AuditLog.domain`이 이 enum을 그대로 재활용한다. 새 도메인이 생길 때 두 곳을 동시에 갱신해야 한다(이미 동일 enum이라 갱신 비용 한 번).

### 2.4 컨트롤러 인벤토리

도메인별 controller를 모두 열거하고, 각각이 갖는 mutation 성격의 endpoint 유무를 정리한다. (자세한 endpoint 매핑은 적용 단계에서 도메인별 PR로 산출.)

| 도메인            | Controller                                                                                                                                                                                                    | mutation 비중                                                                      | 보안 우선순위           |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|-------------------|
| audit          | `AuditLogController`                                                                                                                                                                                          | — (조회만)                                                                          | —                 |
| authentication | `AuthenticationController`, `CredentialAuthenticationController`, `EmailAuthenticationController`, `MemberOAuthController`, `TokenAuthenticationController`                                                   | 로그인/로그아웃/토큰 갱신/이메일 인증/OAuth 연동·해제                                                | **P0** (보안 핵심)    |
| authorization  | `ChallengerRoleController`, `ResourcePermissionController`                                                                                                                                                    | 권한 부여·회수                                                                         | **P0**            |
| member         | `MemberCommandController` (변경/탈퇴 mutation), `MemberQueryController` (PII 일괄 조회는 `READ_SENSITIVE`)                                                                                                             | 회원 정보 변경/탈퇴/admin PII 조회                                                         | **P0**            |
| challenger     | `ChallengerCommandController`, `ChallengerPointCommandController`, `ChallengerRecordController`, `ChallengerSearchController`, `ChallengerQueryController`, `ChallengerRecordPermissionController`(파일 위치 이상함) | 챌린저 등록·제명·포인트·기록                                                                 | **P1**            |
| community      | `CommentController`, `PostController`, `ReportController` (Query 둘)                                                                                                                                        | 게시·신고                                                                               | **P1** (신고/제재 기록) |
| notice         | `NoticeCommandController`, `NoticeContentController`, `NoticeVoteResponseController`, `NoticeQueryController`                                                                                                 | 공지 작성·수정·삭제·투표 응답                                                                | **P1**            |
| organization   | `Chapter*`, `Gisu*`, `School*`, `StudyGroup*`, `StudyGroupSchedule*`                                                                                                                                          | 운영 조직 변경                                                                         | **P1**            |
| project        | `ProjectApplicationFormController`, `ProjectCommandController`, `ProjectMatchingRoundController`, `ProjectQueryController`                                                                                    | 프로젝트 매칭/지원서                                                                      | **P1**            |
| curriculum     | `ChallengerWorkbookCommand*`, `ChallengerWorkbookMissionCommand*`, `CurriculumCommand*`, `OriginalWorkbook*` 등 (v2)                                                                                           | 미션 제출/평가                                                                         | **P2**            |
| schedule       | `ScheduleCommandV2Controller`, `ScheduleQueryV2Controller`                                                                                                                                                    | 일정 — **부분 적용**(`ScheduleCommandService.create`/`update`만, delete·참석/출석 변경 등 미적용) | **P1 (잔여 적용 필요)** |
| notification   | `FcmController`                                                                                                                                                                                               | FCM 토큰 등록                                                                        | **P2**            |
| storage        | `StorageController`                                                                                                                                                                                           | presigned URL 발급/파일 메타                                                           | **P2**            |
| term           | `TermController`                                                                                                                                                                                              | 약관 동의                                                                            | **P1** (법적 증빙)    |
| figma          | `FigmaOAuthController`, `FigmaSyncController`, `FigmaWatchedFileController`, `FigmaRoutingDomainController`                                                                                                   | OAuth 연동·해제, 파일 등록                                                               | **P2**            |
| test           | `TestController`                                                                                                                                                                                              | 테스트용 — 실서비스 X                                                                    | —                 |
| global/error   | `CustomErrorController`                                                                                                                                                                                       | —                                                                                | —                 |

> 위 표는 controller 단위 우선순위다. mutation endpoint별 매핑은 적용 단계에서 도메인 PR로 작성한다. (참고: [ChallengerRecordPermissionController](src/main/java/com/umc/product/challenger/application/service/evaluator/ChallengerRecordPermissionController.java)는 파일 경로가 evaluator 패키지에 있어 컨벤션 어긋남 — 별도 정리 후보.)
>
> `Domain` enum에는 `SURVEY`, `RECRUITMENT`, `EMAIL`, `LLM`, `WEBHOOK`도 존재하지만 본 분석 시점 기준 외부 web API(`*Controller`)가 노출되어 있지 않다. 이들은 내부 호출 또는 외부 webhook 수신 채널이며, 내부 호출자가 audit이 필요한 액션을 수행한다면 그 호출자(다른 도메인의 service)에서 `@Audited`/수동 발행이 일어난다. 향후 controller가 추가되면 본 표를 갱신한다.

### 2.5 Audit 적용 사례 (현재 2건)

```java
// ScheduleCommandService.create
@Audited(
    domain = Domain.SCHEDULE,
    action = AuditAction.CREATE,
    targetType = "Schedule",
    targetId = "#result",
    description = "'일정이 생성되었습니다.'"
)
public Long create(CreateScheduleCommand command) { ... }

// ScheduleCommandService.update
@Audited(
    domain = Domain.SCHEDULE,
    action = AuditAction.UPDATE,
    targetType = "Schedule",
    targetId = "#command.scheduleId()",
    description = "'일정이 수정되었습니다.'"
)
public Long update(EditScheduleCommand command) { ... }
```

→ 적용 layer는 **application service의 메서드 단위**(트랜잭션 경계 안). 본 계획은 이 컨벤션을 표준으로 채택한다.

---

## 3. 갭 분석 (P1~P5)

### P1 (Critical — 보안 사고 직결)

- 로그인 성공/실패, 로그아웃, 토큰 재발급, OAuth 연동/해제 — `authentication` 전체에 audit 흔적 없음
- 권한 부여/회수 — `authorization` 도메인에 흔적 없음
- 회원 탈퇴, admin의 강제 탈퇴/정보 변경 — 흔적 없음

### P2 (Significant)

- `@Audited` AOP가 **`@AfterReturning`만 처리** → 실패한 요청(권한 거부, 검증 실패 등)이 로그에 남지 않음. 실패는 보안 감사의 절반이다.
- `details` 페이로드가 **항상 `Map.of()` 빈 값**으로 발행됨([AuditAspect.java:59](src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java#L59)). annotation에 `details` SpEL이 없어 변경 전후 값을 담을 방법 자체가 없다.
- `AuditAction` enum이 9개로 한정 — `LOGIN`, `LOGOUT`, `TOKEN_REFRESH`, `GRANT`, `REVOKE`, `READ_SENSITIVE`, `EXPORT`, `ACCESS_DENIED` 등 누락.
- 시스템 액션(스케줄러, 이벤트 핸들러)에서는 `SecurityContextHolder`에 인증이 없어 `actorMemberId = null`로 저장됨. 시스템 actor를 식별할 별도 표기 필요(`actorMemberId = -1`이거나 별도 컬럼).
- audit_log 테이블이 **변경 가능**하다. UPDATE/DELETE를 막는 DB 제약이 없어 권한이 있는 사람은 흔적을 지울 수 있다. 보안 감사 표준상 append-only가 권장.

### P3 (Code Quality)

- `description` SpEL 표현식 오류 시 `AuditAspect`가 catch + `log.error`만 — 조용히 실패. 어노테이션 파라미터 컴파일 타임 검증이 없으니 운영 중 갱신 누락이 생긴다.
- 보관 정책(retention) 미정. 1년? 3년? 영구? `created_at` 인덱스는 있으나 정리 잡 없음.
- PII 마스킹 정책 부재 — `description`/`details`에 이메일/이름이 평문 들어갈 수 있음.
- audit 적용을 잊은 메서드를 자동 감지할 수단 없음(`@Audited`가 빠진 command 메서드를 lint로 잡을 수 있음).

### P4 (Alternative)

- 적용 layer를 **service**로 통일했지만, controller layer 또는 filter/interceptor에서 잡을 수도 있다. 컨벤션을 명문화해야 혼란이 없다.
- audit 검색 API에 `targetType`/`targetId` 필터, 본문 키워드 검색이 없음 — 운영 시 "회원 12345에 대한 모든 행위" 같은 질의가 잦으면 추가 필요.

### P5 (Minor)

- `details`를 `String`(JSON)으로 직접 저장 — 도메인 어디서든 `Map<String,Object>`를 넘기면 `AuditLogCommandService`가 직렬화하지만, 잘못된 타입이 들어오면 런타임에 발견된다.
- AuditLog가 `BaseEntity`를 상속하지 않고 `@CreatedDate` 직접 — immutable 의도와 일치. 의도된 결정.
- `AuditAction` enum에 displayName/한국어 라벨 없음 — admin UI 표시 시 코드와 라벨 매핑 필요해질 수 있음.

---

## 4. 결정 사항

### 4.1 적용 layer — Application Service 메서드 단위

`@Audited`는 **application service의 command 메서드**에 붙인다. 컨트롤러나 도메인 객체에 붙이지 않는다.

이유:

- 트랜잭션 경계 안에서 발행되어야 `@TransactionalEventListener(AFTER_COMMIT)`이 의도대로 동작한다.
- 컨트롤러 메서드는 `@Valid` 등 입력 검증이 분리된 단계라 audit 의미와 결합도가 낮다.
- service 메서드는 비즈니스 의미가 일관되며, AOP가 SpEL로 commands/results를 표현하기 적합하다.

**예외 — 수동 발행**: 인증 도메인의 일부 흐름(예: 로그인 실패, 자격증명 거부)은 `@AfterReturning` 기반 annotation으로는 잡기 어렵다. 메서드가 정상 종료되지 않는 경로가 audit 대상이거나(LOGIN_FAILURE), 성공/실패 분기마다 다른 액션을 기록해야 하기 때문이다. 이 경우 service 메서드의 try/catch 분기에서 직접 `eventPublisher.publishEvent(AuditLogEvent)`를 호출한다(annotation 의존 X). 본 계획서에서는 이 패턴을 "수동 발행"으로 명명한다. 수동 발행도 동일한 `AuditLogEventListener`로 들어가 비동기 저장·격리 동작은 동일하다.

### 4.2 audit 도메인 보강

#### 4.2.1 `AuditAction` enum 확장

```java
public enum AuditAction {
    // 기존
    CREATE, UPDATE, DELETE,
    APPROVE, REJECT,
    CHECK, SUBMIT,
    REGISTER, WITHDRAW,

    // 추가
    LOGIN_SUCCESS,
    LOGIN_FAILURE,           // 보안 모니터링 핵심
    LOGOUT,
    TOKEN_REFRESH,
    OAUTH_LINK,
    OAUTH_UNLINK,

    GRANT,                   // 권한 부여
    REVOKE,                  // 권한 회수

    READ_SENSITIVE,          // 민감 정보 조회 (회원 PII, audit log 자체 등)
    EXPORT,                  // 대량 다운로드/CSV export

    ACCESS_DENIED            // 권한 검증에서 거부된 시도
}
```

확장 enum은 마이그레이션 불필요(EnumType.STRING으로 저장).

#### 4.2.2 `@Audited` 어노테이션 확장

`details`를 SpEL로 받을 수 있게 한다.

```java
public @interface Audited {
    Domain domain();
    AuditAction action();
    String targetType();
    String targetId() default "";
    String description() default "";
    String details() default "";   // SpEL, evaluate 결과는 Map<String,Object> 또는 직렬화 가능 객체
}
```

`AuditAspect`는 `details` 표현식을 평가해 `Map`이면 그대로, 그 외 객체이면 `objectMapper.convertValue(value, Map.class)`로 변환해서 `AuditLogEvent.details`에 채운다.

#### 4.2.3 실패 케이스 로깅

세 가지 경로로 실패도 흔적을 남긴다.

1. **`@Audited(logFailure = true)`** — annotation에 `logFailure` 옵션을 추가하고 `AuditAspect`에 `@AfterThrowing` 분기를 더한다. 메서드가 예외로 끝나면 동일 `action` + `outcome=FAILURE`로 발행한다. 별도 `failureAction` enum 항목(예: `WITHDRAW_FAILED`)을 두는 안도 검토했으나, action enum 폭증을 피하기 위해 **action은 그대로 두고 audit_log에 `outcome` 컬럼(JPA `EnumType.STRING`, DB `VARCHAR(16)`, 값 `SUCCESS`/`FAILURE`)을 추가**하는 안을 채택한다.
2. **`@CheckAccess`(authorization aspect) 가드 거부**는 `ACCESS_DENIED` 액션으로 별도 기록. authorization aspect에 hook을 추가해 거부 시점에 직접 발행한다.
3. **로그인 실패**(자격증명 오류)는 service 메서드의 catch 블록에서 직접 `LOGIN_FAILURE` 이벤트 수동 발행.

### 4.3 audit_log 무결성

- `audit_log`는 INSERT-only로 운영한다. PostgreSQL에서 `REVOKE UPDATE, DELETE ON audit_log FROM <app_role>`을 적용해 어플리케이션 DB role이 수정·삭제하지 못하도록 강제한다(별도 admin role만 가능).
- 테이블에 `outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS'` 컬럼 추가 마이그레이션이 필요하다(§6.1).
- `created_at`은 이미 `@CreatedDate + nullable=false, updatable=false`. JPA 레벨에서도 변경 차단.

### 4.4 시스템 actor 표기

`AuditAspect.extractMemberId`가 `null`을 반환하는 케이스(스케줄러, 이벤트 핸들러, 시스템 작업)는 본 계획 단계에서 `actorMemberId IS NULL`을 그대로 "시스템 액션"의 표지로 사용한다. `actor_type ENUM('USER','SYSTEM','ADMIN_BACKDOOR')` 같은 별도 컬럼 도입은 운영 데이터가 쌓이고 분류 필요성이 명확해진 뒤 §7-1에서 재검토한다.

### 4.5 PII / 민감정보 정책

- `description`/`details`에 **이메일, 비밀번호, 토큰, 주민등록번호, 휴대폰, 카드번호**를 절대 포함하지 않는다.
- 회원 식별이 필요하면 `actorMemberId`/`targetId(=memberId)`만 사용한다.
- annotation 작성 PR 리뷰 시 위 항목에 대한 체크리스트를 PR 템플릿에 추가한다.

### 4.6 보관 정책

- 기본 보관: **3년** (개인정보보호법 위탁 관련 표준에 맞춤).
- 일별 정리 잡(`@Scheduled cron = "0 0 4 * * *"`)으로 3년 초과 row는 cold storage로 archive 후 hard delete. 본 계획은 jab 도입까지 포함하되, 실제 archive 대상 storage(S3 cold archive 등)는 후속 결정.

### 4.7 적용 우선순위

1. **P0 (즉시 적용)**: authentication, authorization, member (탈퇴 등 회원 라이프사이클)
2. **P1**: notice (운영 공지), term (약관 동의), challenger 등록·제명, organization 변경, project 매칭, community 신고/제재
3. **P2**: curriculum 미션 제출/평가, schedule(이미 일부 적용), notification, storage, figma
4. **외**: read 전용 controller는 audit 미적용. 단, **민감 조회**(audit 자체, 회원 PII 일괄 조회, export)는 `READ_SENSITIVE`/`EXPORT`로 적용.

---

## 5. `@Audited` 적용 표준 컨벤션

### 5.1 어디에 붙이나

application service의 **command 메서드**.

### 5.2 무엇을 채우나

| 필드            | 규칙                                                    |
|---------------|-------------------------------------------------------|
| `domain`      | 비즈니스 도메인. `Domain` enum 그대로                           |
| `action`      | `AuditAction` enum. 가장 정확한 액션 선택                      |
| `targetType`  | 변경 대상 엔티티 타입명(예: `"Member"`, `"Challenger"`). 문자열 그대로 |
| `targetId`    | 우선순위: `#result`(생성 시) > `#command.xxxId()`(수정/삭제 시)   |
| `description` | 한국어 한 문장. 상수 또는 `'…' + #command.xxx()` 형태. PII 금지     |
| `details`     | 변경된 필드 → 값 Map. 변경 전후가 의미 있으면 `before`/`after` 키 사용   |

### 5.3 예시 패턴

```java
// CREATE
@Audited(
    domain = Domain.NOTICE,
    action = AuditAction.CREATE,
    targetType = "Notice",
    targetId = "#result",
    description = "'공지가 생성되었습니다.'",
    details = "{ 'title': #command.title(), 'classification': #command.classification().name() }"
)
public Long create(CreateNoticeCommand command) { ... }

// DELETE — 결과가 없으니 command에서 추출
@Audited(
    domain = Domain.MEMBER,
    action = AuditAction.WITHDRAW,
    targetType = "Member",
    targetId = "#memberId.toString()",
    description = "'회원 탈퇴 처리가 완료되었습니다.'"
)
public void withdraw(Long memberId) { ... }

// 인증 — 트랜잭션 없음 → 수동 발행
public TokenPair login(LoginCommand command) {
    try {
        TokenPair pair = authenticate(command);
        eventPublisher.publishEvent(AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN_SUCCESS)
            .targetType("Member")
            .targetId(pair.memberId().toString())
            .actorMemberId(pair.memberId())
            .description("로그인 성공")
            .details(Map.of("provider", command.provider().name()))
            .ipAddress(...)
            .build());
        return pair;
    } catch (AuthenticationDomainException e) {
        eventPublisher.publishEvent(AuditLogEvent.builder()
            .domain(Domain.AUTHENTICATION)
            .action(AuditAction.LOGIN_FAILURE)
            .targetType("Member")
            .targetId(null)
            .actorMemberId(null)
            .description("로그인 실패: " + e.getErrorCode().name())
            .details(Map.of("provider", command.provider().name()))
            .build());
        throw e;
    }
}
```

### 5.4 SpEL 안전성

- 표현식은 한 줄에 한 connection만(중첩 ternary 금지).
- `#command`/`#result`만 참조하고, 정적 메서드/시스템 호출은 금지(`T(System).currentTimeMillis()` 같은 표현식 사용 금지).
- annotation에 SpEL 표기 오류가 발견되는 즉시 빌드 게이트에서 막을 수 있도록, 단위 테스트로 각 audit annotation을 검증하는 헬퍼(`AuditedAnnotationTest`)를 추가한다.

---

## 6. 단계별 적용 계획 (Commit 단위)

각 커밋은 독립적으로 빌드/테스트 통과해야 하며, Conventional Commits(`<type>: <subject>`)를 따른다. PR은 도메인 묶음 단위로 분리한다.

### Phase 0 — audit 도메인 보강

1. `feat: AuditAction enum에 인증·권한·민감 조회·실패 액션 추가`
    - 위 §4.2.1 11개 신규 값 추가.
    - 단위 테스트로 enum 갯수 변화 명시.

2. `feat: audit_log 테이블에 outcome 컬럼 추가`
    - Flyway: `V2026.MM.DD__alter_audit_log_add_outcome.sql`
        - `ALTER TABLE audit_log ADD COLUMN outcome VARCHAR(16) NOT NULL DEFAULT 'SUCCESS'`
        - 인덱스: `(domain, action, outcome)` 추가(실패 행 빠른 조회 용도).
    - `AuditLog.outcome` 필드(`@Enumerated(EnumType.STRING)`), `AuditLogEvent.outcome`, `AuditLog.from(...)` 시그니처 확장.
    - DB 레벨 INSERT-only 강제(`REVOKE UPDATE, DELETE ON audit_log FROM <app_role>`)는 환경별(local/dev/prod) 어플리케이션 DB role 이름 합의가 선행되어야 하므로 본 commit에 포함하지 않고 §7-7로 분리한다.

3. `feat: @Audited annotation에 details SpEL과 logFailure 옵션 추가`
    - `details() default ""` — SpEL로 `Map<String,Object>` 또는 직렬화 가능 객체를 평가. `Map`이면 그대로, 그 외 객체는 `objectMapper.convertValue(value, Map.class)`로 변환해 jsonb에 저장.
    - `logFailure() default false` — true면 `AuditAspect`의 `@AfterThrowing` 분기에서 같은 `action` + `outcome=FAILURE`로 발행한다(성공 시는 기존 `@AfterReturning` 경로로 `outcome=SUCCESS`).
    - 단위 테스트: SpEL 평가(빈 문자열/Map/객체), `logFailure=true` 시 예외 경로 발행, `logFailure=false`(기본) 시 예외 경로 미발행.

4. `feat: ACCESS_DENIED 자동 기록을 위한 authorization aspect hook`
    - `@CheckAccess` aspect의 거부 분기에서 `AuditLogEvent(action=ACCESS_DENIED, outcome=FAILURE)` 직접 발행.
    - resourceType과 permission을 `details`에 담는다.

5. `chore: AuditLogQueryRepository에 outcome 필터 추가`
    - `/api/v1/admin/audit-logs?outcome=FAILURE` 등 필터 파라미터.
    - 컨트롤러·DTO 동기화.

### Phase 1 — P0 도메인 적용 (보안 핵심)

6. `feat: authentication 도메인 audit 적용`
    - `CredentialAuthenticationService`, `OAuthAuthenticationService`, `EmailAuthenticationService`, `TokenAuthenticationService` 각각의 success/failure 경로에 수동 발행.
    - 액션 매핑: `LOGIN_SUCCESS`/`LOGIN_FAILURE`/`LOGOUT`/`TOKEN_REFRESH`/`OAUTH_LINK`/`OAUTH_UNLINK`.
    - 단위 테스트: 각 경로에서 이벤트 발행 검증(`ApplicationEvents` 사용).

7. `feat: authorization 도메인 audit 적용`
    - `ChallengerRoleService`(가칭), `ResourcePermissionService`의 부여/회수 메서드에 `@Audited(action=GRANT|REVOKE)`.
    - `details`에 `roleType`/`resource`/`permission` 포함.

8. `feat: member 도메인 audit 적용`
    - `MemberCommandService`의 회원 정보 변경, 탈퇴 메서드에 `@Audited`.
    - admin이 다른 회원을 변경한 경우 `actorMemberId` ≠ `targetId`로 자연 분리됨.

### Phase 2 — P1 도메인 적용

9. `feat: notice 도메인 audit 적용`
10. `feat: term 동의/철회 audit 적용`
11. `feat: challenger 등록·제명·포인트 audit 적용`
12. `feat: organization (Chapter/Gisu/School/StudyGroup) 변경 audit 적용`
13. `feat: project 매칭/지원서 audit 적용`
14. `feat: community 신고/제재 audit 적용`

각 커밋은 동일 패턴: 해당 command service에 `@Audited` 부착 + service 단위 테스트로 발행 검증.

### Phase 3 — P2 도메인 적용

15. `feat: curriculum 미션 제출/평가 audit 적용`
16. `feat: schedule 도메인 audit 잔여 메서드 적용`(현재 create/update만 적용. delete, 참석/출석 변경 등 잔여 분 적용)
17. `feat: notification(FCM 토큰), storage(presigned URL 발급), figma audit 적용`

### Phase 4 — 운영·정책

18. `feat: AuditLog 보관 정책 정리 스케줄러 추가`
    - `@Scheduled(cron = "0 0 4 * * *")`로 3년 초과 row archive 후 hard delete.
    - 멀티 인스턴스에서 한 번만 실행되도록 ShedLock 또는 advisory lock 적용.

19. `feat: AuditLog 자체 조회를 READ_SENSITIVE로 기록`
    - `AuditLogController.search`에 `READ_SENSITIVE` 액션 발행. 누가 audit log를 조회했는지도 기록.

20. `chore: PR 템플릿에 audit 적용 체크리스트 추가`
    - `.github/pull_request_template.md`에 "command service 신규/변경 시 `@Audited` 적용했는가?", "PII가 description/details에 들어가지 않는가?" 항목.

21. `test: 커스텀 테스트로 @Audited 누락 감지`
    - 대상: `*application.service.command` 패키지의 public 메서드.
    - 방식: ArchUnit 또는 reflection 기반 단위 테스트가 모든 mutation 메서드를 스캔해 `@Audited` annotation 또는 명시 화이트리스트(authentication 도메인의 수동 발행 메서드 등)에 등재되지 않은 항목을 실패시킨다.
    - 화이트리스트는 `audit-whitelist.yaml`(또는 동등 형식)로 외부화해 점진적 도입을 허용한다.
    - 수동 발행은 컴파일 타임에 검증할 수 없으므로, 인증 도메인의 수동 발행 메서드는 별도 테스트(`AuthenticationAuditEmissionTest`)에서 `ApplicationEvents`로 발행 여부를 직접 검증한다.

22. `docs: audit 운영 가이드 작성`
    - `docs/guides/audit-logging.md`: `@Audited` 사용법, SpEL 안전성, PII 금지 항목, 신규 액션 추가 절차, 보관·archive 절차.

---

## 7. 미해결 / 후속 검토 항목

본 계획은 결정 단계에서 다음 항목을 의도적으로 deferred로 둔다.

1. **시스템 actor를 표기할 별도 컬럼 도입 여부** — `actorMemberId IS NULL`을 시스템으로 간주하는 현재 안 vs `actor_type ENUM` 컬럼 추가. 운영 데이터가 쌓인 뒤 결정.
2. **audit_log archive 저장소** — S3 Glacier? 별도 RDS 인스턴스? 운영팀 합의 필요.
3. **audit 검색 API의 본문 키워드 검색** — `description ILIKE '%탈퇴%'` 같은 질의가 잦으면 GIN 인덱스 도입 검토.
4. **FAILURE outcome의 폭증 모니터링** — `LOGIN_FAILURE`가 분당 임계치 초과 시 Discord 알림 등.
5. **`@Audited`가 잘못 평가될 때 fail-closed vs fail-open** — 현재는 `AuditAspect`가 try/catch + log.error로 fail-open. 보안 액션(LOGIN/LOGOUT/GRANT 등)에서는 fail-closed로 트랜잭션 자체를 실패시켜야 할 수도 있다. 위험과 가용성 trade-off 결정.
6. **audit_log 파티셔닝** — 3년치 누적이면 row 수가 수천만대 가능. PostgreSQL 파티셔닝(`PARTITION BY RANGE (created_at)`) 도입 시점 결정.
7. **audit_log INSERT-only 강제용 DB 권한 마이그레이션** — Phase 0 commit 2에서 `outcome` 컬럼 추가는 즉시 적용하지만, `REVOKE UPDATE, DELETE ON audit_log FROM <app_role>` 마이그레이션은 환경별(local/dev/prod) 어플리케이션 DB role 이름이 운영팀과 합의된 뒤 별도 적용한다. 우선 후보: 어플리케이션은 `app_writer` 같은 role로 운영하고, audit 정정·archive 잡은 별도 admin role(`audit_admin` 등)로 분리.

---

## 8. 빠른 참조 — 핵심 코드 위치

| 책임                    | 파일                                                                                                                                     |
|-----------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Audit 도메인 엔티티         | [AuditLog](src/main/java/com/umc/product/audit/domain/AuditLog.java)                                                                   |
| Audit 액션 enum         | [AuditAction](src/main/java/com/umc/product/audit/domain/AuditAction.java)                                                             |
| Audit 이벤트 record      | [AuditLogEvent](src/main/java/com/umc/product/audit/domain/AuditLogEvent.java)                                                         |
| `@Audited` 어노테이션      | [Audited](src/main/java/com/umc/product/audit/application/port/in/annotation/Audited.java)                                             |
| AOP                   | [AuditAspect](src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java)                                                     |
| 비동기 저장기               | [AuditLogEventListener](src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java)                               |
| 저장 서비스                | [AuditLogCommandService](src/main/java/com/umc/product/audit/application/service/command/AuditLogCommandService.java)                  |
| 검색 컨트롤러               | [AuditLogController](src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java)                                       |
| 권한 평가기                | [AuditLogPermissionEvaluator](src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java)                |
| 도메인 enum (audit 분류 키) | [Domain](src/main/java/com/umc/product/global/exception/constant/Domain.java)                                                          |
| 마이그레이션                | [V2026.03.12.01.00__create_audit_log.sql](src/main/resources/db/migration/V2026.03.12.01.00__create_audit_log.sql)                     |
| 적용 사례 (현재)            | [ScheduleCommandService.create/update](src/main/java/com/umc/product/schedule/application/service/command/ScheduleCommandService.java) |
