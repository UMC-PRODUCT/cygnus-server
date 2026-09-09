# ADR-015: Figma 댓글을 OAuth 폴링 + LLM 분류 + 시간창 단일 유즈케이스 + SUPER_ADMIN 인가로 Discord에 포워딩한다

## Status

Proposed (2026-05-09): ADR-003 / ADR-004 / ADR-005 / ADR-009 의 결정과 amendment 를 정합화해 단일 문서로 병합한 통합본. 본 ADR 채택 시점에 위 4 개 ADR 은 `Superseded by ADR-015` 로 전환된다.

## Context

UMC PRODUCT 운영진은 디자인 리뷰와 기획 검토 과정에서 Figma 파일의 댓글로 의사소통을 한다. 이 흐름에는 다음과 같은 운영상 문제가 있었다.

1. **알림 누락** — Figma 기본 알림은 멘션된 사람/watcher 에게만 도달한다. 담당 파트장이 watcher 가 아니거나 알림을 끄면 회신이 지연된다.
2. **소통 채널 분리** — 팀 비동기 커뮤니케이션은 Discord 에 모이지만 디자인 피드백은 Figma 에 고립된다.
3. **담당자 라우팅 부재** — 댓글이 어느 파트(디자인/기획/개발) 책임인지 사람이 매번 판단해야 한다.
4. **운영 가시성 부재** — 라우팅 도메인 / 폴링 대상 파일을 admin API 만으로 사람 눈으로 확인할 수 없다 (DB 콘솔에 직접 접근해야 했다).
5. **인가 공백** — `/api/v1/admin/figma/...` prefix 만으로는 일반 챌린저 호출을 막지 못한다. 인가 없이 노출되면 누구나 Discord webhook URL 을 자기 채널로 바꾸거나, OAuth 위임을 자신으로 등록하거나, Figma rate limit 을 임의 소진할 수 있었다.

이 문제는 단일 도메인 (`com.umc.product.figma`) 안에서 해결되지만, 다음 6 개의 결정이 함께 묶여야 정합성이 유지된다.

- **인증** — Figma OAuth2 vs PAT
- **트리거 / 동기화 시맨틱** — 폴링 주기, ID boundary vs 시간창, 진입점 분리 vs 단일 유즈케이스, 중복 발송 가드
- **분류 / 라우팅** — 페이지명 기반 vs LLM 기반, 도메인 라우팅 단위, fallback 정책
- **발송 포맷** — 댓글 단위 webhook 호출 vs 도메인 단위 묶음 embed
- **운영 가시성** — Query API 노출, 응답 마스킹, 페이지네이션
- **인가** — `ResourceType` 분리, `READ` / `MANAGE` 단위, SUPER_ADMIN 단일 기준

### 기술 환경 / 제약

- Figma 의 `FILE_COMMENT` webhook 은 Organization/Enterprise 플랜 한정이며 현재 팀 플랜에서 사용 불가하다. REST 폴링이 사실상 유일한 선택지다.
- Figma REST `GET /v1/files/:file_key/comments` 는 OAuth2 access token 또는 PAT 로 호출 가능하다.
- 서버는 이미 [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java) 로 Discord 발송 인프라를 보유한다.
- Spring Security `oauth2-client` 는 사용자 로그인용으로 운영 중이지만, 본 통합은 운영자 1인의 위임(delegation) OAuth 이므로 사용 목적이 다르다.
- 프로젝트 인가 추상은 `@CheckAccess(resourceType, permission)` + `ResourcePermissionEvaluator` 패턴으로 통일되어 있다 (`com.umc.product.authorization`). `ChallengerRoleType.SUPER_ADMIN.isSuperAdmin()` 헬퍼가 존재한다.
- 프로젝트 컨벤션은 Hexagonal Architecture + CQRS 분리 (`{Domain}CommandService` / `{Domain}QueryService`) 와 read 메서드 명명 규칙 (`get` / `find` / `list` / `batchGet` / `search`) 을 강제한다.
- `application.yml` / 테스트 프로필 모두 `spring.jpa.hibernate.ddl-auto: validate` 이므로 신규 엔티티 추가 전에 마이그레이션이 선행되어야 한다.
- figma 도메인의 모든 마이그레이션은 본 ADR 채택 시점까지 develop / main 에 미머지 상태였다.

### 결정해야 할 것

- 인증 방식 (OAuth2 위임 vs PAT)
- 트리거 (폴링 주기, ID boundary vs 시간창, 단일 유즈케이스 vs 진입점 분리, 중복 발송 가드)
- 분류 규칙 (LLM 분류 + 도메인 라우팅, fallback 정책, 분류 캐시 계층)
- 멘션 / 발송 포맷 (도메인 단위 묶음 embed)
- 도메인 배치 (figma + llm 분리)
- Query API 노출 범위 / 마스킹 정책 / 페이지네이션
- 인가 (ResourceType, READ/MANAGE, SUPER_ADMIN, callback 비대칭)

## Decision

우리는 다음과 같이 결정한다. 결정은 6 개 묶음 (인증·트리거·분류·발송·운영 가시성·인가) 으로 정렬한다.

### 1. Figma OAuth2 Authorization Code Flow 를 사용한다 (인증)

운영진 1인이 최초 1회 동의 화면을 통과해 발급받은 refresh token 을 서버에 저장하고, 이후 서버가 access token 을 자동 갱신한다. PAT 는 발급한 운영진 이탈 시 시스템이 즉시 멈추고 권한 범위 제어가 약하므로 사용하지 않는다. refresh token / access token 은 application property 기반 대칭키 (`FIGMA_TOKEN_ENCRYPTION_KEY`) 로 암호화해 보관한다.

OAuth callback 만 `@Public` 으로 유지한다 — Figma 의 redirect 는 Authorization 헤더 없이 들어오므로 인증을 강제하면 호출이 100% 실패한다. state 의 단기 수명 + owner memberId binding 으로 위변조를 흡수한다.

### 2. LLM 분류 + 도메인 라우팅을 채택한다 (분류)

댓글 본문을 LLM 으로 분석해 운영진이 등록한 라우팅 도메인 키 (`figma_routing_domain.domain_key`) 중 하나로 분류한다. 후보 도메인 외 응답 / LLM 호출 실패 / 매칭 미스 시 `fallback=true` 도메인으로 라우팅한다. 페이지/프레임 이름은 라우팅 키가 아니라 embed 의 부가 정보로만 사용한다.

페이지명 기반 명시적 매핑을 채택하지 않는 이유: Figma 페이지명은 디자이너가 자유롭게 변경하는 자산이라 라우팅 키로 강제하기 어렵고, 실제 책임은 페이지가 아니라 "어떤 도메인 작업" 인지에 따라 갈라지기 때문이다. 매핑 갱신 비용이 LLM 호출 비용보다 컸다.

LLM 호출 추상은 별도 도메인 `com.umc.product.llm` 으로 분리하고, `ChatCompletionPort` 를 두어 provider 별 어댑터 (Mock / OpenAI / Gemini / Spring AI) 를 `app.llm.provider` 로 교체할 수 있게 한다. 본 ADR 채택 시점에는 `MockChatCompletionAdapter` 만 활성화한다 (후보 중 무작위 응답).

분류 결과는 3-tier 캐시로 보관한다.

- **L1**: Caffeine in-memory (TTL 5분) — 같은 인스턴스 내 동일 commentId 재호출 흡수.
- **L2**: `figma_comment_classification` 영구 캐시 — 재시작/다중 인스턴스에서도 LLM 재호출 차단. mock provider 응답과 후보 외 응답은 저장하지 않는다.
- **L3**: 실제 LLM batch 호출.

같은 commentId 가 여러 시간창에 중복으로 들어와도 LLM 호출이 추가로 발생하지 않는다.

### 3. 시간창 기반 단일 유즈케이스로 동기화·digest·preview 를 통합한다 (트리거)

Figma 댓글 처리의 본체는 `[from, to]` 시간창을 입력받아 "활성 파일 전체에서 해당 창의 댓글을 모아 → 도메인별로 분류 → 도메인 묶음 embed 발송" 하는 단일 유즈케이스 `SummarizeFigmaCommentsUseCase` 로 통합한다. 본체 구현은 `FigmaCommentSummaryService` (Mode 분기 없는 단일 메서드) 가 담당한다.

세 진입점은 시간창 + 부가 정책 플래그 (`dryRun`, `force`, `advanceCursor`) 로 정의한다.

- **스케줄러 sync**: `[lastWindowEnd, now]` 호출. `lastWindowEnd` 는 `figma_summary_cursor` (단일 행 전역 cursor) 에서 읽고, 발송 성공 후 `now` 로 갱신한다.
- **admin digest**: 운영진이 명시한 `[from, to]` 호출. cursor 비변경, `force=true` 가 기본 (catch-up 시맨틱).
- **admin preview**: 동일한 시간창 입력 + `dryRun=true`. Discord 발송 / dispatch 기록 / cursor advance 모두 건너뛰고 묶음 결과만 응답.

