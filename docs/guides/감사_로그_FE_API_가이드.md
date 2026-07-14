# 감사 로그 FE/API 가이드

> 기준일: 2026-07-13
>
> 대상 독자: 중앙운영사무국 백오피스 FE 개발자
>
> 서버 기준: [`AuditLogController`](../../src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java), [`AuditLogInfo`](../../src/main/java/com/umc/product/audit/application/port/in/query/dto/AuditLogInfo.java), [`SearchAuditLogQuery`](../../src/main/java/com/umc/product/audit/application/port/in/query/dto/SearchAuditLogQuery.java)

## 1. 화면이 보여주는 것과 보여주지 않는 것

감사 로그 화면은 주요 상태 변경과 보안 실패를 **그 일이 발생했을 당시의 증거**로 보여준다. 감사 로그는 현재 회원·일정·게시글 상태의 source of truth가 아니며, FE가 과거 표시를 만들기 위해 현재 도메인 API를 다시 조회하면 안 된다.

- `targetId`는 필터·상관관계에 사용한다.
- 표시명·소속·상태·이전/이후 값은 응답 `details`의 snapshot을 우선한다.
- 대상이 삭제된 뒤에도 저장된 snapshot을 당시 표시의 신뢰 가능한 근거로 사용한다.
- snapshot이 없거나 legacy `details`가 `null`/생략이면 `과거 snapshot 없음`으로 표시한다. 현재 도메인의 최신 이름이나 상태로 채우지 않는다.
- 화면은 읽기 전용이다. 현재 서버에는 감사 로그 수정/삭제 endpoint가 없다.

감사 로그 조회 권한은 [`AuditLogPermissionEvaluator`](../../src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java)의 `AUDIT/READ` 평가를 따르며, 중앙운영사무국 국원만 허용된다. FE에서 권한을 추정하지 말고 401/403 공통 처리를 사용한다.

## 2. 데이터 적재 특성

트랜잭션 안에서 정상 반환한 `@Audited` event는 [`AuditAspect`](../../src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java)가 발행하고 비즈니스 변경과 함께 outbox에 저장한다. 커밋된 event만 relay되어 [`AuditLogEventListener`](../../src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java)가 감사 행을 저장한다. 따라서 비즈니스 요청 직후 조회하면 로그가 아직 보이지 않을 수 있고, 외부 transaction이 rollback되면 이 annotation 행은 저장되지 않는다. 액션 완료 화면에서 audit row가 즉시 존재한다고 가정하지 말고, 새로고침 또는 짧은 backoff polling을 사용한다.

명시 [`RecordAuditLogUseCase`](../../src/main/java/com/umc/product/audit/application/port/in/command/RecordAuditLogUseCase.java)의 rich `SUCCESS`도 같은 transaction-bound event 경로를 사용하므로 외부 business transaction rollback 뒤에는 남지 않는다. 로그인 실패·접근 거부 같은 `FAILURE`만 `REQUIRES_NEW`로 독립 보존한다. 어느 경로든 저장/발행 실패는 원 요청 결과와 격리되므로 FE는 row 존재를 비즈니스 성공의 동의어로 사용하지 않는다.

일부 호출은 `@Audited`와 명시 recorder를 함께 사용한다. 정상 커밋 뒤 두 성공 행이 relay를 통해 순차적으로 나타날 수 있으며 저장 순서는 계약하지 않는다. 대량 생성은 명시 행이 여러 개일 수 있다. 자동 중복 또는 일대일 행위로 매핑하지 말고 `source`, `details`, 대상·행위자·추적값·시각을 함께 사용한다.

성공 감사 event는 항상 outbox에 저장되고 기본 활성화된 relay가 비동기로 처리한다. FE는 outbox를 직접 호출하거나 상태를 조회하지 않으며, audit row가 비즈니스 응답과 동시에 존재한다고 추론하지 않는다.

`requestId`, `traceId`, `ipAddress`는 서버가 신뢰 경계 안에서 채운다. client `X-Request-Id`나 `X-Forwarded-For`를 보내 감사 검색값/IP로 강제할 수 없으며, FE는 서버가 응답·운영 로그로 제공한 식별자만 검색에 사용한다.

## 3. API

### 3.1 Endpoint

```http
GET /api/v1/audit/admin/audit-logs
Authorization: Bearer <ACCESS_TOKEN>
```

