# 감사 로그(Audit Log) FE API 가이드

> 작성일: 2026-05-12
>
> 본 문서는 운영진(중앙운영사무국)이 백오피스에서 감사 로그를 손쉽게 조회할 수 있도록,
> FE 가 호출해야 할 API 와 UI 구성 시 고려할 서버측 의도를 정리한 문서입니다.
>
> 대상 독자: 백오피스 FE 개발자
> 관련 도메인 패키지: [audit/](src/main/java/com/umc/product/audit)

---

## 0. 한눈에 보기

- **무엇을 보여주는 화면인가?** 시스템 내에서 발생한 주요 상태 변경(생성/수정/삭제/승인 등)의 이력을 시간 순으로 보여주는 관리자 전용 화면입니다.
- **누가 볼 수 있는가?** **중앙운영사무국 국원**만 조회 가능합니다.
  ([AuditLogPermissionEvaluator.java](src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java))
- **데이터는 어떻게 쌓이나?** 백엔드 메서드에 붙은 `@Audited` 어노테이션이 메서드 정상 종료 후 이벤트를 발행하고, 트랜잭션 커밋 이후 **비동기로** DB 에 저장합니다.
- **호출해야 할 엔드포인트는?** `GET /api/v1/audit/admin/audit-logs` 입니다. 기존 `GET /api/v1/admin/audit-logs` 도 하위호환을 위해 동일하게 동작합니다.

---

## 1. 서버측 의도 (왜 이렇게 설계되었는가)

FE 가 화면을 만들 때 다음 설계 의도를 알고 있으면 더 자연스러운 UI 를 만들 수 있어요.

### 1.1 감사 로그는 "사후 기록" 이다 (Append-Only, Immutable)

- 저장된 감사 로그는 **수정/삭제되지 않습니다**. `AuditLog` 엔티티는 `BaseEntity` 를 상속하지 않으며
  쓰기 API 가 존재하지 않습니다. ([AuditLog.java](src/main/java/com/umc/product/audit/domain/AuditLog.java))
- → FE 도 마찬가지로 **읽기 전용 화면**으로만 구성해야 합니다. "수정" / "삭제" 버튼 같은 액션 영역을 두지 마세요.

### 1.2 비즈니스 트랜잭션과 분리되어 비동기로 적재된다

- `@Audited` 가 붙은 메서드 → AOP 가 `AuditLogEvent` 발행 → `@TransactionalEventListener(AFTER_COMMIT)` + `@Async("auditTaskExecutor")` 로 저장.
  ([AuditAspect.java](src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java),
  [AuditLogEventListener.java](src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java))
- 의미하는 바:
    1. **롤백된 트랜잭션의 로그는 절대 남지 않는다** (유령 로그 방지).
    2. 비즈니스 액션 직후 화면을 갱신해도 **수 ms 시차 뒤에 로그가 보일 수 있다**.
- → FE 가 어떤 액션 직후 "방금 발생한 감사 로그" 를 즉시 보여주는 식의 UX 는 권장하지 않습니다.
  필요하다면 1~2초 지연 후 재조회하거나, 사용자가 명시적으로 새로고침할 수 있게 하세요.

### 1.3 필터는 "검색" 이 아니라 "좁히기" 다

- 검색 파라미터는 **전부 선택(optional)** 이며, **AND 조건**으로 합쳐집니다.
  ([AuditLogQueryRepository.java](src/main/java/com/umc/product/audit/adapter/out/persistence/AuditLogQueryRepository.java))
- 키워드 검색(텍스트 like 검색)은 **지원하지 않습니다**. `description` 컬럼은 자유 텍스트지만
  서버 측 `where` 절에 사용되지 않습니다.
- → FE 는 "키워드 검색창" 보다 **필터 칩(chip)** 형태가 서버 의도에 부합합니다.

### 1.4 정렬은 항상 최신순 고정

- 결과는 `createdAt DESC` 로 고정 정렬되어 반환됩니다. Spring `Pageable` 의 `sort` 파라미터는 무시됩니다.
- → FE 에서 "오래된 순으로 보기" UI 는 만들지 마세요. 굳이 필요하면 서버에 정렬 옵션 추가 요청부터.

