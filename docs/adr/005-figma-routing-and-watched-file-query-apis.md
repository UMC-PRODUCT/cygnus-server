# ADR-005: Figma 라우팅 도메인 / 폴링 대상 파일에 Query API 를 추가한다

## Status

Superseded by [ADR-015](015-figma-comment-discord-forwarder-consolidated.md) (2026-05-09): ADR-003 / ADR-004 / ADR-005 / ADR-009 의 결정을 정합화한 통합 ADR 로 대체되었다. 본 ADR 의 Query UseCase / Service 분리, 응답 DTO 2단계 매핑, `discord_webhook_url` 마스킹, mention 본문 노출 정책, 페이지네이션 미도입 결정은 ADR-015 §Decision 7·8 에 그대로 흡수되었다. 본 문서는 변경 이력 보존용으로만 유지한다.

---

이전 Status 이력:

Accepted (2026-05-07): 6 개 커밋(query usecase/service · API 노출 · 테스트) 으로 본 ADR 의 결정을 그대로 구현 완료.

## Context

ADR-003 을 기반으로 `figma` 도메인에는 운영진이 라우팅 도메인과 폴링 대상 파일(이하 watched file) 을 직접 관리할 수 있는 admin API 가 도입되어 있다. 다만 현재 시점(2026-05-07) 기준으로 운영진에게 노출된 admin API 는 **상태 변경(Command)** 쪽에만 치우쳐 있다.

[FigmaRoutingDomainController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaRoutingDomainController.java)

- `POST /admin/figma/routing-domains` — 도메인 등록
- `DELETE /admin/figma/routing-domains/{domainId}` — 도메인 삭제
- `POST /admin/figma/routing-domains/{domainId}/mentions` — 멘션 추가
- `DELETE /admin/figma/routing-domains/mentions/{mentionId}` — 멘션 삭제

[FigmaWatchedFileController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaWatchedFileController.java)

- `POST /admin/figma/watched-files` — 파일 등록
- `POST /admin/figma/watched-files/{watchedFileId}/enable` — 활성화
- `DELETE /admin/figma/watched-files/{watchedFileId}` — 비활성화

이 구조에서 발생하는 운영상 문제는 다음과 같다.

1. **현재 등록 상태를 사람 눈으로 확인할 수 없다.** 운영진이 도메인을 새로 등록하기 전에 이미 같은 `domain_key` 가 있는지, fallback 도메인이 누구인지, 어느 도메인에 어떤 멘션이 붙어 있는지를 API 만으로는 알 수 없다. DB 콘솔에 직접 붙어 조회해야 하는데, 이는 운영자 권한과 별개라 접근성이 낮다.
2. **의사결정에 필요한 컨텍스트 부재.** "왜 이 댓글이 fallback 으로 갔는가?" 같은 운영 질문에 답하려면 LLM candidate 목록(=등록된 모든 라우팅 도메인의 `domain_key`)을 확인해야 하지만, 현재는 `LoadFigmaRoutingDomainPort.listAllDomains` 가 내부적으로만 사용되고 외부에 노출되지 않는다.
3. **watched file 의 sync 상태 가시성 부재.** `figma_watched_file` 에는 이미 `last_synced_comment_id`, `last_synced_at`, `last_error` 가 보관되지만, 어느 API 도 이 값을 반환하지 않는다. 그 결과 운영진이 "이 파일이 마지막으로 언제 동기화되었나", "에러로 멈춰 있는 파일이 있는가" 를 알 수 없다.
4. **프론트엔드(또는 admin 화면) 구현 차단.** 향후 admin 페이지에서 라우팅 도메인 목록 / 파일 목록을 보여주려면 Query API 가 반드시 필요하다. 현재는 프론트가 자체적으로 상태를 추적하거나, 등록 직후 응답값만 캐시하는 방식으로 우회해야 한다.

기술적 제약 / 환경은 다음과 같다.

- 프로젝트는 Hexagonal Architecture + CQRS 분리 컨벤션을 따른다 (`{Domain}CommandService` / `{Domain}QueryService`).
- 프로젝트 read 메서드 명명 규칙(`get*`, `find*`, `list*`, `batchGet*`, `search*`)은 도메인/애플리케이션 레이어에서는 엄격히 적용되지만 JPA 레이어는 Spring Data 컨벤션을 따른다.
- Outbound port `LoadFigmaRoutingDomainPort` / `LoadFigmaWatchedFilePort` 는 이미 존재하며, `findById` / `findDomainByKey` / `listAllDomains` / `listMentionsByDomainId` / `listEnabled(limit)` 등 대부분의 read 메서드가 갖춰져 있다.
- 컨트롤러에서 도메인 엔티티 직접 노출은 금지된다. 항상 `*Response` / `*Info` 로 매핑해야 한다.
- 운영진 외 일반 사용자에게는 노출되지 않는 admin API 영역이다 (`/api/v1/admin/figma/...`).