응답은 기존 `ApiResponse<Page<AuditLogInfo>>` 래퍼를 유지한다. `result`는 커스텀 page DTO가 아니라 Spring `Page` 형태다.

### 3.2 Query parameters

모든 필터는 optional이며, 입력된 조건은 AND로 결합된다. enum은 대문자 문자열을 사용한다.

| 파라미터 | 타입 | 동작 |
| --- | --- | --- |
| `domain` | `Domain` | 도메인 exact match |
| `action` | `AuditAction` | 액션 exact match |
| `actorMemberId` | `Long` | 행위자 ID exact match |
| `from` | `Instant` | `createdAt >= from`, inclusive |
| `to` | `Instant` | `createdAt <= to`, inclusive |
| `targetType` | `String` | 대상 타입 exact match |
| `targetId` | `String` | 대상 ID exact match. 숫자만 가능하다고 가정하지 않는다 |
| `outcome` | `AuditOutcome` | `SUCCESS` 또는 `FAILURE` exact match |
| `source` | `AuditSource` | 출처 enum exact match |
| `requestId` | `String` | exact match. LIKE wildcard가 아니다 |
| `traceId` | `String` | exact match. 부분 문자열 검색이 아니다 |
| `page` | `int` | 0-indexed page. Spring `Pageable` 기본값 사용 |
| `size` | `int` | 기본 20 |
| `sort` | `String` | query repository가 `createdAt DESC`로 고정하므로 사용자 정렬로 의존하지 않는다 |

`from`과 `to`는 offset이 있는 ISO-8601 문자열을 보낸다.

```text
2026-07-13T00:00:00Z
2026-07-13T09:00:00+09:00
```

`2026-07-13`처럼 offset이 없는 날짜만 보내면 `Instant` 변환이 실패해 400이 될 수 있다.

### 3.3 호출 예시

```bash
curl -G 'https://<HOST>/api/v1/audit/admin/audit-logs' \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  --data-urlencode 'targetType=Schedule' \
  --data-urlencode 'targetId=10' \
  --data-urlencode 'outcome=SUCCESS' \
  --data-urlencode 'source=EXPLICIT_RECORDER' \
  --data-urlencode 'page=0' \
  --data-urlencode 'size=20'
```

요청 추적값으로 찾을 때:

```bash
curl -G 'https://<HOST>/api/v1/audit/admin/audit-logs' \
  -H 'Authorization: Bearer <ACCESS_TOKEN>' \
  --data-urlencode 'requestId=REQUEST_ID_VALUE'
```

## 4. 성공 응답과 필드

```json
{
  "success": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "content": [
      {
        "id": 1024,
        "domain": "SCHEDULE",
        "action": "UPDATE",
        "targetType": "Schedule",
        "targetId": "10",
        "actorMemberId": 73,
        "description": "일정을 수정했습니다.",
        "details": "{\"schemaVersion\":1,\"target\":{\"type\":\"Schedule\",\"id\":\"10\",\"name\":\"정기 세션\",\"status\":\"OPEN\"}}",
        "ipAddress": "198.51.100.7",
        "outcome": "SUCCESS",
        "source": "EXPLICIT_RECORDER",
        "requestId": "request-123",
        "traceId": "0123456789abcdef0123456789abcdef",
        "createdAt": "2026-07-13T00:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 20,
    "first": true,
    "last": true,
    "numberOfElements": 1,
    "empty": false
  }
}
```

| 응답 field | nullable/표시 기준 |
| --- | --- |
| `id` | `Long` 숫자 |
| `domain`, `action`, `targetType` | 신규 row에서 필수 enum/string |
| `targetId` | 문자열. 숫자, UUID, 복합 ID를 모두 허용하며 대상이 없는 이벤트에서는 null 가능 |
| `actorMemberId` | null 가능. null을 곧 과거 사용자 정보가 없다는 뜻으로만 해석하고 `시스템/알 수 없음`으로 표시 |
| `description` | 정적 운영 문장. 저장 시 줄바꿈·민감 marker·500자 제한을 적용하며 위반 값은 안전한 기본 문장으로 대체 |
| `details` | JSON 문자열 또는 null/생략. object로 바로 캐스팅하지 않는다. 허용 snapshot scalar에 민감 marker/CRLF가 있으면 값은 `[REDACTED]`로 표시된다 |
| `ipAddress` | null 가능. 관리자 권한 화면에서만 제한적으로 표시 |
| `outcome` | `SUCCESS` 또는 `FAILURE` |
| `source` | `ANNOTATION`, `EXPLICIT_RECORDER`, `AUTHENTICATION_SERVICE`, `AUTHORIZATION_ASPECT`, `SYSTEM` |
| `requestId`, `traceId` | nullable. legacy row는 null/생략 가능 |
| `createdAt` | ISO-8601 Instant |