기존 `last_synced_comment_id` (Figma REST 응답 정렬에 의존하던 ID boundary) 는 폐기한다. ID boundary 는 정렬 가정이 깨지면 누락/중복이 동시에 발생할 수 있고, 시간창 시맨틱이 운영진 멘탈모델 ("어느 시각까지 봤는가") 에 더 직접 매핑되기 때문이다.

폴링 주기는 5분을 기본값으로 한다. 운영 데이터 확보 후 조정한다.

### 4. 중복 발송은 `figma_comment_dispatch` 테이블로 가드한다 (트리거)

`last_synced_comment_id` 가 담당하던 발송 가드 책임은 신규 테이블 `figma_comment_dispatch (comment_id UNIQUE, domain_id, dispatched_at)` 로 옮긴다.

- 시간창 안의 댓글 중 이미 dispatch 행이 있는 commentId 는 `force=false` 일 때 발송 단계에서 제외한다 (preview 응답에는 그대로 노출하되 `alreadyDispatched=true` 로 표시).
- 발송 성공 후 `(comment_id, domain_id, dispatched_at=now)` 를 insert 한다. 발송 트랜잭션과 분리된 별도 트랜잭션 (`Propagation.REQUIRES_NEW`) 으로 실행해 한 도메인 발송 실패가 다른 도메인의 dispatch 기록을 막지 않게 한다.
- admin digest 는 기본 `force=true` 로 dispatch 를 무시한다 (catch-up).
- 보존 기간은 90 일이며, `dispatched_at` 인덱스 + 별도 회수 잡으로 정리한다.

`figma_summary_cursor` 는 단일 행 전역 cursor 로, 다중 인스턴스 환경에서는 `SELECT ... FOR UPDATE` 또는 ShedLock 으로 직렬화한다. cursor 의 advance 는 `newEnd >= lastWindowEnd` 일 때만 적용해 같은 시간창 재발송을 방어한다. 파일별 cursor 가 아니라 전역 cursor 를 채택하는 이유는 (i) 도메인 묶음 발송이 cross-file 이고, (ii) 단일 cursor 가 운영진 멘탈모델에 직접 매핑되기 때문이다.

### 5. 발송 단위는 "도메인 1건 = 묶음 embed 메시지 1건" 이다 (발송)

같은 시간창 안의 같은 라우팅 도메인 댓글들은 cross-file 로 한 메시지의 embed 안에 모인다. 댓글 1 건 = embed `fields[]` 의 1 개 entry (작성자 / 파일+페이지 / 본문 / 댓글 deeplink). Discord 제약 (fields 25/embed, embeds 10/message, embed 합산 ≤ 6000자) 초과 시 어댑터가 자동 분할 발송한다.

도메인 단위로 멘션 대상 (Discord role 또는 user) 을 N 개까지 영속화 (`figma_routing_domain_mention`) 한다. 멘션은 알림 발생을 위해 첫 페이지 메시지의 외부 `content` 영역에 한 번만 출력한다 (`allowed_mentions.parse=["roles","users"]`).

```jsonc
{
  "content": "<@&123> <@456>",                         // 첫 페이지 메시지에만 멘션
  "embeds": [{
    "title": "[Figma] {domainKey} 신규 댓글 {N}건",     // 분할 시 " (1/3)" 접미사
    "color": 15879710,                                  // 0xF24E1E
    "fields": [
      {
        "name": "👤 {작성자} · {파일} / {페이지명}",
        "value": "{댓글 본문}\n🔗 [열기]({deeplink})",
        "inline": false
      }
    ],
    "footer": {"text": "Figma · {windowFrom} ~ {windowTo}"},
    "timestamp": "{묶음 내 최신 댓글 시각, ISO8601}"
  }],
  "allowed_mentions": {"parse": ["roles", "users"]}
}
```

댓글 1 건 = webhook 호출 1 회 방식은 채택하지 않는다 — Discord rate limit (per-URL 약 5 req/2s, 30 req/분) 부담과 채널 가독성 저하가 운영진 알림 신뢰를 떨어뜨리는 1차 요인이었다. 도메인 단위 묶음으로 호출 수가 급감하고 (`전형적 5분 사이클 K<25 → N개 도메인이면 N개 메시지`), embed `fields[]` 로 댓글 모음을 한 곳에 정리할 수 있다.

### 6. 신규 도메인 `com.umc.product.figma` + `com.umc.product.llm` 을 분리한다 (도메인 배치)

외부 시스템 (Figma) 통합은 별도 컨텍스트로 두고, 기존 `notification` 도메인은 "범용 알림 발송 인프라" 로 유지한다. `figma` 도메인이 `notification` 의 발송 포트 (`SendDiscordMentionPort`) 를 호출하는 구조다.

LLM 호출 추상은 `com.umc.product.llm` 도메인으로 분리해, 추후 Notion / Slack 등 다른 외부 댓글 시스템도 동일 분류기를 재사용할 수 있게 한다.

### 7. Query UseCase / Service 를 신설하고 admin Query API 를 노출한다 (운영 가시성)

CQRS 컨벤션에 따라 `GetFigmaRoutingDomainUseCase`, `GetFigmaWatchedFileUseCase` 두 개의 inbound port 를 신설하고, 각각 `FigmaRoutingDomainQueryService`, `FigmaWatchedFileQueryService` 로 구현한다 (`@Transactional(readOnly = true)`). 기존 `Manage*UseCase` 에는 read 메서드를 추가하지 않는다.

Query API 는 기존 `FigmaRoutingDomainController` / `FigmaWatchedFileController` 에 그대로 추가한다 (별도 QueryController 는 신설하지 않는다 — URL prefix 분리 비용이 분리 이점을 초과한다). 노출 endpoint 는 다음 5 개로 한정한다.

- `GET /admin/figma/routing-domains` — 등록된 도메인 전체 (mention 본문 미포함, mention 개수만 포함).
- `GET /admin/figma/routing-domains/{domainId}` — 단건 (mention 목록 포함).
- `GET /admin/figma/routing-domains/{domainId}/mentions` — 멘션 목록만.
- `GET /admin/figma/watched-files` — 파일 전체 (`enabled` 쿼리 파라미터로 필터).
- `GET /admin/figma/watched-files/{watchedFileId}` — 단건 상세 (sync 상태 포함).

페이지네이션은 도입하지 않는다. 라우팅 도메인 / watched file 은 운영자 직접 등록 메타데이터로 수십~수백 건 규모를 넘지 않는다.

### 8. 응답 DTO 는 `*Info` + `*Response` 2단계 매핑, 민감 필드는 마스킹한다 (운영 가시성)

애플리케이션 레이어에서는 도메인 엔티티 → `*Info` 로 변환해 반환하고, 컨트롤러에서는 `*Info` → `*Response` 로 한 번 더 매핑해 도메인 엔티티가 컨트롤러 경계 밖으로 새지 않게 한다.

- `discord_webhook_url` 은 응답에서 **마지막 4 자만 노출** 하는 형태로 마스킹한다 (`https://discord.com/api/webhooks/****5678/****..xyz`). 이 URL 자체가 채널 발송 권한 토큰이므로 admin 화면 캡처/공유로 노출되는 risk surface 를 줄인다.
- `last_error` 는 그대로 노출한다 (운영자 디버깅 가치, 평문 텍스트로 민감 정보 들어갈 가능성 낮음).
- list 응답에는 `mentionCount` 만, 단건 / mentions 전용 endpoint 에는 mention 본문을 포함한다 (페이로드 크기 통제).

read 메서드 명명 규칙: 단건 조회는 `get*` (`T` 반환, 미존재 시 `FigmaDomainException`), 다건은 `list*` (빈 리스트, null 금지). inbound port 와 service 모두 동일 컨벤션. JPA 레이어는 Spring Data 컨벤션 유지.

### 9. `ResourceType.FIGMA` + `FigmaPermissionEvaluator` 로 SUPER_ADMIN 만 모든 figma admin endpoint 에 접근 가능하게 한다 (인가)

`com.umc.product.authorization.domain.ResourceType` 에 `FIGMA("figma", "Figma 통합", Set.of(READ, MANAGE))` 를 추가한다. 권한 단위는 두 개로 분리한다.

- `READ` — 모든 GET 조회 endpoint.
- `MANAGE` — POST / DELETE 류 (라우팅 도메인 / 멘션 / watched file CRUD, sync, digest, OAuth start).

