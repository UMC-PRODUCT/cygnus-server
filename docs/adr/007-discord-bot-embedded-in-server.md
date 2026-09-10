# ADR-007: 동일 서버 프로세스 안에서 Discord bot 을 함께 띄운다 (JDA + Gateway-owner 단일 인스턴스 모델)

## Status

Proposed

## Context

2026-05-08 기준, UMC PRODUCT 서버는 Discord 와 다음 두 경로로 통신한다.

- [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java) — 운영 알람용 webhook 발송 (`SLACK / DISCORD / TELEGRAM` 공용 인터페이스).
- [DiscordMentionWebhookAdapter](../../src/main/java/com/umc/product/figma/adapter/out/external/DiscordMentionWebhookAdapter.java) — Figma 댓글 → Discord embed 묶음 발송.

두 어댑터 모두 **Discord webhook URL 단방향 POST** 다. 즉 서버는 Discord 로 메시지를 "보내는" 채널만 갖고 있고, Discord 측 이벤트(슬래시 커맨드, 멘션 응답, reaction, DM, 채널 가입 등) 를 "받는" 채널이 없다. 이 구조에서 운영 / 기획 측에 다음 한계가 누적되어 있다.

1. **운영진 명령(action) 채널 부재**: "지금 figma digest 를 임의 시간창으로 다시 돌려줘" 같은 운영 액션은 `POST /admin/figma/digest?from&to` 같은 admin REST 가 있어야만 가능하다. Discord 안에서 슬래시 커맨드로 같은 동작을 트리거할 수 있으면 운영 회의 도중 즉시 호출하는 흐름이 자연스럽다.
2. **양방향 Q&A 채널 부재**: 알림이 Discord 로 도달했을 때 운영진이 그 메시지에 답해도(혹은 reaction 을 달아도) 서버는 어떤 신호도 받지 못한다. "확인됨" 표시, 자동 ack, FAQ 응답 같은 시나리오가 모두 닫혀 있다.
3. **신규 도메인(문의, ADR-006)** 의 **운영진 측 입력 채널을 Discord 로 확장**할 가능성이 보인다. 문의 도메인이 도입되더라도 운영진은 자체 앱 화면보다 Discord 에서 즉시 응답하는 흐름을 더 선호할 가능성이 크다.
4. **서버 상태 조회 / 운영 도구**: 캐시 invalidation, FCM 토픽 토글, watched-file 강제 sync 등 Admin REST 로 노출하기엔 무거운 작업을 Discord 에서 슬래시 커맨드로 호출할 수 있다면 운영 비용이 줄어든다.

이 한계들을 풀려면 Discord 의 **Gateway WebSocket 연결**(즉 bot 계정) 이 필요하다. webhook 은 본질적으로 outbound 만 가능하고, inbound 이벤트(=상호작용) 는 Discord Gateway 또는 Interactions endpoint(HTTP) 로만 들어온다.

이번 결정에서 정해야 할 사항은 다음과 같다.

1. **Bot 호스팅 구조**: 본 서버 프로세스 안에서 함께 띄울지, 별도 프로세스/리포지토리/배포 단위로 분리할지.
2. **Discord 라이브러리 선택**: JDA / Discord4J / Javacord / Spring Discord starter / 직접 구현 / Interactions HTTP 전용.
3. **Gateway 연결 단일성 보장**: 수평 확장 시 Gateway 연결이 1개 인스턴스에서만 살아 있어야 한다(중복 연결 시 Discord 가 한쪽을 끊고, 동일 이벤트가 양쪽에서 처리될 위험). 어떻게 단일성을 보장할 것인가.
4. **명령 / 이벤트 처리 모델**: 슬래시 커맨드 라우팅, 권한 모델, 결과 응답 흐름 (즉시 응답 vs deferred reply).
5. **secret / 운영 설정**: bot token, application id, guild id, intents 의 환경변수화.
6. **로컬 개발 환경**: 로컬에서 bot 이 항상 떠야 하는가? guild 별 token 구분?
7. **Hexagonal Architecture 안에서의 위치**: 어느 도메인의 어떤 어댑터 계층에 둘 것인가. Discord 측 inbound 이벤트는 `adapter/in` 어디에 두는 것이 적절한가.

기술적 / 운영적 제약은 다음과 같다.

- Spring Boot 3.5, Java 21. Spring MVC + `RestClient` 기반 (WebFlux 미사용 → Reactor 기반 라이브러리는 친화도가 낮다).
- 프로젝트는 Hexagonal Architecture + CQRS 컨벤션 (`{Domain}CommandService` / `{Domain}QueryService`).
- 도메인은 ID 참조로만 다른 도메인과 통신하고, `adapter/in` 은 여러 종류 (REST 컨트롤러, 스케줄러, 이벤트 리스너) 가 이미 공존한다.
- 단일 인스턴스로 운영 중이지만 모집/이벤트 시즌에 다중 인스턴스로 확장하는 시나리오가 가까이 있다.
- ADR-006 으로 WebSocket + STOMP (서버 ↔ 클라이언트) 도입이 검토되고 있다 — Discord Gateway 연결과는 별개의 WebSocket 이지만 운영 측면에서 "한 프로세스 안에 두 개의 영구 연결" 이 공존하는 점은 인지가 필요하다.
- `application.yml` 에 이미 `app.webhook.discord.url` 이 있고, profile 은 `local / dev / prod`. 운영 알람은 `local` 외 모든 환경에서 발송되는 컨벤션이 자리잡혀 있다.

### 배경 기술 — WebFlux · Reactor · RestClient (이 결정의 기술적 전제)