### 1.5 인덱스 설계가 곧 권장 필터 조합이다

마이그레이션 ([V2026.03.12.01.00__create_audit_log.sql](src/main/resources/db/migration/V2026.03.12.01.00__create_audit_log.sql)) 에서
다음 4 개 인덱스를 생성합니다.

| 인덱스                        | 권장 필터 시나리오                                |
|----------------------------|-------------------------------------------|
| `(domain, action)`         | "스케줄 도메인에서 발생한 생성 이벤트만 보기"                |
| `(target_type, target_id)` | 특정 엔티티 하나의 변경 이력 (단, 현재 API 는 미지원 — 4.6 참고) |
| `(actor_member_id)`        | 특정 회원이 일으킨 액션 전체                          |
| `(created_at)`             | 기간 필터, 그리고 정렬                             |

→ 즉, 서버가 효율적으로 답변할 수 있는 조합은 **도메인+액션 / 행위자 / 기간** 입니다. UI 도 이 축을 기본 필터로 두는 게 가장 자연스러워요.

---

## 2. UI 구성 제안

위의 의도에 맞춰 다음 구조를 권장합니다. 시안 그대로 구현해야 한다는 의미는 아니며, "왜 이렇게 권장하는지" 의 근거로 사용하세요.

### 2.1 페이지 레이아웃

```
┌─────────────────────────────────────────────────────────────┐
│ 감사 로그                                              [새로고침]│
├─────────────────────────────────────────────────────────────┤
│ ┌─ 필터 영역 ────────────────────────────────────────────┐ │
│ │ 도메인 [전체 ▼]   액션 [전체 ▼]   행위자 [회원 검색 🔍]    │ │
│ │ 기간   [2026-05-01 00:00] ~ [2026-05-12 23:59]           │ │
│ │                                          [초기화] [조회]   │ │
│ └────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│ 전체 N건 (page 0 / size 20)                                  │
├──┬───────────────┬─────────┬──────────┬──────────┬─────────┤
│  │ 시각          │ 도메인  │ 액션     │ 대상     │ 행위자   │
├──┼───────────────┼─────────┼──────────┼──────────┼─────────┤
│ ▶│ 05-12 14:23   │ SCHEDULE│ CREATE   │ #123     │ 홍길동   │
│ ▶│ 05-12 14:22   │ MEMBER  │ WITHDRAW │ #88      │ (시스템) │
│  │ ...                                                      │
├──┴───────────────┴─────────┴──────────┴──────────┴─────────┤
│             [◀ 이전]   1 2 3 ... 8   [다음 ▶]                │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 필터 컴포넌트 권장

| 필드           | 컴포넌트          | 비고                                                            |
|--------------|---------------|---------------------------------------------------------------|
| `domain`     | 단일 선택 드롭다운    | 옵션은 §3.4 Enum 참고. "전체" 선택 시 파라미터 미전송                          |
| `action`     | 단일 선택 드롭다운    | 옵션은 §3.4 Enum 참고. "전체" 선택 시 파라미터 미전송                          |
| 행위자          | 회원 검색 + 칩     | 회원 검색 모달에서 선택 후 `memberId` 만 서버에 전송. 화면에는 회원명 표시            |
| 기간 시작/종료     | `DateTimePicker` | 로컬 시간 입력 → 서버 전송 시 **ISO-8601 UTC** (`Instant`) 로 변환          |
| (전체 초기화)     | 보조 버튼         | 모든 파라미터를 빼고 `GET /audit-logs` 호출                              |

> 💡 키워드 검색창은 두지 않는 것을 권장합니다 (§1.3).

### 2.3 행 표시

- **시각**: 사용자 로컬 타임존으로 변환해서 `YYYY-MM-DD HH:mm` 형식 권장. 호버 시 초 단위까지 툴팁.
- **도메인 / 액션**: 그대로 표시하되 enum 코드(`SCHEDULE`)는 한국어 라벨로 매핑하면 가독성↑. 매핑은 FE 상수로 관리해도 충분합니다 (서버 측 한국어 라벨 API 는 아직 없음).
- **대상(target)**: `{targetType}#{targetId}` 형태로 한 줄. 예: `Schedule#123`.
- **행위자(actor)**:
    - `actorMemberId` 는 **nullable** 입니다. 시스템/스케줄러/비로그인 흐름에서는 null.
    - null 일 때는 "시스템" 으로 표시.
    - null 아니면 회원 정보 API 로 회원명을 별도 조회하거나, 같은 페이지의 다른 행에서 캐시.