`FigmaPermissionEvaluator` 는 `READ` / `MANAGE` 모두 SUPER_ADMIN 만 통과시킨다 (`isSuperAdmin()` 단일 기준). 중앙운영사무국 총괄단까지 허용하지 않는 이유: Figma OAuth refresh token 등 민감 자산을 다루고, 잘못된 webhook 등록은 회수가 어려워 권한 보유자 수가 곧 risk surface 다. 지원하지 않는 권한 enum 이 들어오면 `false` (방어적 기본값).

모든 figma admin endpoint 에 `@CheckAccess` 를 적용하되, **`FigmaOAuthController#callback` 만 `@Public` 유지** 한다 (Decision §1 사유와 동일).

`@CheckAccess` 는 resourceId 없이 type-level 권한 체크 형태로 사용한다 (`ResourcePermission.ofType(...)`). Figma admin 자산은 운영자 단일 그룹의 글로벌 자산이므로 리소스 단위 권한 차등을 도입하지 않는다. 향후 멀티 테넌시 / 파트별 위임이 필요해지면 별도 ADR 로 도입한다.

### 10. figma 도메인 마이그레이션은 단일 파일로 통합한다

ADR-003 amendment 1·2차 + ADR-004 시점에 figma 도메인 마이그레이션 4 종이 모두 develop / main 에 미머지 feature 브랜치 한정 변경이었다. 따라서 deprecate-then-drop / 점진 마이그레이션 패턴 대신 단일 `V2026.05.07.10.00__create_figma_tables.sql` 로 병합하면서 `last_synced_comment_id` 정의 누락, `figma_part_route` 정의 누락, `figma_summary_cursor` / `figma_comment_dispatch` 신규 정의를 한 번에 처리한다.

병합 파일 안에는 각 테이블 블록 위에 `-- ADR-015 §Decision N: ...` 주석을 남겨, git blame 없이도 의도가 추적되게 한다. 이후 figma 도메인 스키마 변경은 신규 V2026.MM.DD 파일을 추가하는 정상 패턴으로 진행한다.

## Alternatives Considered

### A-1. Personal Access Token (PAT) 인증

운영진 1 인이 본인 계정에서 PAT 를 발급해 서버에 직접 주입.

장점: OAuth 콜백 / 토큰 갱신 / 동의 화면 불필요. Figma OAuth 앱 등록 절차 생략.

단점: 발급자 이탈 시 모든 Figma 연동 즉시 중단. 권한 범위 제어 약함. 노출 시 계정 전체 권한 노출. 인수인계 시 수동 재발급/재배포.

선택하지 않은 이유: 운영진 인수인계 비용과 보안 위험이 OAuth 도입 비용보다 크다. OAuth 는 동의자가 바뀌어도 같은 절차로 재인증이 가능하며, scope 단위 권한 부여가 가능하다.

### A-2. Figma Webhooks v2

Figma 가 제공하는 webhook 으로 댓글 이벤트 즉시 수신.

장점: 폴링 비용 없이 거의 실시간. Figma 가 재시도까지 보장.

단점: `FILE_COMMENT` webhook 은 Organization/Enterprise 플랜 한정 → 현 팀 플랜에서 사용 불가. webhook 수신 endpoint 외부 노출 + signature 검증 + retry 정책 추가 부담.

선택하지 않은 이유: 플랜 자체가 부족해 사용 불가. Enterprise 업그레이드 시 본 ADR 을 superseding 하는 방향으로 재검토.

### A-3. 페이지명 / 프레임명 기반 명시적 매핑 분류

`figma_part_route(file_key, page_name) → 파트 role` 매핑 테이블로 페이지명만으로 라우팅.

장점: 분류 오류 없음. 운영진 통제 가능. LLM 호출 비용/지연 없음.

단점: 페이지명은 디자이너가 자유롭게 변경 → 매핑 갱신 비용이 LLM 호출 비용보다 큼. "로그인 화면 페이지에 달린 댓글" 이라도 실제 책임은 인증/회원/UI 등으로 갈라져 페이지 ↔ 파트 매핑이 부적절. 추후 Notion / Slack 등 다른 외부 시스템에 그대로 적용하기 어려움.

선택하지 않은 이유: 페이지명 변경 빈도와 분류 표현력 한계로 LLM 분류로 전환했다. 분류 오류 리스크는 fallback 도메인 + preview API 로 흡수 가능하다고 판단.

### A-4. ID boundary (`last_synced_comment_id`) 유지 + Mode 분기

`Mode.SYNC` / `Mode.DIGEST` / preview 의 본체 분기를 유지하면서 ID 기반 boundary 로 진행 상태 관리.

장점: 마이그레이션 비용 없음. 단일 컬럼 가드. 운영 검증된 흐름.

단점: Figma REST 응답 정렬에 묵시 의존 (정렬 가정이 깨지면 누락/중복 동시 발생). 진입점 추가마다 분기 누적. 운영 메트릭 ("이번 사이클에서 본 시간창") 표현 어려움.

선택하지 않은 이유: 중기적으로 진입점이 추가될수록 Mode 분기 비용이 누적되며, 시간창 시맨틱이 일반적이고 운영진 멘탈모델에 직접 매핑된다.

### A-5. dispatch 테이블 없이 cursor 만으로 중복 가드

`figma_summary_cursor.last_window_end` 만으로 중복 발송 방지.

장점: 테이블 1 개 감소. 단순한 sliding window.

단점: 사이클 부분 실패 재시도 불가 (cursor 되돌리면 성공한 도메인 재발송, advance 하면 실패 도메인 누락). 같은 시간창을 두 번 처리하는 시나리오 (수동 trigger + 스케줄러 동시) 에서 중복 발송. digest vs sync 의 분리도 모호.

선택하지 않은 이유: 중복 발송이 운영 알림 신뢰를 즉시 무너뜨리는 1차 리스크. dispatch 테이블 1 개의 비용이 합리적이다.

### A-6. 댓글 1 건 = Discord webhook 호출 1 회

신규 댓글마다 즉시 webhook 호출.

장점: 댓글 단위 발송 실패 격리 명료. 구현/추적/로깅 직관적.

단점: 댓글 K 건 → webhook K 회. Discord rate limit (per-URL 5 req/2s, 30 req/분) 에 닿기 쉬움. 같은 사이클 관련 댓글이 시간순으로 흩어져 가독성 저하. 멘션이 댓글마다 발생해 알림 피로 누적.

선택하지 않은 이유: rate limit 부담 + 채널 가독성 저하가 운영 신뢰를 떨어뜨리는 1차 요인. 도메인 단위 묶음으로 호출 수 급감 + embed `fields[]` 정리.

### A-7. 기존 `notification` 도메인에 흡수

신규 figma 도메인 미신설.

장점: 신규 패키지 / 마이그레이션 감소. 알림 발송 책임 한 곳.

단점: `notification` 이 "외부 시스템 댓글 동기화 상태" 같은 알림 발송 무관 책임을 갖게 되어 응집도 저하. Notion / Slack 등 외부 소스 추가 시 모든 폴링/상태 저장소를 떠안음.

선택하지 않은 이유: `notification` 은 발송 채널 책임으로, "외부 시스템에서 무엇을 가져오는가" 는 별도 도메인에 두는 것이 확장성 / 응집도 측면에서 유리.

### A-8. LLM 자연어 한 줄 요약을 발송 단계에 포함

도메인별 댓글 묶음을 LLM 으로 1~3 줄 요약문 압축해 embed 본문에 같이 띄움.

장점: 운영진이 본문 일일이 안 봐도 도메인 논의 한눈 파악. 알림 피로 추가 감소.

단점: 분류와 별개의 요약 LLM 호출이 도메인 × 사이클마다 발생. 분류 캐시는 commentId 단위라 요약에는 그대로 못 씀. 요약 캐시 키 정책 추가 결정. 요약 품질 검증 필요 (잘못된 요약은 분류 오류보다 영향 큼).

선택하지 않은 이유: 본 ADR 의 동기는 인프라 정합화. 자연어 요약은 캐시/비용/품질 가드를 함께 정해야 하는 별도 의사결정이라 후속 ADR 로 분리.

### A-9. 기존 `Manage*UseCase` 에 read 메서드 추가

`ManageFigmaRoutingDomainUseCase.listDomains()` 형태로 Command UseCase 에 read 메서드 흡수.

장점: 신규 인터페이스 / Service 클래스 감소. 컨트롤러 의존 빈 1 개 유지.

단점: write 트랜잭션 컨텍스트에 read 호출이 섞여 책임/트랜잭션 경계 모호. 프로젝트 컨벤션 (CQRS 분리) 위배. 단위 테스트 격리 저하.

선택하지 않은 이유: CQRS 컨벤션 우회 이득 < 신규 Query Service 도입 비용. 트랜잭션 / 책임 분리 이점이 더 크다.

### A-10. 별도 `*QueryController` 분리

`FigmaRoutingDomainQueryController` 등으로 GET 만 모음.