따라서 이번 결정에서는 다음을 정해야 한다.

- Query 전용 UseCase / Service 를 신설할지, 기존 `Manage*UseCase` 에 read 메서드를 끼워 넣을지.
- Query API 를 기존 `FigmaRoutingDomainController` / `FigmaWatchedFileController` 에 함께 둘지, 별도 `*QueryController` 로 분리할지.
- 응답 DTO 형식 — 도메인을 단독 조회할 때 mention 까지 함께 내려줄지, 분리할지.
- 어디까지 노출할지 — `last_error`, `discord_webhook_url` 같은 민감/장문 필드의 노출 범위.
- 페이지네이션 / 필터 정책 — 등록 규모가 작은 admin 데이터에 어디까지 적용할지.

## Decision

우리는 다음과 같이 결정한다.

1. **Query UseCase 와 Service 를 신설한다.** `GetFigmaRoutingDomainUseCase`, `GetFigmaWatchedFileUseCase` 두 개의 inbound port 를 만들고, 각각의 구현으로 `FigmaRoutingDomainQueryService`, `FigmaWatchedFileQueryService` 를 추가한다. 모두 `@Transactional(readOnly = true)`. 기존 `Manage*UseCase` 에는 read 메서드를 추가하지 않는다 (CQRS 컨벤션 준수).
2. **Query API 는 기존 컨트롤러에 그대로 추가한다.** `FigmaRoutingDomainController` / `FigmaWatchedFileController` 가 이미 도메인 묶음별로 분리되어 있으므로 별도 QueryController 를 만들지 않고 동일 컨트롤러에 `@GetMapping` 만 추가한다. 컨트롤러는 양쪽 UseCase (Manage / Get) 를 모두 의존하되, 메서드 단위로는 각각 한 가지 책임만 갖는다.
3. **신규 노출 endpoint** 는 다음 7 개로 한정한다.

   FigmaRoutingDomainController

   - `GET /admin/figma/routing-domains` — 등록된 도메인 전체 (mention 개수 포함, mention 본문은 미포함).
   - `GET /admin/figma/routing-domains/{domainId}` — 도메인 단건 (mention 목록 포함).
   - `GET /admin/figma/routing-domains/{domainId}/mentions` — 도메인의 멘션 목록만.

   FigmaWatchedFileController

   - `GET /admin/figma/watched-files` — 등록된 파일 전체. `enabled` 쿼리 파라미터(`true|false|null`) 로 필터링.
   - `GET /admin/figma/watched-files/{watchedFileId}` — 단건 상세 (sync 상태 포함).

4. **응답 DTO 는 `*Info` record + `*Response` record 로 두 단계 매핑한다.** 애플리케이션 레이어에서는 도메인 엔티티 → `*Info` 로 변환해 반환하고, 컨트롤러에서는 `*Info` 를 `*Response` 로 한 번 더 매핑한다. 이로써 도메인 엔티티는 컨트롤러 경계 밖으로 새지 않는다.
5. **Read 메서드 명명 규칙을 엄격히 적용한다.**
   - 단건 조회는 모두 `get*` (Optional 이 아닌 `T` 반환, 미존재 시 `FigmaDomainException`).
   - 다건 조회는 모두 `list*` (빈 리스트 반환, null 금지).
   - inbound port 와 service 모두 동일 컨벤션. JPA 레이어는 기존대로 Spring Data 컨벤션 유지.
6. **민감/장문 필드 노출 정책**:
   - `discord_webhook_url` 은 응답에서 마스킹한다 (`https://discord.com/api/webhooks/******/******`). 운영진이라도 콘솔 화면에서 webhook URL 이 그대로 노출되는 것을 막기 위해서다. 등록 시 입력값은 그대로 받지만, 조회 시에는 마스킹된 형태로만 내려간다.
   - `last_error` 는 그대로 노출한다. 운영자 디버깅 정보로 가치가 크고, 평문 텍스트로 민감 정보가 들어갈 가능성이 낮다.
   - 도메인 list 응답에는 `mentionCount` 만 포함하고 mention 본문은 단건 조회 또는 mentions 전용 endpoint 에서만 내려보낸다 (페이로드 크기 통제).