### 2.4 행 펼치기 / 상세 패널

`description`, `details`, `ipAddress` 는 목록에서는 자리를 많이 차지하므로 **행 펼치기(▶) 또는 옆 패널**에 두는 것을 권장합니다.

- `description` — 사람이 읽도록 SpEL 로 만들어진 한 줄 설명 (예: `"일정이 생성되었습니다."`)
- `details` — `jsonb` (현재는 빈 객체 `{}` 가 주로 들어옴). 향후 변경 전/후 값이 들어갈 수 있으므로 **JSON 뷰어**로 보여주는 게 안전합니다.
- `ipAddress` — IPv4/IPv6 모두 가능. 그대로 표시.

### 2.5 페이지네이션

- Spring `Pageable` 표준 사용. **0-indexed** 입니다.
- 기본 `size=20`. UI 에서 20 / 50 / 100 정도의 옵션을 두는 것을 권장하지만, 너무 큰 값은 막아주세요 (200 이상 비권장).
- 응답에 `totalElements`, `totalPages`, `number`, `size` 등이 포함됩니다 (§3.2 참고).

### 2.6 빈 상태 / 로딩 / 에러

| 상태             | 안내 문구 예시                                                                            |
|----------------|------------------------------------------------------------------------------------|
| 결과 0건          | "조회 조건에 해당하는 감사 로그가 없습니다."                                                          |
| 권한 없음 (403)    | "Audit Log 는 중앙운영사무국 국원만 조회할 수 있습니다." (서버 메시지를 그대로 사용해도 됨)                          |
| 네트워크/서버 오류     | "조회 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."                                                  |

---

## 3. API 레퍼런스

### 3.1 엔드포인트

```
GET /api/v1/audit/admin/audit-logs
```

[AuditLogController.java](src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java)

- **인증**: `Authorization: Bearer {accessToken}` 필수
- **권한**: `ResourceType.AUDIT` + `PermissionType.READ` → 중앙운영사무국 국원에게만 허용
- Swagger 태그: `Audit | 감사 로그 조회` (`[AUDIT-001]`)

### 3.2 응답 래핑

모든 컨트롤러 응답은 `GlobalResponseWrapper` 를 통해 다음과 같이 래핑됩니다.

**성공 응답:**

```json
{
  "success": true,
  "code": "COMMON200",
  "message": "성공입니다.",
  "result": {
    "content": [ /* AuditLogInfo[] */ ],
    "pageable": { /* ... */ },
    "totalElements": 153,
    "totalPages": 8,
    "number": 0,
    "size": 20,
    "first": true,
    "last": false,
    "numberOfElements": 20,
    "empty": false
  }
}
```

> ⚠️ 이 엔드포인트의 `result` 는 Spring `Page<T>` 직렬화 형태이며, 다른 가이드에서 자주 등장하는 커스텀 `PageResponse<T>` (`content / page / size / totalElements / totalPages / hasNext / hasPrevious`) 와 **필드명이 다릅니다**.
> FE 에서는 `result.content`, `result.totalElements`, `result.totalPages`, `result.number` 를 사용하세요.

**실패 응답:**

```json
{
  "success": false,
  "code": "AUTH-XXXX",
  "message": "Audit Log는 중앙운영사무국 국원만 조회 가능합니다.",
  "result": null
}
```

### 3.3 Query Parameters