장점: 컨트롤러 단위 Command/Query 물리 분리. 보안 정책을 컨트롤러 단위로 부여 용이.

단점: 동일 URL prefix 컨트롤러 2 개 → OpenAPI 그룹 분리 (Swagger 가독성 저하). 권한 분리 운영 요구 부재 (admin 단일 그룹). 컨트롤러는 얇은 매핑 계층이라 책임 누출 위험 적음.

선택하지 않은 이유: 도메인 묶음 분리 비용 > 분리 이점. Service 레이어 분리만으로 충분.

### A-11. `discord_webhook_url` 평문 노출

마스킹 없이 등록값 그대로 응답.

장점: 등록 직후 운영자가 입력값 직접 검증 가능. 마스킹 로직 불필요.

단점: webhook URL 자체가 채널 발송 권한 토큰. admin 화면 캡처/공유로 즉시 권한 노출. 회수 시 발송 일시 중단.

선택하지 않은 이유: 운영 데이터 중 가장 강한 권한값이라 마스킹 비용이 작다. 등록 직후 검증은 추후 "test send" admin API 도입이 더 안전.

### A-12. 기존 `ResourceType` 재사용 (인가)

`ResourceType.NOTICE` / `AUDIT` 등의 권한 체계 차용.

장점: enum 추가 없음. evaluator 신규 불필요.

단점: 의미 불일치 (Figma 통합은 공지/감사 무관). 향후 권한 미세조정 시 다른 도메인 enum 변형 필요. 감사 로그에서 두 도메인이 같은 ResourceType 으로 기록되어 구분 어려움.

선택하지 않은 이유: ResourceType 은 도메인 구분 라벨. 별도 enum 분리가 응집도 / 확장성에 유리.

### A-13. `isAtLeastCentralCore()` 까지 인가 허용

중앙운영사무국 총괄 / 부총괄까지 Figma admin 접근 허용.

장점: 운영 동시성 향상 (SUPER_ADMIN 부재 시에도 대응). 다른 평가기 (`FcmPermissionEvaluator`) 와 권한 정책 일관.

단점: 민감 자산 (OAuth refresh token 등) 권한이 더 많은 사람에게 분산. 운영 사고 시 책임 추적 어려움. webhook URL 노출 시 회수 어려워 권한 보유자 수가 risk surface.

선택하지 않은 이유: Figma 통합 자산 위험도가 일반 알림 (FCM) 보다 높고, 현 운영 규모상 SUPER_ADMIN 으로 충분. 권한 확대는 운영 규모 증가 시 별도 ADR.

### A-14. OAuth callback 에도 `@CheckAccess` 강제

callback 에 `@Public` 제거 + 인증 / 인가 강제.

장점: 인가 정책 일관성. SecurityContext 추적 가능.

단점: Figma redirect 는 Authorization 헤더 없는 브라우저 navigation → 호출 100% 실패. redirect 를 임시 페이지 → 백엔드 호출로 다시 감싸는 별도 구조 필요.

선택하지 않은 이유: 구조 비용 큼. 보안 위험은 state 의 단기 수명 + owner memberId binding 으로 흡수.

## Consequences

### Positive

- Figma 댓글이 누락 없이 Discord 에서 담당 도메인 멘션 대상에게 도달 → 디자인 / 기획 피드백 회수 시간 감소.
- OAuth 기반이므로 운영진 교체 시 PAT 재발급 / 재배포 없이 동의만 다시 받으면 됨.
- 폴링 주기, 모니터링 대상 파일, 라우팅 도메인이 모두 DB / 설정으로 분리 → 코드 변경 없이 운영 조정 가능.
- LLM 분류 + fallback 도메인 구조로 페이지명 변경에 영향 받지 않음. 추후 Notion / Slack 등 다른 외부 댓글 시스템도 동일 LLM 도메인 재사용 가능.
- 단일 시간창 시맨틱으로 sync / digest / preview 본체 통합 → Mode 분기 제거. 본체가 시간창 입력 1 개 + 발송 정책 플래그 2 개로 표현되어 코드 단순화.
- preview / sync / digest 가 같은 묶음 결과 모양 공유 → 운영진이 preview "그대로 보내라" 하면 sync 가 동일 묶음을 발송한다는 보증.
- `figma_comment_dispatch` 가 발송 기록의 단일 진실 원본 → 알림 누락 / 중복 의심 시 SQL 한 줄로 확인 가능.
- 도메인 단위 묶음 embed → Discord webhook 호출 수 급감 + 채널 가독성 향상. rate limit 여유 확보.
- Query API 노출 → 운영진 DB 직접 접근 없이 admin 화면 / HTTP 호출만으로 라우팅 도메인 / watched file 상태 검증 가능. `last_synced_at` / `last_error` 노출로 동기화 실패 파일 즉시 식별.
- CQRS 분리 (Command / Query Service) 가 다른 도메인과 일관 → 구조 단순화.
- 모든 figma admin endpoint 가 SUPER_ADMIN 외 호출에 대해 `RESOURCE_ACCESS_DENIED` → Discord webhook URL / 멘션 / OAuth owner / Figma rate limit 무단 변경 위험 제거. `AccessControlAspect` 로그로 감사 추적 가능.

### Negative

- 폴링 간격 사이 댓글은 즉시 도달하지 않음 (최대 폴링 주기만큼 지연).
- Figma OAuth 동의자 1 인에게 토큰 관리 책임 집중. 해당 운영진의 Figma 권한 회수 시 시스템 전체 정지.
- 라우팅 도메인 매핑 / 멘션은 운영팀 명시 관리 필요. 새 도메인 미등록 시 fallback 채널로만 알림이 가서 담당자 누락 가능.
- LLM 분류 호출은 비용 / 지연 / 외부 의존성 추가. fallback 채널 정기 점검 필요.
- Figma API rate limit (분당 호출 수) 도달 방지 필요 (파일 수 × 폴링 주기 통제).
- 다중 인스턴스 환경에서 `figma_summary_cursor` advance 직렬화 필요 (ShedLock 또는 `SELECT ... FOR UPDATE`).
- `figma_comment_dispatch` 회수 잡 누락 시 무한 누적 (90 일 보존 기준).
- `figma_summary_cursor` 가 하나뿐이라 관리 실수로 cursor 미래 advance 시 그 사이 댓글 통째 누락 (ID boundary 는 "이후" 시맨틱이라 실수해도 더 적게 발송하는 쪽으로 기울었음).
- 신규 인터페이스 (`Get*UseCase`), Service 2 개, Response DTO 다수 추가 → 파일 수 / 코드량 증가.
- `discord_webhook_url` 마스킹으로 운영자가 입력값 사후 검증 어려움 → 별도 "발송 테스트" 흐름 필요.
- SUPER_ADMIN 1 인 부재 시 figma admin 운영 즉시 정지 → 권한 보유자 수가 1 이라는 가정이 운영 SPOF.
- 신규 `ResourceType.FIGMA` enum 추가 → 사용처 모두 함께 고려.
- 컨트롤러마다 `@CheckAccess` 어노테이션이 늘어나 메서드 시그니처 위 코드 라인이 길어짐.

### Neutral / Trade-offs

- refresh token DB 평문 저장 vs 암호화: application property 기반 대칭키 암호화로 기본 두되 운영 정책 결정 후 보강.
- `allowed_mentions.parse=["roles","users"]` 가 기본 정책. Discord webhook 의 `allowed_mentions` 정책 사용 시 별도 설정 필요.
- 폴링 주기 5 분 기본. 짧을수록 알림 지연 ↓ Figma API 호출량 ↑.
- "전역 cursor vs 파일별 cursor": 운영 단순성 ↔ 파일별 격리 trade-off. 본 ADR 은 전역 채택. 파일 수 증가 / 파일별 SLA 분화 시 파일별 cursor 로 재분리 가능.
- dispatch 테이블 보존 90 일: 알림 신뢰도 (중복 방지) ↔ 디스크 비용 trade-off. 운영 데이터 후 조정.
- digest API 기본 `force=true`. 명시적 `force=false` 옵션 (안 보낸 것만 보내는 제3 모드) 은 본 ADR 시점에 도입하지 않음.
- preview 시맨틱 변경: "마지막 sync 이후" → "지정 시간창 (기본: 최근 폴링 주기)". UI 라벨 / 툴팁 갱신 필요.
- `READ` / `MANAGE` 두 권한 분리하지만 evaluator 는 둘 다 SUPER_ADMIN. 추후 권한 분리 시점에 evaluator 만 수정.
- `FigmaOAuthController#callback` 만 `@Public` 비대칭 — state 검증으로 안전성 확보 + 의도된 설계.
- 마스킹 정책은 추후 운영자 권한 등급 분리 시 풀어줄 여지 (super-admin 평문 노출 등).
- 페이지네이션 미도입. 데이터 임계치 초과 시 신규 endpoint(`/v2`) 또는 응답 래핑 변경으로 마이그레이션 가능.
- mention 을 단건 응답에 포함하면서 별도 mentions endpoint 도 두는 구조는 일부 데이터 중복 노출. 부분 갱신 요구를 흡수하기 위한 의도적 중복.