7. **페이지네이션은 도입하지 않는다.** 라우팅 도메인 / watched file 모두 운영자가 직접 등록하는 데이터로, 수십~수백 건 규모를 넘기지 않는다. `LoadFigmaWatchedFilePort.listEnabled(int limit)` 는 sync 스케줄러용으로 남기고, Query 측은 `listAll` / `listAllByEnabled(Boolean)` 를 새로 추가해 별도로 다룬다.

## Alternatives Considered

### 1. 기존 `Manage*UseCase` 에 read 메서드를 추가

`ManageFigmaRoutingDomainUseCase.listDomains()` 같은 형태로 기존 Command UseCase 에 read 메서드를 끼워 넣는 방식.

장점:

- 신규 인터페이스 / Service 클래스가 줄어 파일 수가 적게 늘어난다.
- 컨트롤러가 의존해야 할 빈이 1 개로 유지된다.

단점:

- `Manage*UseCase` 는 이미 `@Transactional` (write) 컨텍스트에 묶여 있다. read 전용 호출을 같은 인터페이스에 두면 트랜잭션 경계와 책임이 모호해진다.
- 프로젝트 컨벤션이 명시적으로 `{Domain}CommandService` / `{Domain}QueryService` 로 분리할 것을 요구한다 (CLAUDE.md §4).
- 단위 테스트에서 read 시나리오와 write 시나리오가 동일 mock 트리에 섞여 격리가 떨어진다.

선택하지 않은 이유:
CQRS 컨벤션을 우회할 만큼의 이득이 없다. 신규 Query Service 도입 비용은 작고, 트랜잭션/책임 분리의 이점이 더 크다.

### 2. 별도 `*QueryController` 분리

`FigmaRoutingDomainQueryController`, `FigmaWatchedFileQueryController` 를 만들어 `@GetMapping` 만 모으는 방식.

장점:

- 컨트롤러 단위에서도 Command/Query 가 물리적으로 분리된다.
- 보안 정책(예: read 전용 운영자 권한) 을 컨트롤러 단위로 부여하기 쉬워진다.

단점:

- URL prefix 가 동일한 두 개 컨트롤러가 생겨 OpenAPI 그룹이 갈라진다 (Swagger 상에서 하나의 도메인 묶음이 두 그룹으로 분리되어 가독성이 떨어진다).
- 권한 분리를 컨트롤러 단위로 할 만한 운영 요구가 현재 없다 (admin 운영자 1 그룹만 존재).
- 컨트롤러는 얇은 매핑 계층이라 책임 누출 위험이 적다 — Service 레이어에서 분리되어 있다면 충분하다.

선택하지 않은 이유:
도메인 단위 묶음(Tag, URL prefix) 이 깨지는 비용이 분리의 이점을 초과한다. Service 레이어 분리만으로 책임 분리 효과는 충분히 달성된다.

### 3. 단일 도메인 상세에 sync 상태 / mention 모두 묶어 한 응답으로 내려주기

`GET /admin/figma/routing-domains/{domainId}` 응답에 mention 본문 + recent activity 까지 묶어 보내는 방식.

장점:

- 프론트에서 한 번의 호출로 도메인 상세 페이지를 그릴 수 있다.
- HTTP round trip 이 줄어든다.

단점:

- 라우팅 도메인 도메인 객체와 mention, 그리고 (장기적으로) 발송 활동 로그까지 한 응답에 묶이면 응답 모델이 비대해진다.
- 일부 화면(예: mentions 만 갱신) 에서는 불필요하게 큰 페이로드가 내려간다.
- 향후 정렬/필터 요구가 mention 에 추가되면 단건 응답 안에서 풀어내기 어려워진다.

선택하지 않은 이유 (부분):
mention 까지는 단건 응답에 포함하는 것이 합리적이다 (라우팅 도메인의 핵심 부속). 다만 활동 로그 등 시간축 데이터까지 묶는 것은 과도하므로, mentions 전용 endpoint 를 별도로 두어 부분 갱신 요구를 분리한다.

### 4. `discord_webhook_url` 을 그대로 노출

마스킹 없이 등록값을 그대로 내려주는 방식.