Jackson null inclusion 설정에 따라 nullable field가 JSON에서 생략될 수 있다. FE 모델은 null과 absent를 같은 legacy 상태로 처리한다.

`details`의 허용 key라도 `token`, `password`, `body`, `authorization`, `bearer`, `secret` marker 또는 CR/LF·로그 위조용 개행이 들어간 원문은 저장되지 않는다. 서버는 해당 scalar를 고정 `[REDACTED]`로 대체하고, 정상적인 name/nickname/school/status snapshot은 보존한다.

### 4.1 details parsing

DB는 `jsonb`지만 HTTP `details` 타입은 `String`이다. `details`가 non-null일 때만 parse한다.

```ts
type JsonScalar = string | number | boolean;
type DetailsSection = Record<string, JsonScalar>;

type AuditDetailsV1 = {
  schemaVersion: 1;
  actor?: DetailsSection;
  target?: DetailsSection;
  context?: DetailsSection;
  before?: DetailsSection;
  after?: DetailsSection;
};

type AuditDetailsParseResult =
  | { kind: "v1"; details: AuditDetailsV1 }
  | { kind: "fallback"; reason: string; raw: string | null };

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isJsonScalar(value: unknown): value is JsonScalar {
  return typeof value === "string" || typeof value === "number" || typeof value === "boolean";
}

function isDetailsSection(value: unknown): value is DetailsSection | undefined {
  if (value === undefined) return true;
  return isRecord(value) && Object.values(value).every(isJsonScalar);
}

function isAuditDetailsV1(value: Record<string, unknown>): value is AuditDetailsV1 {
  return value.schemaVersion === 1
    && isDetailsSection(value.actor)
    && isDetailsSection(value.target)
    && isDetailsSection(value.context)
    && isDetailsSection(value.before)
    && isDetailsSection(value.after);
}

function parseAuditDetails(raw: string | null | undefined): AuditDetailsParseResult {
  if (raw == null || raw.trim() === "") {
    return { kind: "fallback", reason: "legacy-null-or-empty", raw: null };
  }

  try {
    const parsed: unknown = JSON.parse(raw);
    if (typeof parsed === "number") {
      return { kind: "fallback", reason: "json-number", raw };
    }
    if (typeof parsed === "string") {
      return { kind: "fallback", reason: "legacy-string", raw };
    }
    if (!isRecord(parsed)) {
      return { kind: "fallback", reason: "unsupported-json-shape", raw };
    }
    if (parsed.schemaVersion !== 1) {
      return { kind: "fallback", reason: "unsupported-schema-version", raw };
    }
    if (!isAuditDetailsV1(parsed)) {
      return { kind: "fallback", reason: "invalid-schema-v1-shape", raw };
    }
    return { kind: "v1", details: parsed };
  } catch {
    return { kind: "fallback", reason: "invalid-json-or-legacy-text", raw };
  }
}
```

현재 지원 버전은 literal `schemaVersion === 1`뿐이다. `schemaVersion`이 없는 legacy object/plain String, JSON 숫자, `null`, malformed JSON, `schemaVersion`이 1이 아닌 object는 모두 `fallback`으로 보내고, 지원하지 않는 version을 `AuditDetailsV1`로 캐스팅하지 않는다. 위 예시는 unchecked cast 없이 section shape까지 확인한다. fallback 화면은 원문을 안전한 plain text로 표시하거나 `지원하지 않는 details 형식`으로 표시하며, `null`/absent는 정상적인 legacy 상태로 처리한다.

### 4.2 snapshot 표시 규칙