## Implementation Notes

### 도메인 패키지 구조

```
com.umc.product.figma/
├── domain/
│   ├── FigmaIntegration.java               # OAuth refresh/access token 보관
│   ├── FigmaWatchedFile.java               # 폴링 대상 파일 (last_synced_at, last_error)
│   ├── FigmaRoutingDomain.java             # 라우팅 도메인 (domain_key, webhook URL, fallback)
│   ├── FigmaRoutingDomainMention.java      # 도메인별 멘션 대상 (role/user)
│   ├── FigmaCommentClassification.java     # 분류 L2 캐시
│   ├── FigmaSummaryCursor.java             # 단일 행 전역 cursor
│   ├── FigmaCommentDispatch.java           # 댓글 발송 기록 (commentId UNIQUE)
│   └── exception/
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── SummarizeFigmaCommentsUseCase.java     # 단일 진입점 (sync/digest/preview 통합)
│   │   │   ├── ManageFigmaRoutingDomainUseCase.java
│   │   │   ├── ManageFigmaWatchedFileUseCase.java
│   │   │   ├── GetFigmaRoutingDomainUseCase.java      # Query
│   │   │   ├── GetFigmaWatchedFileUseCase.java        # Query
│   │   │   └── RegisterFigmaIntegrationUseCase.java   # OAuth callback
│   │   └── out/
│   │       ├── LoadFigmaIntegrationPort.java / SaveFigmaIntegrationPort.java
│   │       ├── LoadFigmaWatchedFilePort.java / SaveFigmaWatchedFilePort.java
│   │       ├── LoadFigmaRoutingDomainPort.java / SaveFigmaRoutingDomainPort.java
│   │       ├── LoadFigmaSummaryCursorPort.java / SaveFigmaSummaryCursorPort.java
│   │       ├── LoadFigmaCommentDispatchPort.java / SaveFigmaCommentDispatchPort.java
│   │       ├── FetchFigmaCommentPort.java
│   │       └── SendDiscordMentionPort.java            # notification 모듈 발송 포트 재사용
│   └── service/
│       ├── FigmaCommentSummaryService.java            # 단일 본체 (Mode 분기 없음)
│       ├── FigmaCommentSyncCommandService.java        # SummarizeFigmaCommentsUseCase 위임 shim
│       ├── FigmaCommentDigestService.java             # 위임 shim
│       ├── FigmaCommentPreviewQueryService.java       # 위임 shim
│       ├── FigmaCommentDomainClassifier.java          # 3-tier 분류 캐시
│       ├── FigmaRoutingDomainCommandService.java
│       ├── FigmaRoutingDomainQueryService.java
│       ├── FigmaWatchedFileCommandService.java
│       ├── FigmaWatchedFileQueryService.java
│       ├── FigmaIntegrationCommandService.java
│       └── evaluator/FigmaPermissionEvaluator.java
└── adapter/
    ├── in/
    │   ├── web/
    │   │   ├── FigmaOAuthController.java              # start + callback
    │   │   ├── FigmaRoutingDomainController.java      # Command + Query
    │   │   ├── FigmaWatchedFileController.java        # Command + Query
    │   │   ├── FigmaSyncController.java               # 즉시 동기화 / preview
    │   │   └── FigmaDigestController.java             # catch-up 발송
    │   └── scheduler/
    │       ├── FigmaCommentSyncScheduler.java
    │       └── FigmaCommentDispatchRetentionScheduler.java
    └── out/
        ├── persistence/
        └── external/
            ├── FigmaOAuthClient.java
            ├── FigmaCommentClient.java
            ├── FigmaFileMetadataClient.java
            └── FigmaOAuthProperties.java

com.umc.product.llm/
├── application/
│   ├── port/
│   │   ├── in/ChatCompleteUseCase.java
│   │   └── out/ChatCompletionPort.java
│   └── service/ChatCompletionService.java
└── adapter/
    └── out/external/
        └── MockChatCompletionAdapter.java   # provider=mock (본 ADR 시점 기본)
```

### 신규 시그니처

```java
public interface SummarizeFigmaCommentsUseCase {
    FigmaSummaryResult summarize(SummarizeFigmaCommentsCommand command);
}

public record SummarizeFigmaCommentsCommand(
    Instant from,
    Instant to,
    boolean dryRun,         // true → Discord 발송 X, dispatch 기록 X, cursor advance X
    boolean force,          // true → dispatch 가 있어도 다시 발송
    boolean advanceCursor   // true → 발송 성공 시 figma_summary_cursor.last_window_end = to
) {
    public static SummarizeFigmaCommentsCommand scheduledSync(Instant from, Instant to) {
        return new SummarizeFigmaCommentsCommand(from, to, false, false, true);
    }
    public static SummarizeFigmaCommentsCommand digest(Instant from, Instant to) {
        return new SummarizeFigmaCommentsCommand(from, to, false, true, false);
    }
    public static SummarizeFigmaCommentsCommand preview(Instant from, Instant to) {
        return new SummarizeFigmaCommentsCommand(from, to, true, false, false);
    }
}

public interface GetFigmaRoutingDomainUseCase {
    FigmaRoutingDomainSummaryInfo getDomainById(Long domainId);
    List<FigmaRoutingDomainSummaryInfo> listDomains();
    List<FigmaRoutingDomainMentionInfo> listMentionsByDomainId(Long domainId);
}

public interface GetFigmaWatchedFileUseCase {
    FigmaWatchedFileInfo getById(Long watchedFileId);
    List<FigmaWatchedFileInfo> listAll(Boolean enabledFilter);   // null → 전체, true|false → 필터
}
```

### 환경 변수

```yaml
app:
  llm:
    provider: ${LLM_PROVIDER:mock}       # mock | openai | gemini | spring-ai

  figma:
    oauth:
      client-id: ...
      client-secret: ...
      redirect-uri: https://api.umc-product.com/admin/figma/oauth/callback
      scope: current_user:read,file_metadata:read,file_comments:read
      authorize-uri: https://www.figma.com/oauth
      token-uri: https://api.figma.com/v1/oauth/token
      refresh-uri: https://api.figma.com/v1/oauth/refresh
      token-encryption-key: ${FIGMA_TOKEN_ENCRYPTION_KEY}
    sync:
      enabled: ${FIGMA_SYNC_ENABLED:false}
      poll-interval: PT5M
      max-files-per-run: 50
    summary:
      dispatch-retention: P90D
      cursor-bootstrap-multiplier: 2     # 초기 cursor 부재 시 (now - pollInterval × N)
      retention-poll-interval: PT24H
```

> 도메인별 role ID 를 yaml 평문에 두던 안 (`app.figma.discord.role-mentions`) 은 폐기.
> Discord webhook URL / 멘션 ID 는 모두 DB (`figma_routing_domain`, `figma_routing_domain_mention`) 에 행 단위로 보관, admin API 로 등록 / 삭제.

### 데이터 모델 (Flyway 단일 파일 병합)

기존 figma 도메인 마이그레이션 4 종 (`V10.00`, `V10.10`, `V10.20`, `V21.30`) 을 develop / main 미머지 상태에서 단일 `V2026.05.07.10.00__create_figma_tables.sql` 로 병합한다. 각 테이블 블록 위에 책임 ADR / Decision 번호를 한 줄 주석으로 남긴다.