장점:

- 등록 직후 운영자가 자신이 입력한 값이 정확한지 그대로 확인할 수 있다.
- 별도 마스킹 로직이 필요 없어 구현이 단순하다.

단점:

- Discord webhook URL 은 사실상 채널 발송 권한을 가진 토큰이다. admin 화면을 열어둔 채 자리를 비우거나, 화면 캡처가 외부로 흐르면 즉시 권한이 노출된다.
- 한 번 노출된 webhook URL 은 회수가 어렵고, 회수 시 해당 도메인의 디스코드 발송이 일시 중단된다.

선택하지 않은 이유:
운영 데이터 중 가장 강한 권한을 가진 값이라 마스킹 비용이 작다. 등록 직후 값 검증이 필요하다면 별도의 "test send" admin API 를 추후 도입하는 편이 안전하다.

### 5. 페이지네이션 도입

`Pageable` 기반으로 `Page<*Info>` 를 내려주는 방식.

장점:

- 데이터가 늘어나도 응답 크기가 안정적이다.
- 정렬/오프셋 정책이 표준화된다.

단점:

- 라우팅 도메인 / watched file 은 운영자가 명시적으로 등록하는 메타데이터로, 1,000 건을 넘기 어렵다.
- 페이지네이션은 응답 형태(`Page` 직렬화) 와 프론트 처리 비용을 모두 늘린다.
- 현재 운영 화면 요구사항에 정렬/필터가 들어 있지 않다.

선택하지 않은 이유:
규모와 요구사항 모두 페이지네이션 비용을 정당화하지 못한다. 향후 데이터가 임계치를 넘으면 그 시점에 별도 ADR 로 도입을 검토한다.

## Consequences

### Positive

- 운영진이 admin 화면이나 단순 HTTP 호출만으로 라우팅 도메인 / watched file 의 현재 상태를 직접 검증할 수 있다. DB 직접 접근 없이도 운영 가시성이 확보된다.
- `LoadFigmaRoutingDomainPort.listAllDomains` 가 외부로 노출되어, LLM candidate 목록과 운영 화면 사이의 sanity check 가 가능해진다.
- watched file 의 `last_synced_at` / `last_error` 가 노출되어 동기화 실패 파일을 운영진이 즉시 식별할 수 있다 (현재는 로그를 뒤져야 발견 가능).
- CQRS 분리가 이번 도메인에도 동일하게 적용되어, 다른 도메인과 일관된 구조를 유지한다.
- 프론트엔드 admin 페이지 작업이 비차단적으로 진행될 수 있다.

### Negative

- 신규 인터페이스(`Get*UseCase`), Service 2 개, Response DTO 다수가 추가되어 파일 수와 코드량이 증가한다.
- `discord_webhook_url` 마스킹으로 인해 운영자가 자신이 입력한 값을 사후 검증하기 어려워진다 (문자열 비교가 아니라 별도의 "발송 테스트" 흐름을 거쳐야 한다).
- 컨트롤러가 두 개의 UseCase 빈에 의존하게 되어 Mockito 기반 단위 테스트의 셋업이 약간 늘어난다.
- 운영자가 `GET /admin/figma/watched-files` 를 자주 호출하면 watched file 전체 select 쿼리가 발생한다 (단, 데이터 규모가 작아 성능 영향은 무시 가능).

### Neutral / Trade-offs

- 페이지네이션을 미도입하므로, 데이터가 임계치를 넘는 시점에 호환성을 깨지 않고 도입할 수 있는 응답 포맷(`*ListResponse` 래퍼) 을 처음부터 둘지 여부는 절충 사항이다. 본 ADR 에서는 `List<*Response>` 를 그대로 내려주되, 추후 도입 시 신규 endpoint(`/v2`) 또는 응답 래핑 변경을 통해 마이그레이션한다.
- mention 을 단건 응답에 포함하면서도 별도 mentions endpoint 를 두는 구조는 일부 데이터의 중복 노출(domain 단건과 mentions 단독 endpoint 가 동일 데이터를 노출) 을 만든다. 다만 부분 갱신 요구를 흡수하기 위한 의도적 중복이며, 응답 모델은 동일 record 를 재사용한다.
- 마스킹 정책은 추후 "운영자 권한 등급" 이 분리되면 풀어줄 수 있는 여지를 남긴다 (예: super-admin 에게는 평문 노출).

## Implementation Notes

### inbound port 신설