1. 행의 `targetType`/`targetId`로 기본 식별자를 표시한다.
2. `details.target.name`, `nickname`, `schoolName`, `status`가 있으면 **그 값을 당시 표시**로 사용한다.
3. `before`/`after`가 있으면 변경 전·후를 표시한다.
4. snapshot key가 없으면 현재 Member/Schedule API를 호출하지 않고 `과거 snapshot 없음`을 표시한다.
5. `targetId`가 삭제된 현재 entity를 가리키더라도 링크를 무조건 활성화하지 않는다. 링크가 필요하면 “현재 대상 보기”임을 별도 표시하고 과거 audit 표시와 섞지 않는다.
6. `description`은 저장 경계에서 정규화된 정적 plain text다. HTML로 해석하지 않고 escaping해 표시한다. 당시 회원명·nickname·학교명은 description이 아니라 권한 있는 화면의 `details` snapshot에서만 표시한다.

## 5. enum 참고

### `AuditOutcome`

| 값 | 의미 |
| --- | --- |
| `SUCCESS` | 정상 처리 결과 또는 성공 event |
| `FAILURE` | 로그인 실패·접근 거부 등 감사 대상 실패 |

### `AuditSource`

| 값 | 의미 |
| --- | --- |
| `ANNOTATION` | `@Audited` 자동 성공 경로 |
| `EXPLICIT_RECORDER` | application service가 명시적으로 조립한 rich/success event |
| `AUTHENTICATION_SERVICE` | 인증 service가 직접 기록한 보안 event |
| `AUTHORIZATION_ASPECT` | 접근 제어 aspect가 기록한 거부 event |
| `SYSTEM` | 그 밖의 명시 시스템 event |

### `AuditAction`

현재 enum은 [`AuditAction.java`](../../src/main/java/com/umc/product/audit/domain/AuditAction.java)를 source of truth로 사용한다. 주요 값은 `CREATE`, `UPDATE`, `DELETE`, `APPROVE`, `REJECT`, `CHECK`, `SUBMIT`, `REGISTER`, `WITHDRAW`, `LOGIN`, `LINK`, `UNLINK`, `ACCESS_DENIED`, `PUBLISH`, `CANCEL`, `REMIND`, `REORDER`, `FINALIZE`다.

### `Domain`

`domain`은 [`Domain.java`](../../src/main/java/com/umc/product/global/exception/constant/Domain.java)의 enum 문자열이다. 모든 도메인이 같은 수준으로 rich audit된다고 가정하지 않는다. 결과가 없는 도메인은 정상적인 빈 결과일 수 있다.

## 6. UI/에러 처리

- 결과는 `createdAt DESC`로 최신순이다. 서버가 정렬 option을 계약하지 않으므로 오래된 순 토글을 만들지 않는다.
- 비즈니스 액션 직후 row가 늦게 보일 수 있으므로 새로고침·backoff polling을 제공한다. 특정 지연 시간이나 실시간 stream을 보장하지 않는다.
- 401은 공통 인증 만료 흐름, 403은 권한 없음 안내, 400은 enum/Instant/파라미터 형식 오류, 5xx는 재시도 안내로 처리한다.
- 키워드/description LIKE 검색, CSV export, 실시간 streaming endpoint는 현재 제공되지 않는다.
- `details` snapshot과 정규화된 description은 표시용 데이터다. description은 plain text escaping하고, name/nickname/school 같은 snapshot은 현재 `AUDIT/READ` 권한 경계 밖으로 재전달하지 않는다.

## 7. 서버 운영 한계와 FE의 금지 가정

- 성공 감사 event는 항상 outbox를 거치며, FE가 outbox row나 relay 상태를 직접 조회하는 API는 없다.
- audit 저장 실패는 요청을 반드시 실패시키지 않는다. 화면에서 “audit 기록 완료”를 비즈니스 성공의 동의어로 사용하지 않는다.
- `SUCCESS`는 annotation/명시 recorder 모두 business commit에 결합되고 rollback 시 저장하지 않는다. `FAILURE`만 독립 transaction으로 보존한다.
- `audit_log` retention/archival, DB-level append-only, hash chain/WORM/SIEM은 현재 구현 범위가 아니다.
- 과거 audit row의 `details`는 null일 수 있고, `requestId`/`traceId`도 null일 수 있다.

자세한 서버 규칙은 [Logging Onboarding](../../onboarding/log/README.md), rollout 후속 과제는 [audit logging rollout plan](../backlog/audit-logging-rollout-plan.md)을 참고한다.