```sql
-- =====================================================================================
-- Figma 도메인 통합 마이그레이션 (ADR-015)
-- 기존 4개 마이그레이션 (V10.00 / V10.10 / V10.20 / V21.30) 이 develop/main 미머지
-- 상태였으므로 deprecate / drop 단계를 거치지 않고 단일 파일로 병합.
-- 이후 figma 도메인 스키마 변경은 신규 V2026.MM.DD 파일을 추가하는 정상 패턴.
-- =====================================================================================

-- ADR-015 §Decision 1: Figma OAuth refresh/access token 보관 (운영진 1인 위임)
CREATE TABLE figma_integration (
    id                       BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    owner_member_id          BIGINT                      NOT NULL,
    refresh_token_enc        TEXT                        NOT NULL,
    access_token_enc         TEXT,
    access_token_expires_at  TIMESTAMP(6) WITH TIME ZONE,
    scope                    VARCHAR(500)                NOT NULL,
    created_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uix_figma_integration_owner ON figma_integration (owner_member_id);


-- ADR-015 §Decision 3: 폴링 대상 파일.
--   last_synced_comment_id 폐기 — "어느 시각까지 봤는가" 는 figma_summary_cursor,
--   "어느 댓글이 발송됐는가" 는 figma_comment_dispatch 가 담당.
CREATE TABLE figma_watched_file (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    file_key        VARCHAR(100)                NOT NULL,
    display_name    VARCHAR(255)                NOT NULL,
    enabled         BOOLEAN                     NOT NULL DEFAULT TRUE,
    last_synced_at  TIMESTAMP(6) WITH TIME ZONE,           -- 마지막으로 이 파일을 fetch 한 시각
    last_error      TEXT,                                   -- 마지막 fetch 에서 발생한 오류 (성공 시 NULL)
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uix_figma_watched_file_key    ON figma_watched_file (file_key);
CREATE INDEX        ix_figma_watched_file_enabled ON figma_watched_file (enabled);


-- ADR-015 §Decision 2: LLM 분류 결과(domain_key) 라우팅. fallback 도메인 1개 필수.
--   페이지명 매핑(figma_part_route) 은 본 ADR 시점에 폐기 — 정의하지 않음.
CREATE TABLE figma_routing_domain (
    id                   BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    domain_key           VARCHAR(100)                NOT NULL,
    description          VARCHAR(500),
    discord_webhook_url  TEXT                        NOT NULL,
    fallback             BOOLEAN                     NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at           TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uix_figma_routing_domain_key     ON figma_routing_domain (domain_key);
CREATE INDEX        ix_figma_routing_domain_fallback ON figma_routing_domain (fallback);


-- ADR-015 §Decision 5: 도메인별 멘션 대상 (role/user) N개. embed 외부 content 영역에 출력.
CREATE TABLE figma_routing_domain_mention (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    domain_id       BIGINT                      NOT NULL,
    mention_id      VARCHAR(50)                 NOT NULL,
    mention_type    VARCHAR(20)                 NOT NULL,   -- ROLE | USER
    display_label   VARCHAR(255),
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_frdm_domain FOREIGN KEY (domain_id) REFERENCES figma_routing_domain (id) ON DELETE CASCADE
);
CREATE INDEX        ix_figma_routing_domain_mention_domain_id ON figma_routing_domain_mention (domain_id);
CREATE UNIQUE INDEX uix_figma_routing_domain_mention          ON figma_routing_domain_mention (domain_id, mention_type, mention_id);


-- ADR-015 §Decision 2 (3-tier 캐시 §L2): commentId → domain_key 영구 캐시.
--   재시작/다중 인스턴스 환경에서도 동일 commentId 의 LLM 재호출을 막는다.
--   mock provider 응답 / 후보 외 응답은 본 테이블에 저장하지 않고 in-memory L1 캐시에만 둔다.
CREATE TABLE figma_comment_classification (
    id            BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    comment_id    VARCHAR(255)                NOT NULL,
    domain_key    VARCHAR(100)                NOT NULL,
    provider      VARCHAR(64)                 NOT NULL,
    classified_at TIMESTAMP(6) WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX uix_figma_comment_classification_comment_id   ON figma_comment_classification (comment_id);
CREATE INDEX        ix_figma_comment_classification_classified_at ON figma_comment_classification (classified_at);


-- ADR-015 §Decision 3: 단일 행 전역 cursor. 스케줄러는 (last_window_end, now] 시간창을 사용하며,
--   발송 성공 시 last_window_end 를 now 로 advance 한다. 다중 인스턴스 환경에서는
--   SELECT FOR UPDATE 또는 ShedLock 으로 직렬화. application 코드가 단일 row 불변을 보장.
CREATE TABLE figma_summary_cursor (
    id                BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    last_window_end   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP(6) WITH TIME ZONE NOT NULL
);


-- ADR-015 §Decision 4: 댓글 단위 발송 기록. 시간창 시맨틱으로 옮기면서 사라진
--   "이미 발송된 댓글을 다시 보내지 않는다" 가드 책임을 본 테이블이 담당.
--   admin digest 는 force=true 로 본 테이블을 무시하고 재발송할 수 있다 (catch-up 시맨틱 유지).
--   90일 보존이며 별도 회수 잡으로 정리.
CREATE TABLE figma_comment_dispatch (
    id              BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    comment_id      VARCHAR(255)                NOT NULL,
    domain_id       BIGINT                      NOT NULL,
    dispatched_at   TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_fcd_domain FOREIGN KEY (domain_id) REFERENCES figma_routing_domain (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uix_figma_comment_dispatch_comment_id   ON figma_comment_dispatch (comment_id);
CREATE INDEX        ix_figma_comment_dispatch_dispatched_at ON figma_comment_dispatch (dispatched_at);
```

### 시간창 결정 흐름 (스케줄러)

```
1. 스케줄러 진입 → FigmaSummaryCursor 행 SELECT FOR UPDATE
2. now = Instant.now()
3. from = cursor.lastWindowEnd ?? (now - pollInterval × cursor-bootstrap-multiplier)
4. to   = now
5. SummarizeFigmaCommentsUseCase.summarize(scheduledSync(from, to))
6. 발송 성공한 도메인 묶음의 commentId 들에 대해 dispatch insert (REQUIRES_NEW)
7. cursor.lastWindowEnd = to (커밋)
```

### 분류 → 라우팅 → 묶음 발송 흐름

```
1. enabled 파일 목록 → 파일별 댓글 조회 (Figma REST)
2. 시간창 적용:
   - sync     : createdAt ∈ (lastWindowEnd, now]
   - digest   : createdAt ∈ [from, to]
   - preview  : 동일, dryRun=true
3. LLM candidates = SELECT domain_key FROM figma_routing_domain
4. 댓글 1건씩 ChatCompleteCommand.classify → domain_key (3-tier 캐시 통과)
5. 응답 ∈ candidates → 매칭 도메인, else → fallback 도메인
6. (file, comment, domain) 튜플을 도메인 단위로 group by
7. 도메인별:
   - filtered = comments \ existing(figma_comment_dispatch.comment_id)  when not force
   - mentions = SELECT FROM figma_routing_domain_mention WHERE domain_id = applied
   - embed payload 빌드 (fields 25/embed, embeds 10/message 자동 분할)
   - webhook POST → 성공 시 dispatch insert (REQUIRES_NEW)
8. (sync 만) cursor.lastWindowEnd = now
```

### Figma REST 호출

- 댓글 조회: `GET https://api.figma.com/v1/files/:file_key/comments` (`Authorization: Bearer {access_token}`)
- 댓글 위치: 응답의 `client_meta.node_id`. 페이지 이름은 `GET /v1/files/:file_key/nodes?ids=...` 의 ancestors 에서 type=CANVAS 노드명을 추출 → embed 부가 정보 (라우팅 키 아님, in-memory Caffeine 캐시).
- access token 갱신: `POST https://api.figma.com/v1/oauth/refresh` (refresh token 사용)
- 401 → refresh 1 회 시도 후 재요청. 429 → Retry-After 존중하고 다음 주기로 미룸 (즉시 재시도 금지).

### digest API 형식

```
POST /api/v1/admin/figma/digest?from={Instant}&to={Instant}
```

- 사이클 / sync 상태와 무관하게 `[from, to]` 안의 댓글을 도메인별로 묶어 Discord 로 발송.
- `figma_summary_cursor` 비변경 + `force=true` 가 기본 → 동일 시간창을 반복 호출하면 Discord 에 다시 발송된다 (catch-up / 회고용).
- 응답: `FigmaDigestSummary { from, to, totalComments, unmatchedCount, domains[] }`. `domains[].sent` 가 발송 성공 여부.

### 응답 DTO 와 마스킹

```java
public record FigmaRoutingDomainResponse(
    Long id,
    String domainKey,
    String description,
    String discordWebhookUrlMasked,    // 마지막 4자만 노출
    boolean fallback,
    int mentionCount,
    List<FigmaRoutingDomainMentionResponse> mentions   // list 응답에서는 null
) { ... }

public record FigmaRoutingDomainMentionResponse(
    Long id,
    String mentionId,
    DiscordMentionType mentionType,
    String displayLabel
) { ... }

public record FigmaWatchedFileResponse(
    Long id,
    String fileKey,
    String displayName,
    boolean enabled,
    Instant lastSyncedAt,
    String lastError
) { ... }
```

마스킹 유틸 (`FigmaWebhookUrlMasker` / `DiscordWebhookUrlMasker`):

```
입력: https://discord.com/api/webhooks/123456789012345678/abcDEF...xyz
출력: https://discord.com/api/webhooks/****5678/****..xyz
```

`*Info` record 는 `application/port/in/dto` 에 두고, `*Response.from(*Info)` 정적 팩토리에서 마스킹한다.

### 인가 어노테이션 적용표