```java
// figma/application/port/in/GetFigmaRoutingDomainUseCase.java
public interface GetFigmaRoutingDomainUseCase {

    FigmaRoutingDomainSummaryInfo getDomainById(Long domainId);

    List<FigmaRoutingDomainSummaryInfo> listDomains();

    List<FigmaRoutingDomainMentionInfo> listMentionsByDomainId(Long domainId);
}

// figma/application/port/in/GetFigmaWatchedFileUseCase.java
public interface GetFigmaWatchedFileUseCase {

    FigmaWatchedFileInfo getById(Long watchedFileId);

    List<FigmaWatchedFileInfo> listAll(Boolean enabledFilter);
}
```

`enabledFilter` 는 nullable. `null` 이면 전체, `true|false` 면 해당 상태로 필터.

### Service 신설

- `FigmaRoutingDomainQueryService implements GetFigmaRoutingDomainUseCase` (`@Transactional(readOnly = true)`)
- `FigmaWatchedFileQueryService implements GetFigmaWatchedFileUseCase` (`@Transactional(readOnly = true)`)

기존 outbound port 는 그대로 사용한다. 단, 다음 메서드를 추가한다.

- `LoadFigmaWatchedFilePort.listAll(Boolean enabledFilter)` 또는 `LoadFigmaWatchedFilePort.listAll()` + `listAllByEnabled(boolean)` 분리.
- `LoadFigmaRoutingDomainPort.listAllDomains` 는 이미 존재. mention count 까지 한 번에 가져오려면 추후 N+1 회피용 `Map<Long, Integer>` 형태의 batchGet 메서드 도입을 검토 (1차 구현은 단순 `listMentionsByDomainId` 반복 후 size 집계로 시작).

### 응답 DTO

```java
// figma/adapter/in/web/dto/response/FigmaRoutingDomainResponse.java
public record FigmaRoutingDomainResponse(
    Long id,
    String domainKey,
    String description,
    String discordWebhookUrlMasked,
    boolean fallback,
    int mentionCount,
    List<FigmaRoutingDomainMentionResponse> mentions  // list 응답에서는 null
) { ... }

public record FigmaRoutingDomainMentionResponse(
    Long id,
    String mentionId,
    DiscordMentionType mentionType,
    String displayLabel
) { ... }

// figma/adapter/in/web/dto/response/FigmaWatchedFileResponse.java
public record FigmaWatchedFileResponse(
    Long id,
    String fileKey,
    String displayName,
    boolean enabled,
    String lastSyncedCommentId,
    Instant lastSyncedAt,
    String lastError
) { ... }
```

`*Info` record 는 `application/port/in/dto` 에 별도로 두고, `*Response.from(*Info)` 정적 팩토리로 매핑한다.

### 마스킹 유틸

`figma/application/service/DiscordWebhookUrlMasker` (단순 정적 메서드) 또는 `FigmaWebhookUrlMasker` 를 두고 다음 규칙을 적용한다.

```text
입력: https://discord.com/api/webhooks/123456789012345678/abcDEF...xyz
출력: https://discord.com/api/webhooks/****5678/****..xyz
```

마지막 4자만 노출. 응답 매핑 시점(`*Info → *Response` 변환 시) 에 적용한다.

### 컨트롤러 변경

```java
@RestController
@RequestMapping("/api/v1/admin/figma/routing-domains")
@RequiredArgsConstructor
public class FigmaRoutingDomainController {

    private final ManageFigmaRoutingDomainUseCase manageFigmaRoutingDomainUseCase;
    private final GetFigmaRoutingDomainUseCase getFigmaRoutingDomainUseCase;

    @Operation(summary = "[FIGMA-016] 라우팅 도메인 목록 조회")  // 본 ADR 채택 시점에는 015 로 계획되었으나, ADR-003 amendment 로 015 슬롯이 digest 에 할당되면서 016 으로 밀렸다.
    @GetMapping
    public List<FigmaRoutingDomainResponse> listDomains() { ... }

    @Operation(summary = "[FIGMA-017] 라우팅 도메인 단건 조회 (mention 포함)")
    @GetMapping("/{domainId}")
    public FigmaRoutingDomainResponse getDomain(@PathVariable Long domainId) { ... }

    @Operation(summary = "[FIGMA-018] 라우팅 도메인의 멘션 목록 조회")
    @GetMapping("/{domainId}/mentions")
    public List<FigmaRoutingDomainMentionResponse> listMentions(@PathVariable Long domainId) { ... }

    // 기존 [FIGMA-011] ~ [FIGMA-014] 그대로 유지
}
```