| 파라미터            | 타입                    | 필수 | 설명                                                                          |
|-----------------|------------------------|----|-----------------------------------------------------------------------------|
| `domain`        | `Domain` (enum string) | N  | 발생 도메인. 예: `SCHEDULE`                                                       |
| `action`        | `AuditAction` (enum)   | N  | 액션 종류. 예: `CREATE`, `UPDATE`                                                |
| `actorMemberId` | `Long`                 | N  | 행위자 memberId. 시스템 액션은 항상 null 이므로 이 필터로는 검색 불가                              |
| `from`          | `Instant` (ISO-8601)   | N  | 시작 시각 (포함, `createdAt >= from`)                                            |
| `to`            | `Instant` (ISO-8601)   | N  | 종료 시각 (포함, `createdAt <= to`)                                              |
| `page`          | `int`                  | N  | 0-indexed 페이지 번호. 기본값 0                                                     |
| `size`          | `int`                  | N  | 페이지당 개수. 기본값 20                                                             |
| `sort`          | `string`               | N  | **무시됨** — 서버에서 `createdAt DESC` 로 고정 정렬합니다                                  |

> `from` / `to` 는 `Instant` 라서 반드시 **UTC 기준 ISO-8601** 로 보내야 합니다.
> 예: `2026-05-12T00:00:00Z` 또는 `2026-05-12T09:00:00+09:00` 처럼 오프셋이 명시된 형태.
> 단순 `2026-05-12` 같이 보내면 파싱 실패로 400 이 떨어집니다.

### 3.4 주요 Enum

#### `Domain`

[Domain.java](src/main/java/com/umc/product/global/exception/constant/Domain.java)

`COMMON`, `AUTHENTICATION`, `AUTHORIZATION`, `MEMBER`, `CHALLENGER`, `ORGANIZATION`,
`CURRICULUM`, `SCHEDULE`, `COMMUNITY`, `NOTICE`, `FCM`, `SURVEY`, `RECRUITMENT`,
`TERMS`, `EMAIL`, `STORAGE`, `WEBHOOK`, `AUDIT_LOG`, `PROJECT`, `LLM`

> 모든 도메인이 실제로 감사 로그를 발행하는 것은 아닙니다 (현재는 `SCHEDULE` 위주). 드롭다운에는 전체 enum 을 노출해도 무방하나, "결과 0건" 빈 상태가 자주 나올 수 있다는 점을 유의하세요.

#### `AuditAction`

[AuditAction.java](src/main/java/com/umc/product/audit/domain/AuditAction.java)

| 값          | 의미                |
|------------|-------------------|
| `CREATE`   | 생성                |
| `UPDATE`   | 수정                |
| `DELETE`   | 삭제                |
| `APPROVE`  | 승인 (출석, 모집 지원 등)  |
| `REJECT`   | 거절                |
| `CHECK`    | 확인/조회 기반의 의미 있는 액션 |
| `SUBMIT`   | 제출                |
| `REGISTER` | 등록 (신규 가입 등)      |
| `WITHDRAW` | 탈퇴/취소             |

### 3.5 응답 본문(Result Item)

[AuditLogInfo.java](src/main/java/com/umc/product/audit/application/port/in/query/dto/AuditLogInfo.java)

```json
{
  "id": 1024,
  "domain": "SCHEDULE",
  "action": "CREATE",
  "targetType": "Schedule",
  "targetId": "123",
  "actorMemberId": 42,
  "description": "일정이 생성되었습니다.",
  "details": null,
  "ipAddress": "10.0.0.7",
  "createdAt": "2026-05-12T05:23:11.482Z"
}
```

필드별 노트:

- `targetId` — 문자열입니다. UUID 도, 숫자 ID 도, 컴포지트 key 도 들어올 수 있어요.
- `actorMemberId` — null 가능. UI 에서는 "시스템" 으로 표시 권장.
- `description` — null 가능. SpEL 미설정이거나 결과가 null 인 경우.
- `details` — null 또는 JSON 문자열. 빈 객체일 때도 있고, 미래에는 변경 사항이 들어올 수 있음 (`{"before": ..., "after": ...}` 형태가 자주 쓰이는 패턴).
- `ipAddress` — null 가능. 비-HTTP 컨텍스트(스케줄러, 콘솔 등) 에서 발생한 액션.
- `createdAt` — ISO-8601 UTC.