| 컨트롤러                           | endpoint                                     | 어노테이션                         |
|--------------------------------|----------------------------------------------|-------------------------------|
| `FigmaRoutingDomainController` | `POST /`                                     | `@CheckAccess(FIGMA, MANAGE)` |
| `FigmaRoutingDomainController` | `DELETE /{domainId}`                         | `MANAGE`                      |
| `FigmaRoutingDomainController` | `POST /{domainId}/mentions`                  | `MANAGE`                      |
| `FigmaRoutingDomainController` | `DELETE /mentions/{mentionId}`               | `MANAGE`                      |
| `FigmaRoutingDomainController` | `GET /`                                      | `READ`                        |
| `FigmaRoutingDomainController` | `GET /{domainId}`                            | `READ`                        |
| `FigmaRoutingDomainController` | `GET /{domainId}/mentions`                   | `READ`                        |
| `FigmaWatchedFileController`   | `POST /`                                     | `MANAGE`                      |
| `FigmaWatchedFileController`   | `DELETE /{watchedFileId}`                    | `MANAGE`                      |
| `FigmaWatchedFileController`   | `POST /{watchedFileId}/enable`               | `MANAGE`                      |
| `FigmaWatchedFileController`   | `GET /`                                      | `READ`                        |
| `FigmaWatchedFileController`   | `GET /{watchedFileId}`                       | `READ`                        |
| `FigmaSyncController`          | `POST /`                                     | `MANAGE`                      |
| `FigmaSyncController`          | `POST /watched-files/{watchedFileId}`        | `MANAGE`                      |
| `FigmaSyncController`          | `GET /watched-files/{watchedFileId}/preview` | `READ`                        |
| `FigmaDigestController`        | `POST /`                                     | `MANAGE`                      |
| `FigmaOAuthController`         | `GET /` (start)                              | `MANAGE`                      |
| `FigmaOAuthController`         | `GET /callback`                              | (그대로 `@Public` 유지)            |

```java
// figma/application/service/evaluator/FigmaPermissionEvaluator.java
@Component
public class FigmaPermissionEvaluator implements ResourcePermissionEvaluator {

    @Override
    public ResourceType supportedResourceType() {
        return ResourceType.FIGMA;
    }

    @Override
    public boolean evaluate(SubjectAttributes subjectAttributes, ResourcePermission resourcePermission) {
        return switch (resourcePermission.permission()) {
            case READ, MANAGE -> isSuperAdmin(subjectAttributes);
            default -> false;
        };
    }

    private boolean isSuperAdmin(SubjectAttributes subjectAttributes) {
        return subjectAttributes.roleAttributes().stream()
            .anyMatch(role -> role.roleType().isSuperAdmin());
    }
}
```

### 운영 시 주의사항

- 최초 OAuth 동의는 운영진 1 인이 진행. 동의자 변경은 `RegisterFigmaIntegrationUseCase` 재호출로 덮어쓰기.
- 다중 인스턴스 환경에서 `figma_summary_cursor` advance 는 ShedLock (또는 `SELECT ... FOR UPDATE`) 으로 직렬화. 본 ADR 시점 ShedLock 미도입이면 `summarize-figma-comments` 잡 단독 락 추가.
- digest API 는 기본 `force=true` → 같은 시간창 반복 호출 시 중복 발송 (의도). preview 는 `dryRun=true` → cursor / dispatch 변경 없음.
- Figma REST rate limit (429) 초과 시 어댑터에서 Retry-After 존중 + 다음 주기로 미룸.
- LLM 분류가 후보 외 값을 반환하거나 호출 실패 시 fallback 도메인 자동 라우팅. 운영진은 fallback 채널 정기 점검.
- LLM 호출은 비용 / 지연 → 신규 댓글에만 호출, 3-tier 캐시 (L1 Caffeine 5 분 / L2 영구 / L3 LLM). preview API 도 LLM 호출 발생 → 사용 빈도가 비용 변수.
- `figma_comment_dispatch` 90 일 보존. 90 일 이상 지난 댓글이 다시 발송되지 않게 하려면 운영진이 명시적으로 force 옵션 사용.
- `FigmaOAuthController#callback` 에 절대 `@CheckAccess` 를 붙이면 안 된다 (Figma redirect 가 Authorization 헤더 없이 들어와 100% 실패). PR 리뷰 체크리스트 항목으로 추가.
- evaluator 추가 시 `AuthorizationService` 시작 로그 (`등록된 ResourcePermissionEvaluator: ...`) 에 `FIGMA` 가 함께 보여야 함. 부팅 후 로그로 등록 여부 검증.
- figma 도메인 마이그레이션 4 종은 develop / main 미머지 상태이므로 단일 `V2026.05.07.10.00__create_figma_tables.sql` 로 병합. deprecate-then-drop / 운영 안정화 기간 미적용.

## Implementation Plan (Commit 단위)

각 커밋은 독립 빌드 / 테스트 통과. Conventional Commits 규칙 (`<type>: <subject>`) 준수. PR 은 의미 단위로 묶어 `[Feat] Figma 댓글 Discord 포워딩 …` / `[Refactor] Figma 시간창 단일 유즈케이스 통합` 등의 제목으로 합친다.

> 본 ADR 은 ADR-003 / ADR-004 / ADR-005 / ADR-009 의 최종 산출물을 정합화한 것이며, 1차 라우팅 (페이지명 기반) → amendment 1·2차 (LLM 분류 + 도메인 라우팅 + 묶음 embed + digest) → 시간창 통합 → Query API → 인가 의 순으로 누적된 결과를 그대로 반영한다.
>
> 이후 figma 도메인 변경은 본 ADR 을 superseding 하거나 후속 ADR 로 다룬다 (예: 시계열 통계, LLM 자연어 요약, 멀티 테넌시).
>
> ordering 주의 — `ddl-auto: validate` 환경 제약상 마이그레이션 (Phase 1 §2) 이 도메인 엔티티 (Phase 1 §3) 보다 먼저 적용되어야 중간 커밋에서 부트가 깨지지 않는다.

### Phase 1 — figma OAuth + 기본 인프라 (5 commits)

1. `chore: figma 도메인 패키지와 환경 설정 스켈레톤 추가`
    - `com.umc.product.figma` 빈 패키지, `FigmaOAuthProperties`(record), `application*.yml` 의 `app.figma.*` / `app.llm.*` 키 추가, `RestClient` 빈 재사용 확인.
2. `chore: figma 마이그레이션 단일 파일 (V10.00) 작성 — 모든 figma 테이블 + summary cursor + comment dispatch 포함`
    - 본 ADR §Implementation Notes › 데이터 모델의 단일 SQL 파일 그대로. `figma_part_route` / `last_synced_comment_id` 미정의.
3. `feat: figma 통합 도메인 + persistence adapter 작성`
    - `FigmaIntegration`, `FigmaWatchedFile` 엔티티 / Builder / 도메인 메서드 (`rotateAccessToken`, `markFetched`, `disable`).
    - JPA Repository, Persistence Adapter, Save / Load Port.
4. `feat: figma OAuth authorization code 교환 / refresh 어댑터 구현`
    - `FigmaOAuthClient` (token endpoint), `RegisterFigmaIntegrationUseCase` + `FigmaIntegrationCommandService`, 자동 refresh 흐름.
    - 토큰 평문 저장 금지 — application key 기반 대칭 암호화 유틸 적용.
5. `feat: figma OAuth callback + watched file admin command API`
    - `FigmaOAuthController` (`GET /admin/figma/oauth/start`, `GET /admin/figma/oauth/callback`).
    - `FigmaWatchedFileController` Command (`POST /`, `POST /{id}/enable`, `DELETE /{id}`).

### Phase 2 — 분류 / 라우팅 / 발송 (5 commits)

6. `chore: LLM 도메인 패키지와 ChatCompletion 추상 추가`
    - `com.umc.product.llm` 패키지, `ChatCompleteUseCase` (in) / `ChatCompletionPort` (out) / `ChatCompletionService`.
7. `feat: LLM mock provider 어댑터 추가 (provider=mock 기본)`
    - `MockChatCompletionAdapter` + `@ConditionalOnProperty(app.llm.provider=mock)`.
8. `feat: figma 라우팅 도메인 / 멘션 데이터 모델과 admin command API`
    - `FigmaRoutingDomain`, `FigmaRoutingDomainMention` 도메인 + Persistence Adapter.
    - `ManageFigmaRoutingDomainUseCase` + `FigmaRoutingDomainCommandService` + `FigmaRoutingDomainController` Command.
9. `feat: figma 댓글 LLM 분류 + 3-tier 캐시 (L1 in-memory + L2 영구 + L3 LLM batch)`
    - `FigmaCommentDomainClassifier` + `figma_comment_classification` Adapter / Port.
10. `feat: 도메인 묶음 Discord embed 발송 어댑터 + 자동 분할`
    - `SendDiscordMentionPort` (notification 측) 호출 어댑터 / `allowed_mentions.parse=["roles","users"]`.
    - fields 25 / embeds 10 / 6000 자 초과 시 자동 분할 + 첫 페이지에만 멘션.