본 ADR 의 결정점 ②(라이브러리 선택), `Decision §2`, `Alternatives §3` 에서 "Reactor 기반 라이브러리는 친화도가 낮다", "WebFlux 미사용", "Spring MVC + RestClient 기반이라 Reactor 가 외부에서만 들어오는 형태" 같은 표현이 반복된다. 이 판단의 근거가 되는 세 기술과 그 사이의 마찰을 명시적으로 정리한다.

#### Project Reactor — `Mono<T>` / `Flux<T>` 비동기 파이프라인

[Reactor 공식](https://projectreactor.io/) 의 reactive streams 구현체. 두 핵심 타입:

- `Mono<T>` — 0~1 개의 비동기 결과 (HTTP 응답 1건, DB 단건 조회 결과 등).
- `Flux<T>` — 0~N 개의 비동기 스트림 (Kafka 컨슘, 큰 결과셋 streaming 등).

일반 Java 메서드가 `T` 를 즉시 반환한다면, Reactor 메서드는 `Mono<T>` 를 반환해 "결과가 언제 어떤 스레드에서 도착할지 모르는 약속" 을 표현한다. `map` / `flatMap` / `filter` / `zip` 같은 연산자로 파이프라인을 구성하고, 마지막 `subscribe()` 시점에야 실제 실행이 트리거된다 (lazy / pull-style).

핵심 특성:

- **non-blocking** — I/O 대기 시점에 스레드를 점유하지 않고 이벤트 루프로 반환. 적은 스레드로 높은 동시성.
- **backpressure** — 소비자가 버틸 수 있는 만큼만 요청해 OOM 을 방지하는 흐름 제어.
- **viral type** — `Mono` / `Flux` 가 한 번 메서드 시그니처에 들어오면 호출 체인 위쪽으로 계속 전파된다. 중간에 `.block()` 으로 동기 변환이 가능하지만, **non-blocking 스레드(Netty event loop) 에서 호출하면 `BlockHangDetected` 예외 즉발**.

#### Spring WebFlux — Spring MVC 의 reactive 대체재

Spring 5 에서 도입된 reactive web 프레임워크. Spring MVC 와는 **둘 중 하나만 활성**되는 양자택일 관계다 (한 컨텍스트에 둘 다 띄울 수는 있으나 권장되지 않음).

|             | Spring MVC (현 본 프로젝트)                       | Spring WebFlux                                  |
|-------------|---------------------------------------------|-------------------------------------------------|
| 스레딩 모델      | thread-per-request (Tomcat, 기본 200 threads) | event loop (Netty, CPU 수 정도의 적은 스레드)            |
| 컨트롤러 반환 타입  | `T`, `ResponseEntity<T>`                    | `Mono<T>`, `Flux<T>`, `ResponseEntity<Mono<T>>` |
| HTTP client | `RestClient` / `RestTemplate`               | `WebClient`                                     |
| 트랜잭션 전파     | `@Transactional` (ThreadLocal 기반)           | `TransactionalOperator` (Reactor Context 기반)    |
| 로깅 MDC      | ThreadLocal 그대로 사용 가능                       | Reactor Context 또는 `MDCContextLifter` 로 별도 전파   |
| 진입 starter  | `spring-boot-starter-web`                   | `spring-boot-starter-webflux`                   |
| 검증된 패턴      | 본 프로젝트의 모든 도메인                              | (본 프로젝트 미경험)                                    |

WebFlux 의 강점은 I/O bound 워크로드에서 적은 스레드로 높은 동시성을 처리한다는 점. 단점은 디버깅 / 학습 곡선 / 트랜잭션 · MDC · `@Transactional` 같은 ThreadLocal 자산을 그대로 못 쓴다는 점이다.

#### RestClient — Spring 6.1 의 동기 HTTP client

`RestTemplate` 의 fluent API 후속. Spring 6.1 / Spring Boot 3.2 부터 표준 권장.

```java
String body = restClient.get()
    .uri("/api/...")
    .retrieve()
    .body(String.class);            // ← 호출 스레드 block, 응답이 올 때까지 대기
```

호출 결과를 즉시 `T` 로 받는다. 본 프로젝트는 [LlmProperties](../../src/main/java/com/umc/product/llm/adapter/out/external/LlmProperties.java) 의 LLM 호출, [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java) 의 알람 발송, [FigmaCommentClient](../../src/main/java/com/umc/product/figma/adapter/out/external/FigmaCommentClient.java) 의 Figma REST 호출까지 모든 외부 통신 어댑터가 `RestClient` 기반이고, 컨트롤러 → UseCase → 어댑터 호출 체인이 전부 동기다.

#### 왜 RestClient (동기) 와 Reactor 가 병행이 까다로운가

기술적으로 컴파일은 통과한다 — 한 프로젝트에 `RestClient` + Reactor `Mono` / `Flux` 가 공존해도 빌드는 깨지지 않는다. 문제는 다음 운영 / 일관성 / 트랜잭션 측면의 마찰이 영구적으로 누적된다는 점이다.

1. **타입 viral 전파** — Discord4J 의 거의 모든 API 가 `Mono` / `Flux` 를 반환한다. 슬래시 커맨드 핸들러에서 본 프로젝트의 동기 `XxxUseCase.run(...)` 을 호출하려면 매 경계마다 `Mono.fromCallable(() -> useCase.run(...))` 변환이 필요하다. 한두 군데면 괜찮지만, 슬래시 커맨드가 늘어날수록 boundary 변환이 누적되고 "이 메서드는 동기인가 비동기인가" 의 기억 부담이 커진다.

2. **스레드 풀 분리 + `.block()` 금지** — Discord4J 는 내부적으로 Netty event loop 를 띄운다. 이 스레드에서 `.block()` 을 호출하면 즉시 예외. 따라서 동기 UseCase 를 호출할 때 반드시 `Schedulers.boundedElastic()` 또는 별도 `Executor` 로 옮긴 뒤 호출해야 한다 — 코드가 한 단계 두꺼워지고, "어느 스레드에서 도는지" 가 핸들러마다 달라져 디버깅 / 스택 트레이스 해석 비용이 늘어난다.

3. **`@Transactional` / MDC 미전파** — Spring 의 `@Transactional` 은 `TransactionSynchronizationManager` 의 ThreadLocal 에 트랜잭션을 묶는다. Reactor 가 스레드를 자유롭게 옮기는 순간 이 ThreadLocal 이 끊긴다. MDC (로깅 컨텍스트) 도 같은 메커니즘이라 같이 사라진다. WebFlux 환경이라면 `TransactionalOperator` + Reactor Context 로 대응 가능하지만, 본 프로젝트는 동기 UseCase 가 `@Transactional` 을 전제로 한다. Discord4J 핸들러에서 동기 UseCase 호출 시 트랜잭션 / MDC 경계를 별도 설계로 보장해야 한다.

4. **HTTP client 이중화** — WebFlux 권장 HTTP client 는 reactive `WebClient`. 본 프로젝트는 `RestClient` 일색이다. Discord4J 가 끌고 들어오는 `reactor-netty-http` 가 본 프로젝트의 `RestClient` (JDK HttpClient 기반) 와 공존하며, 메트릭 / 인증 인터셉터 / retry 정책을 두 클라이언트에 각각 설정해야 한다. 같은 외부 시스템에 호출 인터셉터를 다르게 적용하는 일이 발생한다.

5. **Spring Boot 자동구성 모호성** — `spring-boot-starter-web` 과 `spring-boot-starter-webflux` 를 동시에 의존하면 Spring Boot 는 MVC 를 우선 활성화하지만, 일부 자동구성 (codec / WebClient builder 등) 이 양쪽 경로에서 동시에 등록된다. Discord4J 자체는 `starter-webflux` 를 강제하지 않지만, reactor-netty 를 transitive 로 끌어와 비슷한 충돌 영역을 만든다. 일부 메트릭 / 헬스체크가 두 번 등록되거나, 한쪽이 silently 무시되는 케이스가 생긴다.

6. **JDA 의 동기 콜백 모델 비교** — JDA 의 Gateway 콜백은 `void onSlashCommandInteraction(SlashCommandInteractionEvent event)` 형태의 동기 메서드이고, 응답은 `event.reply("...").queue()` 같이 fire-and-forget 패턴이다. 반환값이 동기라 본 프로젝트의 동기 UseCase 를 직접 호출할 수 있고, 3초 이상 걸리면 `event.deferReply()` 후 `applicationTaskExecutor` 에 `submit` 하는 친숙한 동기 패턴이 그대로 적용된다.

#### 정리

본 프로젝트가 WebFlux 기반이었다면 Discord4J + Reactor 가 가장 자연스러운 선택이었을 것이다. 하지만 Spring MVC + `RestClient` 일색인 현 코드베이스에서는 Reactor 를 도입하는 순간 (a) 핸들러마다 boundary 변환, (b) 트랜잭션 / MDC 별도 처리, (c) HTTP client 이중화 같은 영구적 비용이 누적된다. 그래서 `Decision §2` 에서 JDA 를 선택했고, `Alternatives §3` 에서 Discord4J 를 reject 한 이유 — "프로젝트 스타일과 일관성이 떨어진다" — 가 본 절에서 풀어쓴 그 일관성 비용이다.

## Decision

우리는 다음과 같이 결정한다.

1. **Bot 은 본 서버(Spring Boot) 프로세스 안에서 함께 띄운다.** 별도 리포지토리 / 별도 배포 단위로 분리하지 않는다. 근거는 다음과 같다.
    - 공유해야 할 도메인 객체 / UseCase 가 많다 (Figma digest, FCM 트리거, 문의 도메인, Member / ChallengerRole 권한 검증 등).
    - 별도 프로세스 분리 시 동일 도메인의 UseCase 를 두 곳에서 호출하기 위해 RPC 또는 메시지 큐가 추가로 필요해진다.
    - 운영팀(=현재) 의 배포 / 모니터링 부담이 한 서비스 단위로 유지된다.
2. **라이브러리는 JDA (Java Discord API) 를 사용한다.**
    - 본 프로젝트가 Spring MVC + 동기 `RestClient` 기반이라 Reactor 친화 라이브러리(Discord4J) 의 이점이 약하다.
    - JDA 가 Java 21 + Spring Boot 환경에서 가장 널리 검증되어 있고, 필요한 Gateway intents / interactions 모두 노출한다.
    - Reactive 가 필요하면 future 별 ADR 로 마이그레이션 가능 (어댑터 레이어만 교체).
3. **Gateway-owner 단일 인스턴스 모델로 시작한다.**
    - `app.discord.bot.enabled` 환경변수로 bot 활성화 여부를 인스턴스 단위로 토글한다 (`false` → JDA 빈 자체가 등록되지 않음 / Gateway 미연결).
    - 1차에는 prod 1대만 `true`. 다중 인스턴스 도입 시점에 leader-only 로 1개 인스턴스에서만 `true`. 다중 인스턴스 환경에서 자동 leader-election 도입은 후속 ADR.
    - 슬래시 커맨드 응답 (interactions) 은 Gateway 연결 인스턴스만 받는다. 이 인스턴스가 핸들러에서 다른 도메인 UseCase 를 호출 → 다른 인스턴스에 전파가 필요한 작업은 기존 도메인 트랜잭션 / 이벤트 메커니즘 (DB / FCM / Outbox) 에 위임한다 (Discord 로직은 인스턴스 간 공유 상태가 없도록 유지).
4. **로컬 / dev 에서는 기본 OFF, prod 에서 ON.**
    - `local`, `dev` 환경에서는 `app.discord.bot.enabled=false` 가 기본. 개발자가 본인의 dev guild 와 본인 token 으로 켜고 끌 수 있도록 `.env.local` 에서만 활성화 가능.
    - 단일 token 을 여러 개발자가 동시에 사용하면 Gateway 연결이 서로를 끊는다. **개인 dev token / 개인 dev guild** 사용을 가이드한다.
5. **신규 도메인 `discordbot` 을 신설한다 (`com.umc.product.discordbot`).**
    - 패키지 구조는 다른 도메인과 동일 (`domain` / `application/port/(in|out)` / `application/service` / `adapter/(in|out)`).
    - JDA `JDA` / `JDABuilder` 빈은 **adapter 계층** 에 둔다 (`adapter/out/gateway/JdaClient` 같은 구체 어댑터). 도메인 코드는 JDA 타입에 직접 의존하지 않는다.
    - Discord 측 inbound 이벤트(슬래시 커맨드, 메시지 등) 핸들러는 `adapter/in/gateway/` 에 둔다 (REST 컨트롤러와 같은 위계). 핸들러 안에서는 **타 도메인의 UseCase 만** 호출한다 (도메인 경계 유지).
6. **명령 / 이벤트 처리는 단계적으로 도입한다.**
    - 1차(본 ADR 범위) 는 **bot 띄우기 + 기본 lifecycle 알람 + 운영자용 슬래시 커맨드 1개(`/ping`) 로만 검증**. 실제 운영 명령(`/figma-digest` 등) 은 후속 PR / 후속 ADR.
    - 슬래시 커맨드 등록(`upsertCommand`) 은 **guild 단위** 로 한다 (전역 등록은 propagation 이 최대 1시간 걸려 운영 / 디버깅이 어려움). 운영 guild id 는 환경변수.
    - 응답이 3초 이상 걸리는 핸들러는 `event.deferReply()` 로 defer 후 비동기 응답 (Spring 의 `TaskExecutor` 재사용).
7. **운영 알람 발송 채널은 webhook 그대로 둔다.** bot 이 도입되더라도 `DiscordWebhookAdapter` / `DiscordMentionWebhookAdapter` (Figma 도메인) 의 발송 경로는 변경하지 않는다. 이유:
    - webhook 은 token 회수 / 권한 분리가 채널 단위로 이뤄지고, bot token 보다 운영 위험이 낮다.
    - bot 이 일시적으로 다운되어도 webhook 발송 경로는 영향이 없다 (운영 알람의 가용성이 bot 가용성과 분리된다).
    - bot 으로 발송 경로를 통일하는 것은 후속 의사결정 사항이며, 본 ADR 범위 밖.
8. **Privileged Intents 는 최소만 활성화한다.**
    - 1차 범위에는 슬래시 커맨드 / interactions 만 다루므로 `MESSAGE_CONTENT` 등 privileged intent 는 신청하지 않는다. 필요 시점에 별도 PR + Discord Developer Portal 신청 절차로 진행한다.
9. **권한 모델은 Discord guild role + 서버 측 member 매핑** 으로 한다.
    - 1차에는 가장 단순하게 — 슬래시 커맨드 사용 가능자 = "운영 guild 멤버" 로 한정 (guild 자체가 운영진 전용). 권한 세분화(중앙운영사무국 vs 학교 운영진 등) 는 본 ADR 범위 밖이며, ChallengerRole 매핑이 필요한 시점에 후속 ADR.
    - 향후 매핑은 Discord user id ↔ `Member.id` 매핑 테이블(`discord_user_member_link`) 로 풀 것을 권장한다 (본 ADR 에서는 스키마만 가볍게 언급).
10. **장애 / 재연결 정책.** Gateway 연결 끊김은 JDA 가 자동 재연결을 시도한다 (default `RECONNECT` 활성). 재연결 실패가 임계치(예: 30분) 를 넘으면 Sentry 알림을 발생시키고, prometheus 게이지(`discord_gateway_connected{instance}`) 로 노출한다. 운영자는 webhook 으로 들어오는 lifecycle 알람으로도 인지 가능 (bot 이 다운되어도 webhook 은 살아있음).

## Alternatives Considered

### 1. Bot 을 별도 프로세스 / 별도 리포지토리로 분리

`umc-product-discord-bot` 같은 별도 Spring Boot 또는 Node.js 프로젝트를 신설.

장점:

- 본 API 서버의 가용성과 bot 가용성이 완전히 분리된다.
- 라이브러리 / 언어 선택을 자유롭게 할 수 있다 (예: Discord.js, discord.py).
- 멀티 인스턴스 환경에서 bot 만 단일 인스턴스로 띄우는 인프라 설계가 단순해진다.

단점:

- 도메인 UseCase 를 호출하려면 RPC / HTTP / 메시지 큐가 필요하다 → "Discord 슬래시 커맨드 → 본 서버 admin REST" 호출 형태가 되며, 인증 / 권한 / 트랜잭션이 두 단계로 갈라진다.
- 배포 / 모니터링 / 로깅 / Sentry 연동을 한 벌 더 만들어야 한다.
- 도메인 모델이 두 리포지토리에 분산되면서 중복 / 표류 위험이 생긴다.

선택하지 않은 이유:
1차 운영 규모에서 분리의 이점(가용성 분리) 이 운영 / 개발 비용을 정당화하지 못한다. 본 서버 안에서 같이 띄워도 bot 다운이 API 가용성에 영향을 주지 않게 모듈 경계를 유지할 수 있다 (`enabled=false` 토글, lifecycle / scheduling 격리). 분리는 bot 동시 실행 인스턴스가 여러 대 필요해지거나, 다른 언어 / 다른 라이브러리로의 이동이 강제되는 시점에 별도 ADR 로 재논의한다.

### 2. Bot 대신 Discord Interactions Endpoint (HTTP) 만 사용

Discord 가 슬래시 커맨드 호출을 **외부 endpoint 에 HTTP POST** 로 보내는 모드. Gateway WebSocket 연결 없이 동작.

장점:

- WebSocket 영구 연결 / 재연결 / shard 관리 부담이 없다.
- 멀티 인스턴스 환경에서 어느 인스턴스로 들어와도 처리가 가능하다 (HTTP 라우팅이 자연스럽게 분산).
- 본 서버의 기존 Spring MVC 인프라(컨트롤러 / Security / 트랜잭션) 위에 그대로 얹힌다.

단점:

- Interactions endpoint 는 Discord 가 보낸 요청에 **3초 안에 응답** 해야 하며, 서명 검증(Ed25519 public key) 이 필수다.
- **메시지 / reaction / DM / member join 같은 Gateway 이벤트는 받을 수 없다** — interactions 만 받을 수 있다. 본 ADR 의 동기 범위(슬래시 커맨드만) 에는 들어맞지만, 향후 "Discord 메시지에 답하면 문의방 응답으로 매핑" 같은 시나리오는 불가.
- 외부에 endpoint 를 노출해야 하므로 SSL / WAF / signature 검증이 추가된다.

선택하지 않은 이유 (부분):
1차 요구만 보면 매력적이지만, 향후 양방향 Q&A / 메시지 hook 까지 확장될 가능성이 있어 Gateway 채널을 미리 확보하는 편이 안전하다. Interactions HTTP 도입은 향후 멀티 인스턴스 분산이 강제되는 시점에 보조 채널로 함께 도입할 수 있다 (둘 다 동시 사용 가능).

### 3. Discord4J (Reactor) 사용

`Mono` / `Flux` 기반의 비동기 라이브러리.

장점:

- Spring WebFlux 와 친화도가 가장 높다.
- 백프레셔 / streaming 에 강하다.

단점:

- 본 프로젝트는 Spring MVC + `RestClient` 기반이라 Reactor 가 외부에서만 들어오는 형태가 된다 — 컨트롤러 / 서비스가 모두 동기인데 이 도메인만 reactive 가 되는 비대칭이 생긴다.
- 핸들러 안에서 다른 UseCase (동기) 호출 시 boundary 변환 (`Mono.fromCallable` 등) 이 반복된다.
- Reactor 학습 / 디버깅 비용을 새로 쌓아야 한다.

선택하지 않은 이유:
프로젝트 스타일과 일관성이 떨어진다. Reactor 가 도메인 차원에서 필요해지는 시점에 통째로 마이그레이션하는 것이 더 일관적이다. (구체적인 boundary 변환 / 트랜잭션 / HTTP client 이중화 비용은 `Context §배경 기술` 절 1~5 항목 참조)

### 4. Javacord 사용

JDA 보다 가벼운 Discord 라이브러리.

장점:

- API 가 단순.
- 학습 곡선이 낮다.

단점:

- 사용자 / 커뮤니티 규모가 JDA 대비 작아 신규 Discord API 변경에 따라잡는 속도가 느릴 수 있다.
- Spring Boot 통합 사례 / 레퍼런스가 JDA 대비 적다.

선택하지 않은 이유:
기능 / 안정성 / 사용자 규모 모두 JDA 가 우위다. 관리 비용이 비슷하다면 더 안전한 쪽을 택한다.

### 5. Spring 내부에서 자체 WebSocket Gateway 클라이언트 직접 구현

라이브러리 없이 Discord Gateway 프로토콜을 우리가 직접 구현.

장점:

- 외부 의존성 0.

단점:

- Discord Gateway 는 heartbeat, sequence, reconnect, identify, sharding 등 별도 프로토콜을 가진다 — 이를 직접 구현하는 것은 본 ADR 의 가치 대비 압도적으로 많은 작업이다.
- 프로토콜 변경 시 따라잡기 비용을 우리가 진다.

선택하지 않은 이유:
재발명할 이유가 없다.

### 6. webhook 으로 발송 경로 통일하는 대신, 모든 outbound 도 bot 으로 통일

`DiscordWebhookAdapter` / `DiscordMentionWebhookAdapter` 의 발송 경로를 bot 의 `TextChannel#sendMessage` 로 갈아끼움.

장점:

- 발송 경로가 한 군데로 모인다.
- bot 의 신원이 메시지 발신자로 명확히 표시된다 (webhook 별 이름 / 아바타 관리 불필요).

단점:

- bot 가용성에 운영 알람 가용성이 묶인다 (bot 다운 시 알람도 끊김 — 가장 알람이 필요한 순간에 끊긴다는 모순).
- bot rate limit (메시지 / 채널 / 글로벌) 이 webhook 보다 엄격할 수 있다.
- 기존 도메인별 webhook URL / Discord embed 포맷 자산을 재구성해야 한다.

선택하지 않은 이유:
운영 알람의 가용성 분리가 더 중요하다. 본 ADR 은 "양방향 채널 도입" 만 다루고, 발송 경로 통합은 별도 의사결정으로 분리한다.

### 7. `app.discord.bot.enabled` 가 아닌 Spring Profile 로 분기

`@Profile("bot")` 같은 프로필 기반 활성화.

장점:

- Spring 표준 메커니즘과 친화도가 높다.
- 환경변수 / 설정 / 배포 단계에서 자연스럽다.

단점:

- 같은 프로필 안에서도 인스턴스별 on/off 를 다르게 두기 어렵다 (멀티 인스턴스 환경에서 leader-only 토글이 까다로워진다).
- `local` 에서 켜고 끄려면 임시로 활성 프로필을 바꿔야 해 부수효과가 따라온다.

선택하지 않은 이유:
멀티 인스턴스 환경에서 leader-only 활성화를 단순한 환경변수 한 개로 관리하는 편이 운영상 단순하다. 프로필은 발송 환경(local/dev/prod) 분기에 그대로 쓰고, bot 토글은 그와 직교하는 별도 축으로 둔다.

## Consequences

### Positive

- Discord 안에서 운영 액션을 직접 호출하는 흐름이 열린다 (운영 회의 / 인시던트 도중 슬래시 커맨드로 즉시 트리거).
- 본 서버 도메인 UseCase 와 Discord 사이의 중간 RPC / 메시지 큐 없이 같은 트랜잭션 / 같은 객체를 직접 호출할 수 있다.
- bot 은 webhook 발송 경로와 분리되어, bot 다운 시에도 운영 알람이 살아있다.
- Hexagonal 경계 안에 격리되므로, 향후 별도 프로세스 / 다른 라이브러리로 옮기는 마이그레이션이 어댑터 교체로 풀린다.

### Negative

- 한 프로세스 안에 영구 WebSocket 연결(Discord Gateway) 이 추가된다 — 모니터링 / 메트릭 / 재연결 정책을 새로 운영해야 한다 (ADR-006 의 STOMP WebSocket 도 도입되면 영구 연결이 두 종류).
- bot token 이라는 새 secret 이 운영 비밀 목록에 추가된다 (rotation 절차 / 회수 절차 정의 필요).
- 멀티 인스턴스 환경 도입 시 leader-only 토글을 수동으로 관리해야 한다 (자동 leader-election 미도입 — 후속 ADR).
- JDA 의 Java agent / reflection 기반 동작으로 startup 비용이 약간 증가한다 (수백 ms 단위).

### Neutral / Trade-offs

- bot 도입은 inbound 채널 1 종을 더하는 결정이지, "발송 경로 통합" 결정이 아니다. webhook 은 그대로 살아있으며 기능 중복 구간이 한동안 유지된다 — 이는 가용성 분리를 위한 의도적 trade-off.
- 1차에는 슬래시 커맨드를 1개(`/ping`) 만 노출한다. "왜 이렇게 작은 범위인가?" 라는 의문이 있을 수 있지만, Gateway 인프라 / lifecycle / 재연결 / 메트릭 등 검증할 항목이 충분해 본 ADR 의 도입 자체에 가치를 둔다.
- Privileged intents (MESSAGE_CONTENT) 미도입은 향후 "Discord 메시지에 답하면 자동 처리" 시나리오를 막는다 — 필요해지는 시점에 별도 ADR 로 신청 / 도입.

## Implementation Notes

### 사전 준비 (Discord 측)

1. **Discord Developer Portal 에서 Application 생성** — App 이름 `UMC PRODUCT Bot` (예시).
2. **Bot 토큰 발급** — `Bot` 탭에서 token 생성. 환경변수 `DISCORD_BOT_TOKEN` 으로 secret 등록.
3. **Application ID / Public Key 확인** — 슬래시 커맨드 등록 / Interactions endpoint 검증 시 사용. 환경변수 `DISCORD_APPLICATION_ID`, `DISCORD_PUBLIC_KEY` 로 등록 (1차에는 application id 만 사용, public key 는 향후 Interactions HTTP 도입 시 사용).
4. **운영 Guild 초대 URL 생성** — OAuth2 → URL Generator 에서 scope `bot applications.commands` + 권한 `Send Messages, Read Message History` (1차 최소). Guild 관리자가 초대 URL 로 bot 을 운영 guild 에 추가.
5. **운영 Guild ID 확인** — 슬래시 커맨드 guild 단위 등록 시 사용. 환경변수 `DISCORD_GUILD_ID`.
6. **Privileged Intents 비활성화 유지** — 1차 범위.

### 환경변수 / application.yml 추가

```yaml
# application.yml 끝부분에 추가
app:
  discord:
    bot:
      enabled: ${DISCORD_BOT_ENABLED:false}        # 인스턴스 단위 토글
      token: ${DISCORD_BOT_TOKEN:}
      application-id: ${DISCORD_APPLICATION_ID:}
      guild-id: ${DISCORD_GUILD_ID:}                # 슬래시 커맨드 등록 대상
      activity: ${DISCORD_BOT_ACTIVITY:UMC PRODUCT}  # 상태 메시지
```

`.env.local` / `.env.dev` / `.env.prod` 별 분기:

- `local` / `dev` 기본 `DISCORD_BOT_ENABLED=false`.
- `prod` 1대만 `DISCORD_BOT_ENABLED=true`.

### 의존성 (build.gradle.kts)

JDA 5.x 추가. 기존 dependency 블록 안에 다음 라인 추가:

```kotlin
// --- Discord Bot (JDA) ---
implementation("net.dv8tion:JDA:5.0.0-beta.24") {
    exclude(module = "opus-java")  // 음성 미사용
}
```

JDA 는 자체적으로 nv-websocket-client / jackson 을 끌고 들어온다. 충돌 시 BOM / `dependencyManagement` 에서 정렬.

### 패키지 구조

```
src/main/java/com/umc/product/discordbot/
├── domain/
│   └── exception/
│       ├── DiscordBotDomainException.java
│       └── DiscordBotErrorCode.java
├── application/
│   ├── port/
│   │   ├── in/
│   │   │   ├── HandleSlashCommandUseCase.java   // 1차에는 ping 핸들러 한 곳에 집중
│   │   │   └── dto/...
│   │   └── out/
│   │       └── (없음 — 1차 범위)
│   └── service/
│       └── DiscordSlashCommandService.java
└── adapter/
    ├── in/
    │   └── gateway/
    │       ├── DiscordEventListener.java          // JDA EventListener 구현
    │       └── DiscordSlashCommandRegistrar.java  // 부팅 시 슬래시 커맨드 upsert
    └── out/
        └── gateway/
            └── JdaClientFactory.java              // JDA 빈 등록
```

### JDA 빈 등록 / lifecycle

```java
// adapter/out/gateway/JdaClientFactory.java
@Configuration
@ConditionalOnProperty(name = "app.discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
public class JdaClientFactory {

    private final DiscordBotProperties properties;
    private final DiscordEventListener eventListener;

    @Bean(destroyMethod = "shutdownNow")
    public JDA jda() throws InterruptedException {
        return JDABuilder.createLight(
                properties.token(),
                EnumSet.noneOf(GatewayIntent.class)) // 1차에는 privileged intent 없음
            .setActivity(Activity.playing(properties.activity()))
            .addEventListeners(eventListener)
            .build()
            .awaitReady();
    }
}
```

`@ConditionalOnProperty` 로 `enabled=false` 인 인스턴스에서는 빈 자체가 등록되지 않아 Gateway 연결도 시도되지 않는다.

### 슬래시 커맨드 등록 (guild scope)

```java
// adapter/in/gateway/DiscordSlashCommandRegistrar.java
@Component
@ConditionalOnBean(JDA.class)
@RequiredArgsConstructor
@Slf4j
public class DiscordSlashCommandRegistrar {

    private final JDA jda;
    private final DiscordBotProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void registerCommands() {
        Guild guild = jda.getGuildById(properties.guildId());
        if (guild == null) {
            log.warn("Discord guild 미발견: id={}", properties.guildId());
            return;
        }
        guild.updateCommands()
            .addCommands(Commands.slash("ping", "서버 생존 확인"))
            .queue();
    }
}
```

guild 단위 등록은 즉시 반영. global 등록은 propagation 이 최대 1시간 — 운영 / 디버깅 비용 큼.

### Inbound 이벤트 처리

```java
// adapter/in/gateway/DiscordEventListener.java
@Component
@ConditionalOnProperty(name = "app.discord.bot.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DiscordEventListener extends ListenerAdapter {

    private final HandleSlashCommandUseCase handleSlashCommandUseCase;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();
        if ("ping".equals(name)) {
            event.reply("pong (instance " + System.getenv("HOSTNAME") + ")").queue();
            return;
        }
        // 1차 범위 — 그 외는 미구현.
    }
}
```

UseCase 호출이 3초 이상 걸리는 핸들러는 다음 패턴.

```java
event.deferReply().

queue();
applicationTaskExecutor.

submit(() ->{
String result = handleSlashCommandUseCase.run(...);
    event.

getHook().

sendMessage(result).

queue();
});
```

### 운영 / 모니터링

- **Prometheus 게이지**: `discord_gateway_connected{instance="..."}` 1/0. JDA 의 `EventListener` 에서 `ReadyEvent` / `DisconnectEvent` / `ResumedEvent` 를 받아 갱신.
- **Sentry**: `JDA` `getStatus()` 가 `RECONNECT_QUEUED` 등 비정상 상태로 일정 시간 이상 머물면 Sentry 캡처.
- **로깅**: `net.dv8tion.jda` 패키지 기본 INFO. 디버깅 시 DEBUG 로 전환 (`logback-spring.xml` 에 별도 logger 추가 가능).
- **Lifecycle 알람 재사용**: 기존 [ServerLifecycleAlarmListener](../../src/main/java/com/umc/product/notification/adapter/in/event/ServerLifecycleAlarmListener.java) 가 webhook 으로 "서버 시작 / 종료" 를 발송한다. bot 의 `ReadyEvent` 시점에 별도로 알람을 또 발송하지는 않는다 (중복 방지).

### secret / 보안

- `DISCORD_BOT_TOKEN` 은 prod 환경 secret manager 에 저장. 노출 시 즉시 토큰 회전 (Discord Developer Portal 에서 reset).
- bot 권한은 운영 guild 에서만 부여 — 외부 guild 추가 금지.
- bot 이 본 서버의 admin UseCase 를 직접 호출할 수 있는 만큼, 슬래시 커맨드별 권한 검증 (예: 운영 guild role 체크) 을 핸들러에서 명시적으로 수행한다.

### 멀티 인스턴스 / 수평 확장 운영 가이드

- 다중 인스턴스 도입 시점 (이르면 모집 시즌) 에는 다음 중 하나로 대응:
    1. 한 인스턴스만 `DISCORD_BOT_ENABLED=true` (수동 leader). 가장 단순.
    2. JDA Sharding (인스턴스가 N대일 때 N shard 로 분산). 단, 슬래시 커맨드 핸들러는 여전히 인스턴스별로 처리되어 결과를 broadcast 할 인프라가 추가로 필요.
- 자동 leader-election (DB row lock / Redis lock) 도입은 별도 ADR.

---

## Implementation Plan (Phase / Commit 단위)

각 커밋은 단독 빌드 / 테스트 통과 가능. Conventional Commits 준수 (`feat:`, `chore:`, `test:`, `docs:`, `refactor:`).

### Phase 0: 사전 준비 (의존성 / 설정 / 도메인 enum)

bot 코드 추가 전, 의존성 / 환경변수 / 공용 enum 을 정리한다. 이 단계만으로는 동작 변경 없음 (bot 비활성).

1. `chore: JDA 의존성 추가`
    - `build.gradle.kts` 에 `net.dv8tion:JDA:<버전>` (`opus-java` exclude) 추가.
    - 빌드 통과 확인.
2. `chore: discord bot 환경변수 / 설정 추가`
    - `application.yml` 에 `app.discord.bot.*` 블록 추가.
    - `.env.local` / `.env.dev` / `.env.prod` placeholder 추가 (token / app id / guild id / enabled).
    - `DiscordBotProperties` (`@ConfigurationProperties("app.discord.bot")`) 신규.
3. `feat: Domain enum 에 DISCORD_BOT 추가`
    - `global/exception/constant/Domain.DISCORD_BOT` 추가 (도메인 신설에 따른 예외 식별자).
    - `discordbot/domain/exception/DiscordBotDomainException` + `DiscordBotErrorCode` (Gateway 연결 실패 / 빈 미존재 / 권한 없음 등 1차 케이스만).

### Phase 1: bot lifecycle (Gateway 연결 / 종료)

본 ADR 의 핵심 — bot 을 실제로 띄우는 단계. 슬래시 커맨드는 아직 노출하지 않는다.

4. `feat: discordbot 도메인 패키지 + JdaClientFactory`
    - `com.umc.product.discordbot` 패키지 생성.
    - `adapter/out/gateway/JdaClientFactory` (`@Configuration` + `@ConditionalOnProperty(...,havingValue="true")`).
    - `JDA` 빈 등록 + `awaitReady()` + `destroyMethod = "shutdownNow"`.
    - `app.discord.bot.enabled=false` 일 때 컨텍스트 정상 부팅(빈 미등록) 검증 테스트.
5. `feat: DiscordEventListener 골격 + ReadyEvent 로깅`
    - `adapter/in/gateway/DiscordEventListener extends ListenerAdapter`.
    - `onReady` / `onDisconnect` / `onResumed` 에서 `Slf4j` 로 INFO 로그만.
    - `JdaClientFactory.addEventListeners(...)` 등록.
    - 통합 테스트 (옵션) — JDA 의 `JDAImpl` mock 또는 실 token 미사용 단위 테스트.

### Phase 2: 슬래시 커맨드 인프라 + `/ping`

6. `feat: 슬래시 커맨드 등록 / ping 핸들러`
    - `adapter/in/gateway/DiscordSlashCommandRegistrar` — `ApplicationReadyEvent` 에서 `guild.updateCommands().addCommands(...)`.
    - `DiscordEventListener.onSlashCommandInteraction` 에서 `ping` 분기 → `pong (instance ...)` 응답.
    - `application/port/in/HandleSlashCommandUseCase` + `DiscordSlashCommandService` (1차에는 ping 만, 후속 명령 추가에 대비한 인터페이스 형태).
    - 단위 테스트 (Mockito) — 이벤트 객체 mock 으로 핸들러 분기 검증.

### Phase 3: 운영 / 모니터링

7. `feat: Discord Gateway 연결 메트릭 노출`
    - `adapter/in/gateway/DiscordGatewayMetrics` — Micrometer `Gauge` `discord_gateway_connected`.
    - `ReadyEvent` / `DisconnectEvent` / `ResumedEvent` 시점에 게이지 갱신.
8. `feat: Discord Gateway 비정상 상태 Sentry 알림`
    - JDA `Status` polling (`@Scheduled(fixedDelay=...)`) — 비정상 상태 임계 시간 초과 시 Sentry capture.
    - 운영 알람 webhook 도 함께 호출 (`SendWebhookAlarmUseCase`).
    - 임계값은 `app.discord.bot.unhealthy-threshold-seconds` 환경변수.

### Phase 4: 문서 / 운영 가이드

9. `docs: ADR-007 Status 전환 / 운영 가이드 추가`
    - 본 ADR Status 를 `Accepted` 로 전환.
    - `docs/guides/discord-bot-운영.md` 신규 (선택) — bot 추가 / 권한 / 토큰 회전 절차.

### Phase 5 (후속, 별도 PR / 별도 ADR)

본 ADR 범위 밖. 별도 의사결정으로 분리.

- `/figma-digest from to` 슬래시 커맨드 (digest API를 Discord에서 직접 호출).
- 멀티 인스턴스 환경에서 자동 leader-election (DB lock / Redis lock).
- Discord user ↔ `Member` 매핑 테이블 (`discord_user_member_link`) 도입 + 권한 기반 슬래시 커맨드 분기.
- Privileged intents 신청 + 메시지 / reaction 기반 양방향 시나리오.
- Interactions HTTP endpoint 보조 채널 도입 (멀티 인스턴스 대응).
- 발송 경로 (webhook → bot) 통합 검토.

## References

- 관련 ADR
    - [ADR-006: 문의사항 도메인 + WebSocket + STOMP](006-inquiry-domain-with-websocket-stomp.md) — 한 프로세스 안에 두 종류의 영구 WebSocket 연결이 공존하는 운영 컨텍스트.
- 기존 코드
    - [DiscordWebhookAdapter](../../src/main/java/com/umc/product/notification/adapter/out/external/webhook/DiscordWebhookAdapter.java)
    - [DiscordMentionWebhookAdapter](../../src/main/java/com/umc/product/figma/adapter/out/external/DiscordMentionWebhookAdapter.java)
    - [WebhookPlatform](../../src/main/java/com/umc/product/notification/domain/WebhookPlatform.java)
    - [ServerLifecycleAlarmListener](../../src/main/java/com/umc/product/notification/adapter/in/event/ServerLifecycleAlarmListener.java)
- 외부 문서
    - JDA Wiki (5.x): <https://jda.wiki/>
    - Discord Developer Docs — Gateway: <https://discord.com/developers/docs/topics/gateway>
    - Discord Developer Docs — Slash Commands: <https://discord.com/developers/docs/interactions/application-commands>
    - Discord Developer Docs — Interactions Endpoint (대안 §2): <https://discord.com/developers/docs/interactions/receiving-and-responding>