---

## 4. 호출 예시

### 4.1 cURL — 가장 단순한 호출 (모든 도메인, 최근 20건)

```bash
curl -X GET 'https://{HOST}/api/v1/audit/admin/audit-logs' \
  -H 'Authorization: Bearer eyJhbGciOi...'
```

### 4.2 cURL — 특정 기간의 SCHEDULE 도메인 CREATE 만

```bash
curl -G 'https://{HOST}/api/v1/audit/admin/audit-logs' \
  -H 'Authorization: Bearer eyJhbGciOi...' \
  --data-urlencode 'domain=SCHEDULE' \
  --data-urlencode 'action=CREATE' \
  --data-urlencode 'from=2026-05-01T00:00:00Z' \
  --data-urlencode 'to=2026-05-12T23:59:59Z' \
  --data-urlencode 'page=0' \
  --data-urlencode 'size=20'
```

### 4.3 TypeScript (fetch + URLSearchParams)

```ts
type AuditLogQuery = {
  domain?: string;        // Domain enum 문자열
  action?: string;        // AuditAction enum 문자열
  actorMemberId?: number;
  from?: string;          // ISO-8601 (Instant)
  to?: string;            // ISO-8601 (Instant)
  page?: number;          // 0-indexed
  size?: number;          // default 20
};

export async function searchAuditLogs(
  query: AuditLogQuery,
  accessToken: string,
) {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "") {
      params.set(k, String(v));
    }
  });

  const res = await fetch(
    `/api/v1/audit/admin/audit-logs?${params.toString()}`,
    {
      method: "GET",
      headers: { Authorization: `Bearer ${accessToken}` },
    },
  );

  if (!res.ok) {
    // 403 (권한 없음), 401 (토큰 만료) 등은 공통 인터셉터에서 처리 권장
    const err = await res.json().catch(() => null);
    throw new Error(err?.message ?? `조회 실패 (${res.status})`);
  }

  const body = await res.json();
  // body.result 는 Spring Page<AuditLogInfo>
  return body.result as {
    content: AuditLogInfo[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
}
```

### 4.4 React Query 패턴 (참고)

```ts
const useAuditLogs = (query: AuditLogQuery) =>
  useQuery({
    queryKey: ["audit-logs", query],
    queryFn: () => searchAuditLogs(query, getAccessToken()),
    keepPreviousData: true,        // 페이지 전환 시 깜빡임 방지
    staleTime: 10_000,             // 비동기 적재 시차 고려: 너무 짧지 않게
  });
```

> `staleTime` 을 0 으로 두면 사용자가 빠르게 페이지를 오갈 때 비동기 적재 시차(§1.2) 때문에 같은 조건이 다르게 보일 수 있어요. 10초 정도가 무난합니다.

### 4.5 자주 묻는 케이스