### Phase 3 — 시간창 단일 유즈케이스로 통합 (6 commits)

11. `feat: figma summary cursor / comment dispatch 도메인 + Port/Adapter`
    - `FigmaSummaryCursor`, `FigmaCommentDispatch` 엔티티 / Builder / 도메인 메서드 (`bootstrap`, `advance`, `of`).
    - cursor advance 는 `newEnd >= lastWindowEnd` 일 때만 적용 (방어).
12. `feat: SummarizeFigmaCommentsUseCase + 단일 본체 service 추가`
    - `SummarizeFigmaCommentsUseCase` (in) + `SummarizeFigmaCommentsCommand` record + `FigmaSummaryResult`.
    - `FigmaCommentSummaryService` 신규 본체. Mode 분기 없는 단일 시간창 필터.
13. `refactor: 기존 sync / digest / preview 를 SummarizeFigmaCommentsUseCase 위임으로 축소`
    - `FigmaCommentSyncCommandService` / `FigmaCommentDigestService` / `FigmaCommentPreviewQueryService` 를 thin shim 으로.
    - `FigmaCommentBatchProcessor` (Mode 분기 본체) 삭제.
14. `refactor: figma_watched_file 의 last_synced_comment_id 흔적 제거 (엔티티 / DTO / 컨트롤러 주석)`
    - 마이그레이션은 §2 에서 이미 정의 누락. 본 커밋은 코드 영역만.
    - `FigmaWatchedFile.markSynced(commentId, syncedAt)` → `markFetched(syncedAt)` 단순화.
    - DTO / 컨트롤러 / javadoc 의 `lastSyncedCommentId` 참조 모두 제거.
15. `feat: figma 스케줄러를 cursor 기반 시간창으로 전환`
    - `FigmaCommentSyncScheduler.poll()` 가 `figma_summary_cursor` 시간창으로 호출.
    - 운영자 수동 trigger (`syncAll()`) 도 동일 cursor 시맨틱.
16. `feat: figma_comment_dispatch 회수 스케줄러 추가`
    - `FigmaCommentDispatchRetentionScheduler` 일 1 회 보존 기간 초과 행 DELETE. ShedLock / 단독 락.

### Phase 4 — Query API + 운영 가시성 (4 commits)

17. `feat: figma 라우팅 도메인 query usecase / service 추가`
    - `GetFigmaRoutingDomainUseCase` + `FigmaRoutingDomainQueryService` (`@Transactional(readOnly=true)`).
18. `feat: figma 라우팅 도메인 query api 노출 + webhook URL 마스킹`
    - 컨트롤러에 `@GetMapping` 3 개 추가 + Response DTO + 마스킹 유틸.
19. `feat: figma watched file query usecase / service 추가`
    - `GetFigmaWatchedFileUseCase` + `FigmaWatchedFileQueryService` + `LoadFigmaWatchedFilePort.listAll(Boolean)` 추가.
20. `feat: figma watched file query api 노출 + sync 상태 필드`
    - 컨트롤러에 `@GetMapping` 2 개 + `last_synced_at` / `last_error` 노출.

### Phase 5 — admin 인가 (SUPER_ADMIN 전용) (2 commits)

21. `feat: figma admin API 인가용 ResourceType.FIGMA + FigmaPermissionEvaluator 추가`
    - `ResourceType.FIGMA(READ, MANAGE)` enum 추가.
    - `FigmaPermissionEvaluator` SUPER_ADMIN 단일 통과.
22. `feat: figma admin controllers 에 @CheckAccess 일괄 적용 (callback 제외)`
    - 5 개 컨트롤러의 모든 endpoint 에 `@CheckAccess(FIGMA, READ|MANAGE)`. `FigmaOAuthController#callback` 만 `@Public` 유지.

### Phase 6 — 테스트 / 문서 (2 commits)

23. `test: figma 시간창 / 분류 / 라우팅 / 인가 통합 시나리오 테스트`
    - `FigmaCommentSummaryServiceTest`: sync 첫 호출 발송 + dispatch + cursor advance / sync 재호출 dedup / digest force 무시 / preview dryRun 부수효과 0.
    - `FigmaSummaryCursorTest`: advance 방어 (미래 / 과거 / null / idempotent).
    - `FigmaPermissionEvaluatorTest`: SUPER_ADMIN 만 READ / MANAGE 통과, 그 외 false, 미지원 권한 false.
    - `FigmaRoutingDomainQueryServiceTest`, `FigmaWatchedFileQueryServiceTest`, MockMvc 컨트롤러 테스트.
24. `docs: ADR-015 status 를 Accepted 로 갱신 + 운영 가이드 작성`
    - 본 ADR Status 갱신.
    - `docs/guides/figma-comment-forwarder.md`: OAuth 동의 절차 / 파일 등록 / 라우팅 도메인 등록 / cursor 수동 조정 / dispatch 강제 삭제 / 90 일 보존 / 장애 대응 체크리스트.
    - 기존 ADR-003 / 004 / 005 / 009 의 Status 를 `Superseded by ADR-015` 로 전환.

## References

- 본 ADR 은 다음 4 개 ADR 을 병합 · 정합화해 단일 문서로 재작성한 것이다. 각 ADR 은 본 ADR 채택 시점에 `Superseded by ADR-015` 로 전환된다.
    - [ADR-003: Figma 댓글 Discord 포워딩](003-figma-comment-discord-forwarder.md) — OAuth, LLM 분류, 도메인 라우팅, embed 포맷, LLM 도메인 분리, digest API
    - [ADR-004: Figma 댓글 동기화 시간창 단일 유즈케이스 통합](004-figma-comment-time-window-unification.md) — 시간창 시맨틱, dispatch 가드, summary cursor
    - [ADR-005: Figma 라우팅 / Watched File Query API](005-figma-routing-and-watched-file-query-apis.md) — Query UseCase 분리, 응답 마스킹, 페이지네이션 미도입
    - [ADR-009: Figma admin API SUPER_ADMIN 전용](009-figma-admin-api-super-admin-only.md) — `ResourceType.FIGMA`, `READ` / `MANAGE` 분리, callback 비대칭
- 관련 ADR
    - [ADR-001: Apple 로그인 ClientType 라우팅](001-apple-signin-client-type-routing.md) — OAuth 흐름과 `*OAuthProperties` 패턴
    - [ADR-008: LLM 도메인 provider 전략](008-llm-domain-provider-strategy.md) — 본 ADR 의 LLM 도메인 분리 결정을 후속 정리
- 기존 코드
    - [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java)
    - [FigmaCommentDomainClassifier](../../src/main/java/com/umc/product/figma/application/service/FigmaCommentDomainClassifier.java)
    - [FigmaCommentSyncScheduler](../../src/main/java/com/umc/product/figma/adapter/in/scheduler/FigmaCommentSyncScheduler.java)
    - [FigmaWatchedFile](../../src/main/java/com/umc/product/figma/domain/FigmaWatchedFile.java)
    - [FigmaRoutingDomainController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaRoutingDomainController.java) / [FigmaWatchedFileController](../../src/main/java/com/umc/product/figma/adapter/in/web/FigmaWatchedFileController.java)
    - [CheckAccess](../../src/main/java/com/umc/product/authorization/adapter/in/aspect/CheckAccess.java) / [AccessControlAspect](../../src/main/java/com/umc/product/authorization/adapter/in/aspect/AccessControlAspect.java) / [ResourcePermissionEvaluator](../../src/main/java/com/umc/product/authorization/application/port/out/ResourcePermissionEvaluator.java) / [AuthorizationService](../../src/main/java/com/umc/product/authorization/application/service/AuthorizationService.java)
    - [TermPermissionEvaluator](../../src/main/java/com/umc/product/term/application/service/evaluator/TermPermissionEvaluator.java) — SUPER_ADMIN 단일 기준 evaluator 패턴
- Figma 공식 문서
    - [Figma OAuth2](https://www.figma.com/developers/api#oauth2)
    - [GET file comments](https://www.figma.com/developers/api#get-comments-endpoint)
    - [GET file](https://www.figma.com/developers/api#get-files-endpoint)
- Discord 공식 문서
    - [Execute Webhook](https://discord.com/developers/docs/resources/webhook#execute-webhook)
    - [Allowed Mentions](https://discord.com/developers/docs/resources/channel#allowed-mentions-object)
- 컨벤션
    - [CLAUDE.md §2 Architecture & Domain Rules](../../CLAUDE.md) — Hexagonal + CQRS + read 메서드 명명 규칙
- 운영 가이드
    - [Figma 댓글 Discord 포워딩 보고서](../guides/Figma_댓글_Discord_포워딩_보고서.md)