watched file 컨트롤러도 동일 패턴.

```java
@GetMapping
public List<FigmaWatchedFileResponse> listFiles(
    @RequestParam(required = false) Boolean enabled
) { ... }

@GetMapping("/{watchedFileId}")
public FigmaWatchedFileResponse getFile(@PathVariable Long watchedFileId) { ... }
```

OpenAPI 작업번호는 기존 [FIGMA-005] 다음으로 [FIGMA-008], [FIGMA-009] 등 빈 번호를 채운다 (Sync 컨트롤러가 [FIGMA-006/007/010] 을 사용 중이므로 충돌 회피).

### 테스트

- `FigmaRoutingDomainQueryServiceTest` (Mockito): list / 단건 / 미존재 시 예외, mention count 집계 검증.
- `FigmaWatchedFileQueryServiceTest` (Mockito): enabled filter 분기 (null/true/false), 단건 미존재 시 예외.
- `FigmaRoutingDomainControllerTest` / `FigmaWatchedFileControllerTest` (MockMvc): 마스킹된 webhook URL 이 응답에 포함되는지, 단건과 list 응답의 mention 포함 여부 차이.
- 모두 `@DisplayName` 한국어, Given/When/Then 구조 유지.

### 마이그레이션 영향

- DB 스키마 변경 없음 (기존 테이블/컬럼만 사용).
- 신규 outbound 메서드(`listAll(Boolean)`) 추가에 따라 `FigmaPersistenceAdapter` 만 수정. 다른 어댑터는 영향 없음.
- 기존 Command API 의 응답/시그니처는 변경하지 않는다 (호환성 유지).

### Implementation Plan (Commit 단위)

각 커밋은 단독 빌드/테스트 통과. Conventional Commits 준수.

1. `feat: figma 라우팅 도메인 query usecase / service 추가`
   - inbound port `GetFigmaRoutingDomainUseCase`, `*Info` record, `FigmaRoutingDomainQueryService`.
   - `LoadFigmaRoutingDomainPort` 는 기존 메서드만으로 충분하므로 변경 없음.
2. `feat: figma 라우팅 도메인 query api 노출`
   - 컨트롤러에 `@GetMapping` 3 개 추가 + Response DTO + webhook URL 마스킹 유틸.
3. `feat: figma watched file query usecase / service 추가`
   - inbound port `GetFigmaWatchedFileUseCase`, `*Info` record, `FigmaWatchedFileQueryService`.
   - `LoadFigmaWatchedFilePort.listAll(Boolean)` outbound 시그니처 추가 + adapter 구현.
4. `feat: figma watched file query api 노출`
   - 컨트롤러에 `@GetMapping` 2 개 추가 + Response DTO + sync 상태 필드 노출.
5. `test: figma query 흐름 테스트 추가` (위 커밋들에 분산 가능. 별도 묶음 시 본 커밋 단독으로 가능)
   - Service 단위 테스트 + MockMvc 컨트롤러 테스트.
6. `docs: ADR-005 / 운영 가이드 갱신`
   - 본 ADR Status 를 `Accepted` 로 전환하고, 운영 가이드 문서가 있다면 Query API 사용 방법 / 마스킹 정책을 추가.

## References

- 관련 ADR
    - [ADR-003: Figma 댓글 Discord 포워딩](003-figma-comment-discord-forwarder.md) — 본 ADR 의 전제. Command 측 도메인 모델과 admin API 가 여기서 정의됨.
- 기존 코드
    - [FigmaRoutingDomainController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaRoutingDomainController.java)
    - [FigmaWatchedFileController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaWatchedFileController.java)
    - [LoadFigmaRoutingDomainPort](../../src/main/java/com/umc/product/figma/application/port/out/LoadFigmaRoutingDomainPort.java)
    - [LoadFigmaWatchedFilePort](../../src/main/java/com/umc/product/figma/application/port/out/LoadFigmaWatchedFilePort.java)
    - [FigmaPersistenceAdapter](../../src/main/java/com/umc/product/figma/adapter/out/persistence/FigmaPersistenceAdapter.java)
- 컨벤션
    - [CLAUDE.md §2 Architecture & Domain Rules](../../CLAUDE.md) — CQRS 분리 / read 메서드 명명 규칙