- **특정 일정(Schedule#123) 의 이력만 보고 싶다** — 현재 API 는 `targetType` / `targetId` 필터를 지원하지 않습니다.
  당장 필요하면 백엔드에 추가 요청을 주세요. 인덱스(§1.5)는 이미 준비되어 있습니다.
- **description 으로 like 검색하고 싶다** — 미지원 (§1.3). 필요 시 별도 요청.
- **CSV / Excel 내보내기** — 미지원. 임시로 페이지를 돌며 모으는 형태는 비권장 (`size` 를 크게 잡아 1회만 부르고 FE 에서 변환하는 정도가 한계).
- **실시간 스트리밍** — 미지원. 폴링이 필요하면 30초~1분 간격 권장.

---

## 5. 에러 / 권한 거부 다루기

| HTTP | 상황                            | 가이드                                                            |
|------|--------------------------------|----------------------------------------------------------------|
| 401  | 토큰 만료 / 미첨부                  | 공통 인터셉터에서 로그인 화면으로 리다이렉트                                       |
| 403  | 중앙운영사무국 국원이 아님           | 백오피스 진입 자체를 차단하는 게 이상적. 직링크 진입 시 안내 화면 노출                       |
| 400  | `from` / `to` 파싱 실패 등         | 입력 즉시 클라이언트에서 ISO 변환을 강제하면 거의 발생하지 않음. fallback 메시지 노출          |
| 500  | 그 외 서버 오류                    | 재시도 버튼 + 일반 안내                                                  |

권한 메시지는 컨트롤러의 `@CheckAccess(message = ...)` 로 고정되어 있어요:
> `"Audit Log는 중앙운영사무국 국원만 조회 가능합니다."`
이 메시지를 FE 에서 그대로 사용해도 자연스럽습니다.

---

## 6. 향후 확장 가능성 (FE 도 알아두면 좋은 것)

지금 당장 구현되어 있진 않지만, **인덱스/데이터 모델은 이미 준비된** 항목들입니다. UI 를 만들 때 "나중에 붙기 쉬운 구조" 로 두면 좋습니다.

- 대상 엔티티 단위 조회 (`targetType` / `targetId` 필터)
- `details` 의 `before` / `after` diff 표시
- 다중 도메인 / 다중 액션 선택 (현재는 단일)
- 정렬 옵션 (오래된 순)
- 백오피스 CSV 내보내기

새 필터가 추가되어도 위 §2.2 의 칩(chip) 형태라면 자연스럽게 확장됩니다.

---

## 7. 빠른 체크리스트

FE 구현 시 다음을 한 번씩 확인해주세요.

- [ ] 모든 필터 파라미터는 **빈 값이면 쿼리 스트링에서 제외**한다 (서버는 null 만 인식).
- [ ] `from` / `to` 는 **ISO-8601 + 타임존 정보** 가 포함된 문자열로 보낸다.
- [ ] 페이지는 **0-indexed**.
- [ ] 응답은 `result.content` 안에 배열이 있고, 페이지 정보는 `result.number / totalElements / totalPages / size`.
- [ ] `actorMemberId` 가 null 일 수 있다 (시스템 액션).
- [ ] `description`, `details`, `ipAddress` 가 null 일 수 있다.
- [ ] 403 응답 메시지를 사용자에게 자연스럽게 전달한다.
- [ ] 시간 표시는 사용자 로컬 타임존으로 변환한다.
- [ ] 정렬은 최신순 고정이라는 점을 UI 에 반영한다 (정렬 토글 UI 없음).

---

## 8. 참고 파일

- [AuditLogController.java](src/main/java/com/umc/product/audit/adapter/in/web/AuditLogController.java) — 엔드포인트 정의
- [SearchAuditLogQuery.java](src/main/java/com/umc/product/audit/application/port/in/query/dto/SearchAuditLogQuery.java) — 검색 조건 DTO
- [AuditLogInfo.java](src/main/java/com/umc/product/audit/application/port/in/query/dto/AuditLogInfo.java) — 응답 아이템 DTO
- [AuditLog.java](src/main/java/com/umc/product/audit/domain/AuditLog.java) — 도메인 엔티티 (immutable)
- [AuditAction.java](src/main/java/com/umc/product/audit/domain/AuditAction.java) — 액션 enum
- [Domain.java](src/main/java/com/umc/product/global/exception/constant/Domain.java) — 도메인 enum
- [AuditLogQueryRepository.java](src/main/java/com/umc/product/audit/adapter/out/persistence/AuditLogQueryRepository.java) — 검색 구현(QueryDSL)
- [AuditAspect.java](src/main/java/com/umc/product/audit/adapter/in/aop/AuditAspect.java) — `@Audited` AOP
- [AuditLogEventListener.java](src/main/java/com/umc/product/audit/adapter/in/event/AuditLogEventListener.java) — 비동기 저장 리스너
- [AuditLogPermissionEvaluator.java](src/main/java/com/umc/product/audit/application/service/AuditLogPermissionEvaluator.java) — 권한 평가
- [V2026.03.12.01.00__create_audit_log.sql](src/main/resources/db/migration/V2026.03.12.01.00__create_audit_log.sql) — 테이블 스키마 & 인덱스
