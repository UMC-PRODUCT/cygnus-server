# ADR-006: LLM 도메인 구현은 Spring AI + Gemini 1차 도입으로 진행한다

## Status

Proposed

## Context

2026-05-07 기준 Figma 댓글 → 서버 도메인 분류 라우팅이 도입되었고, 이를 위한 LLM 호출 추상은 별도 도메인 `com.umc.product.llm` 으로 분리되어 있다. 다만 본 ADR 작성 시점(2026-05-07)에는 `MockChatCompletionAdapter` 만 활성화된 상태로, candidates 중 하나를 무작위 반환할 뿐 실제 분류는 수행하지 않는다.

운영 도입을 위해 다음 결정이 필요하다.

- LLM 호출 통합 방식 (Spring AI / LangChain4j / 공식 SDK 직접 / RestClient 직접 구현 중 선택)
- 1차 LLM provider (OpenAI / Gemini / Anthropic / 로컬 호스팅 중 선택)
- 운영 안정성 보강 (재시도, 회로 차단, 메트릭, 캐시)
- API 키/시크릿 관리 정책
- 비용/성능 모니터링 정책

기술/운영 제약은 다음과 같다.

- 서비스 환경: Java 21, Spring Boot 3.5.15. 이미 `spring-boot-starter-web`, `spring-boot-starter-actuator`, Micrometer, OpenTelemetry, Sentry 가 운영 중이다.
- 호출 패턴: **분류 (classification)** 가 사실상 전부다. 입력 ~500 토큰, 출력 ~30 토큰. candidates 리스트가 함께 주어져 그 중 하나만 반환되면 된다.
- 호출량 추정: 활성 watched file 50개 × 신규 댓글 5건/일 평균 = sync 250건/일. preview / 운영진 트리거 포함 시 ~500건/일. 즉 일 100만 토큰을 한참 못 미치는 소규모.
- 한국어 분류 정확도가 운영진 신뢰의 핵심. 어색한 매핑은 fallback 채널 누적으로 이어진다.
- 본 시스템은 사용자 대면이 아닌 운영진 대면 도구이므로 분당 수십 호출 이상의 burst 는 발생하지 않는다 (스케줄러 5분 간격 + 운영진 수동 트리거).
- 본 도메인의 인터페이스(`ChatCompletionPort`, `ChatCompleteCommand`, `ChatCompletionResult`)는 이미 정의되어 있고, 어댑터 구현체만 갈아끼우면 되는 구조다.

이 ADR이 결정해야 하는 사항은 다음과 같다.

- LLM 통합 라이브러리 선택
- 1차 provider 와 모델
- 다중 provider 운영 전략 (교체/병행)
- 호출 신뢰성 보강 책임 (어디서, 어떻게)
- 캐시 정책 (있다면 어디 두는가)
- API 키 보관 및 노출 범위
- 관측 수단 (메트릭/로그 표준)

## Decision

우리는 다음과 같이 결정한다.

1. **통합 라이브러리: Spring AI 1.0 을 채택한다.**
    - `spring-ai-bom` 으로 버전 관리, `spring-ai-starter-model-openai` / `spring-ai-starter-model-vertex-ai-gemini` 등 starter 단위로 provider 추가.
    - 우리 `ChatCompletionPort` 구현체 내부에서 Spring AI `ChatClient` 를 호출한다. Spring AI 의 추상에 도메인 코드가 직접 의존하지 않도록 어댑터 경계는 그대로 유지한다.

2. **1차 provider: Google Gemini 1.5 Flash.**
    - 분류 같은 short-input / short-output task 에 비용·지연 효율이 가장 좋다.
    - 무료 티어 + 한국어 분류 정확도가 충분하다.
    - 인증은 Vertex AI service account 또는 Google AI Studio API key 둘 다 가능하도록 어댑터 구성을 분기한다 (운영은 Vertex AI 권장, 로컬/검증은 API key 권장).

3. **2차 provider 어댑터: OpenAI gpt-4o-mini 도 함께 도입한다.**
    - `app.llm.provider=openai` 로 즉시 교체 가능하도록 두 어댑터를 동시에 구현한다.
    - 단, 운영 활성화는 1개 provider 만. provider 별 어댑터는 `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "...")` 로 단일 활성화.

4. **호출 신뢰성: Spring AI 의 retry 추상 + 자체 회로 차단을 LLM 도메인 안에 둔다.**
    - 일시적 5xx / network error → backoff 최대 3회.
    - 연속 N회 실패 시 60초 동안 호출 skip → `ChatCompletionPort` 가 예외 또는 null-equivalent 응답을 반환하면, 호출자(`FigmaCommentDomainClassifier`) 가 fallback 도메인 라우팅으로 자연스럽게 흡수한다.
    - 회로 차단 상태는 in-memory 로 유지 (다중 인스턴스 환경에서는 인스턴스 단위 격리, 충분).

5. **분류 결과 캐시: Figma 도메인 측에서 `commentId` 키로 단기 캐시한다.**
    - LLM 도메인 자체는 캐시하지 않는다 (일반 prompt 는 같은 입력이라도 의미가 다를 수 있어 generic 캐시가 부적절).
    - Caffeine in-memory, TTL 5분 정도. 같은 댓글이 sync 와 preview 양쪽에서 짧은 시간 내 중복 호출되는 것을 방지한다.

6. **API 키 보관: application property + 운영 시크릿 주입.**
    - `LLM_OPENAI_API_KEY`, `LLM_GEMINI_API_KEY` (또는 Vertex AI service account credential) 를 환경 변수로 주입한다.
    - DB 영속화하지 않는다. KMS / Vault 도입은 figma token 암호화와 동일 시점(별도 ADR)에 함께 다룬다.

7. **관측: Micrometer 메트릭 + 구조화 로그.**
    - 메트릭: `llm.chat.completion.seconds` (latency), `llm.chat.completion.total{provider, status}` (호출 카운트), `llm.chat.completion.tokens.total{provider, type=in|out}` (토큰 사용).
    - 로그: provider, model, candidates 크기, latency, fallback 여부를 INFO 또는 DEBUG 로 남긴다. 댓글 본문은 PII 가능성을 감안해 본문 전체가 아니라 길이/해시만 기록.

8. **mock 어댑터는 유지한다.**
    - 테스트 / 로컬 개발 / provider 장애 대응 fallback 용으로 `MockChatCompletionAdapter` 는 그대로 둔다.
    - 통합 테스트는 항상 `app.llm.provider=mock` 으로 강제한다.

## Alternatives Considered

### 1. LangChain4j

Java 진영에서 Spring AI 와 양강 구도를 형성한 LLM 통합 라이브러리.

장점:

- Spring AI 보다 LLM 응용 (agent, tool, RAG, memory) 추상이 풍부하다.
- provider coverage 가 더 넓고 1.0 GA 도 더 일찍 나왔다.
- Spring 의존성 없이도 사용 가능해 모듈성 측면에서 유연하다.

단점:

- 본 시스템의 사용 사례는 단순 분류 한 종류이며, agent / tool / RAG 가 필요 없다.
- Spring Boot auto-configuration / Actuator / Micrometer 와의 매끄러운 통합은 Spring AI 가 우위.
- Spring AI 가 본 프로젝트의 다른 Spring 추상 (auto-config, observability, properties) 과 일관된다.

선택하지 않은 이유:
당장 필요한 기능 범위가 작아서 LangChain4j 의 풍부한 추상은 오버킬이며, 운영/관측 통합이 자연스러운 Spring AI 가 본 프로젝트에 더 잘 맞는다. 추후 LLM 활용 범위가 RAG / agent 까지 확장되면 그 시점에 재검토한다.

### 2. 공식 OpenAI / Google GenAI Java SDK 를 직접 사용

각 provider 의 공식 Java SDK 를 직접 의존한다.

장점:

- 추상이 한 겹 줄어들어 응답 구조 / 신규 기능 변화에 빠르게 따라갈 수 있다.
- 라이브러리 풋프린트가 작다.

단점:

- provider 별 어댑터를 `provider × API` 만큼 수동으로 추상화해야 한다.
- 재시도, 관측, 토큰 카운팅 같은 운영 코드를 우리가 모두 작성해야 한다.
- provider 교체 비용이 커진다 (라이브러리 자체가 다르므로 코드 변경 폭이 크다).

선택하지 않은 이유:
운영 코드를 직접 짜야 하는 비용이 Spring AI 의 한 겹 추상 비용보다 크다. 이 시스템은 LLM 호출이 핵심 가치가 아니라 부수 기능이므로, provider-neutral 추상에 위임하는 편이 유지비가 낮다.

### 3. RestClient 로 직접 구현

이미 의존하고 있는 Spring `RestClient` 로 OpenAI / Gemini REST API 를 직접 호출한다.

장점:

- 의존성 추가가 0 이다.
- 코드 흐름이 가장 단순하고 우리 통제 안에 있다.
- 1차 도입에 한해서는 가장 빠른 PoC 수단이다.

단점:

- provider API 변경(특히 Gemini 의 자주 바뀌는 응답 스키마) 을 우리가 매번 따라가야 한다.
- streaming, function calling, structured output 같은 기능 도입 비용이 높다.
- 토큰 카운트, retry, 백오프, 관측을 모두 자체 구현해야 한다.

선택하지 않은 이유:
PoC 수준에서는 매력적이지만 운영 환경에서의 유지비가 빠르게 누적된다. Spring AI 가 다 해 주는 일을 우리가 다시 하지 않는 편이 낫다.

### 4. 1차 provider 로 OpenAI gpt-4o-mini 를 우선 도입

OpenAI 를 1차 활성화하고 Gemini 는 후속에서 추가한다.

장점:

- 한국어 분류 정확도 / 응답 안정성에서 사실상 업계 표준에 가까운 성능을 보인다.
- Spring AI 1차 시민으로 가장 잘 지원되며 문서/예제가 풍부하다.

단점:

- Gemini 1.5 Flash 대비 같은 task 비용이 약 2~4배.
- 무료 티어가 작아 검증 단계에서도 결제 정보가 필요하다.
- 본 시스템의 task 가 분류 한 종류라 OpenAI 의 모델 풍부함을 활용할 여지가 적다.

선택하지 않은 이유:
분류 task 의 작은 입출력 규모에서 Gemini 1.5 Flash 의 비용/속도 우위가 압도적이고, 한국어 분류 정확도도 충분하다. OpenAI 는 2차 어댑터로 도입해 비교/장애 대비용으로 활용하는 편이 합리적이다.

### 5. Anthropic Claude (Sonnet/Haiku) 1차 도입

Claude 모델로 분류를 수행한다.

장점:

- 한국어 자연어 이해 품질이 우수하다.
- 안정적인 응답 형식과 system prompt 준수도가 높다.

단점:

- 비용이 OpenAI 와 비슷하거나 약간 높다.
- 한국 region 에서의 지연이 OpenAI / Gemini 보다 큰 경향.
- 본 시스템의 task 단순함을 고려하면 비용 대비 이득이 작다.

선택하지 않은 이유:
정확도 우위가 비용/지연 단점을 상쇄할 만한 task 가 아니다. 추후 RAG / 에이전트성 활용 단계에서는 재검토 가능.

### 6. 로컬/자체 호스팅 (Ollama, vLLM 등)

소형 오픈 모델을 자체 인프라에서 서빙한다.

장점:

- 호출 비용 0.
- 데이터가 외부로 나가지 않는다 (운영진 댓글 본문 처리).

단점:

- 인프라 운영 부담 (GPU 서버 또는 대용량 CPU 인스턴스 필요).
- 한국어 분류 정확도가 상용 모델 대비 떨어지는 모델이 다수.
- 로딩/replication/모니터링 책임이 추가된다.

선택하지 않은 이유:
하루 ~500 호출 규모에서 인프라 운영 부담이 호출 비용 절감을 압도한다. 향후 호출량이 크게 증가하거나 댓글 본문이 민감 데이터로 분류되면 재검토한다.

### 7. 캐시 위치를 LLM 도메인 안에 둔다 (prompt-hash 키 기반 generic 캐시)

`ChatCompletionService` 가 prompt 해시를 키로 응답을 캐시한다.

장점:

- LLM 도메인을 다른 사용처가 추가되어도 한 곳에서 캐시 효과를 본다.
- 캐시 책임이 한 컴포넌트에 모인다.

단점:

- LLM 호출은 일반적으로 같은 prompt 라도 의도가 다를 수 있다 (랜덤성 활용, A/B prompt 비교 등). generic 캐시가 의미를 흐릴 수 있다.
- 분류 task 외의 사용처에서는 캐시가 잘못된 동작을 유발할 수 있다.

선택하지 않은 이유:
캐시는 사용처의 의미에 묶이는 결정이다. 분류 결과가 commentId 단위로 idempotent 인 것은 figma 도메인의 도메인 지식이지 LLM 도메인의 일반 속성이 아니다. 캐시는 figma 측 (`FigmaCommentDomainClassifier`) 안에 두는 편이 책임 경계가 명확하다.

## Consequences

### Positive

- Provider 교체가 application property 한 줄로 끝나므로, 분류 정확도/비용/장애 대응에 따라 운영 중 빠르게 갈아끼울 수 있다.
- Spring AI 의 auto-config / Micrometer 통합 덕분에 메트릭과 로그를 표준 방식으로 수집할 수 있다.
- Mock 어댑터를 그대로 유지해 통합 테스트가 결정적(deterministic) 으로 동작한다.
- Gemini 1.5 Flash 의 비용 구조 덕분에 일/월 비용이 ~$1 수준으로 운영 부담이 거의 없다.
- 기존 `ChatCompletionPort` 추상이 그대로 유지되므로, figma 도메인 코드 변경 없이 LLM 어댑터만 추가된다.

### Negative

- Spring AI 가 1.0 GA 가 비교적 최근이라 향후 마이너 버전에서 ChatClient API 가 미세하게 바뀔 가능성이 있다 (운영 모니터링 필요).
- Vertex AI 인증을 사용할 경우 service account JSON 관리/배포 책임이 추가된다.
- LLM 호출 실패 / 분류 오류는 fallback 도메인으로 흡수되므로, 운영진이 fallback 채널을 정기 점검하지 않으면 분류 정확도 악화를 인지하지 못한다.
- 회로 차단 상태가 in-memory 라 다중 인스턴스 운영 시 인스턴스마다 별도로 차단/복구된다 (분산 락은 도입 보류).

### Neutral / Trade-offs

- Spring AI 가 아닌 LangChain4j 로 갈 경우 RAG / agent 까지 자연스럽게 확장되지만, 본 시점의 task 규모를 감안하면 그 확장은 가설이다.
- 분류 결과 캐시를 figma 측에 두는 결정은 LLM 도메인의 단순성을 보존하지만, 향후 다른 도메인이 비슷한 분류를 도입하면 캐시 로직이 중복될 수 있다 (그 시점에 추출 검토).
- 1차 Gemini 선택은 비용 우위가 크지만, OpenAI 가 한국어 분류에 더 안정적인 응답 형식을 보이는 경향이 있다는 보고가 있다. preview 와 fallback 채널 모니터링으로 정확도 차이를 측정한 뒤 필요 시 OpenAI 로 1차 교체한다.

## Implementation Notes

### 의존성 추가

`gradle/libs.versions.toml`:

```toml
spring-ai = "1.1.8"
```

`gradle/dependencies.gradle.kts`:

```kotlin
dependencies {
    add("implementation", platform("org.springframework.ai:spring-ai-bom:${version("spring-ai")}"))
    add("implementation", "org.springframework.ai:spring-ai-starter-model-vertex-ai-gemini")
    add("implementation", "org.springframework.ai:spring-ai-starter-model-openai")
    add("implementation", "org.springframework.ai:spring-ai-starter-model-google-genai")

    // 단기 캐시 (figma classifier 측에서 사용)
    add("implementation", "com.github.ben-manes.caffeine:caffeine")
    add("implementation", "org.springframework.boot:spring-boot-starter-cache")
}
```

### `LlmProperties` 확장

```java
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
    String provider,            // mock | openai | gemini
    String model,               // 예: gpt-4o-mini, gemini-1.5-flash
    Double temperature,         // 분류면 0.0
    Integer maxOutputTokens,    // 분류면 32 정도
    Retry retry,
    CircuitBreaker circuitBreaker
) {
    public record Retry(int maxAttempts, long backoffMillis) {}
    public record CircuitBreaker(int failureThreshold, long openDurationMillis) {}
}
```

### `application.yml`

```yaml
app:
  llm:
    provider: ${LLM_PROVIDER:mock}
    model: ${LLM_MODEL:gemini-1.5-flash}
    temperature: 0.0
    max-output-tokens: 32
    retry:
      max-attempts: 3
      backoff-millis: 200
    circuit-breaker:
      failure-threshold: 5
      open-duration-millis: 60000

spring:
  ai:
    openai:
      api-key: ${LLM_OPENAI_API_KEY:}
    vertex:
      ai:
        gemini:
          project-id: ${VERTEX_PROJECT_ID:}
          location: ${VERTEX_LOCATION:asia-northeast3}
          # credentials 는 GOOGLE_APPLICATION_CREDENTIALS 환경 변수로 주입
```

### 어댑터 구조

```
com.umc.product.llm.adapter.out.external/
├── MockChatCompletionAdapter             (provider=mock, 기존)
├── SpringAiOpenAiChatCompletionAdapter   (provider=openai)
└── SpringAiGeminiChatCompletionAdapter   (provider=gemini)
```

각 어댑터는 `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "...")` 로 단일 활성화. 내부에서 Spring AI `ChatClient` 빌더를 사용해 호출하고, candidates 가 있으면 system prompt 에 "반드시 다음 중 하나만 반환" 제약을 보강한다.

### 회로 차단 / 재시도

Spring AI 의 `RetryTemplate` 통합을 활성화하고, 추가로 LLM 도메인 안에 `LlmCallGuard` 컴포넌트를 두어 연속 실패 카운터 + skip-until 시각을 보관한다. `ChatCompletionService` 가 어댑터 호출 전에 `LlmCallGuard.allow()` 를 확인하고, 차단 상태면 즉시 fallback 응답 (예: `LlmDomainException(CHAT_COMPLETION_FAILED)`) 을 반환한다.

### 메트릭

Spring AI 가 자동 등록하는 `spring.ai.chat.client` 관찰 외에, 도메인 측에서 다음을 직접 등록한다.

- `llm_chat_completion_seconds` (Timer)
- `llm_chat_completion_total{provider, status}`
- `llm_chat_completion_tokens_total{provider, type}`

태그의 cardinality 폭증을 막기 위해 `provider`, `status`, `type` 외의 라벨은 추가하지 않는다.

### figma 측 캐시

```java
@Component
public class FigmaCommentDomainClassifier {
    private final Cache<String, Optional<String>> cache =
        Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public String classify(FigmaCommentInfo c, List<String> candidates) {
        Optional<String> cached = cache.getIfPresent(c.commentId());
        if (cached != null) return cached.orElse(null);

        String picked = doClassify(c, candidates);
        cache.put(c.commentId(), Optional.ofNullable(picked));
        return picked;
    }
}
```

cardinality 가 위험 수준으로 커지지 않도록 maximumSize 한도를 둔다.

### 운영 시 주의사항

- API key / Vertex credential 은 절대 DB / 로그에 평문 저장하지 않는다.
- LLM 응답이 후보 외 값일 때 우리는 fallback 으로 보내지만, **연속해서 후보 외 응답이 누적되면 model/prompt 회귀 신호** 다. `llm_chat_completion_total{status="out-of-candidates"}` 카운터를 별도로 두어 운영 대시보드에서 추적한다.
- preview API 도 LLM 호출을 발생시키므로 운영진의 preview 사용 빈도가 비용 변수가 된다. 캐시(5분 TTL) 가 sync→preview 같은 댓글 호출 폭증을 흡수하지만, 다른 댓글 다수에 대한 preview 는 그대로 호출된다.
- 운영 활성화 직후 1~2주는 fallback 채널 + `out-of-candidates` 카운터를 매일 점검해 분류 품질을 검증한다.

## Implementation Plan (Commit 단위)

각 커밋은 단독으로 빌드/테스트가 통과해야 하며, Conventional Commits 규칙을 따른다. PR 은 의미 단위로 묶어 `[Feat] LLM provider 도입 …` 등의 제목을 사용한다.

1. `chore: spring-ai BOM 의존성과 LlmProperties 확장 추가`
    - `build.gradle.kts` 에 `spring-ai-bom` + OpenAI / Vertex Gemini starter 추가, Caffeine + spring-boot-starter-cache 추가.
    - `LlmProperties` 에 `model`, `temperature`, `maxOutputTokens`, `retry`, `circuitBreaker` 필드 추가.
    - `application.yml` 에 `app.llm.*` 키와 `spring.ai.*` 키 신설 (실제 활성 provider 는 여전히 mock 유지).
    - 외부 호출 없음, 빌드만 통과.

2. `feat: Spring AI ChatClient 기반 OpenAI 어댑터 추가`
    - `SpringAiOpenAiChatCompletionAdapter` (`@ConditionalOnProperty(provider=openai)`) 신설.
    - `ChatClient.Builder` 로 호출, candidates 가 있으면 system prompt 에 강한 제약 추가, temperature 0 / maxOutputTokens 32 적용.
    - 응답이 candidates 에 없으면 그대로 반환 (호출자가 fallback 처리).
    - mock 은 그대로 유지 — 활성화는 운영 단계에서 property 변경.

3. `feat: Spring AI ChatClient 기반 Gemini 어댑터 추가`
    - `SpringAiGeminiChatCompletionAdapter` (`@ConditionalOnProperty(provider=gemini)`) 신설.
    - Vertex AI Gemini 인증 (service account) 또는 Google AI Studio API key 로 분기 가능하도록 설정.
    - 응답 파싱은 OpenAI 어댑터와 동일 로직 재사용 (helper 추출).

4. `feat: LLM 호출 retry 와 회로 차단 가드 도입`
    - `LlmCallGuard` 컴포넌트: 연속 실패 카운터 + skip-until 시각 (in-memory).
    - `ChatCompletionService` 가 어댑터 호출 전후로 가드 확인 / 결과 기록.
    - 일시 실패 retry 는 Spring AI 의 `RetryTemplate` 활용. 영구 4xx 는 즉시 실패.
    - 가드가 차단 상태면 `LlmDomainException(CHAT_COMPLETION_FAILED)` 반환 → figma 측 fallback 라우팅으로 흡수.

5. `feat: LLM 호출 메트릭 등록`
    - `llm_chat_completion_seconds`, `llm_chat_completion_total{provider, status}`, `llm_chat_completion_tokens_total{provider, type}` Micrometer 등록.
    - `out-of-candidates` 상태도 별도 status 라벨로 측정.

6. `feat: figma 분류 결과 단기 캐시 적용`
    - `FigmaCommentDomainClassifier` 안에 Caffeine 캐시 (commentId 키, 5분 TTL, max 10k).
    - sync → preview 같은 댓글 중복 호출 흡수.

7. `chore: 운영 프로필에 provider=gemini + 시크릿 환경 변수 추가`
    - `LLM_PROVIDER`, `LLM_MODEL`, `VERTEX_PROJECT_ID`, `VERTEX_LOCATION`, `LLM_OPENAI_API_KEY` 등 GitHub Actions / 운영 시크릿에 추가 (코드 외부 작업).
    - 코드 내 application.yml 에는 placeholder 만 두어 자동 활성화는 운영 시점에 결정.

8. `test: Spring AI 어댑터 단위 테스트 + figma classifier 캐시 테스트 추가`
    - `MockRestServiceServer` 또는 Spring AI 의 `ChatModel` 모킹 유틸로 어댑터 응답 파싱 / candidates 외 값 처리 검증.
    - `FigmaCommentDomainClassifier` 캐시 hit/miss 시나리오.
    - 통합 테스트는 `app.llm.provider=mock` 강제로 LLM 외부 호출 없이 figma 흐름 전체 검증.

## References

- 코드
    - [ChatCompletionPort](../../src/main/java/com/umc/product/llm/application/port/out/ChatCompletionPort.java) — 본 ADR 의 결정이 구현되는 추상
    - [MockChatCompletionAdapter](../../src/main/java/com/umc/product/llm/adapter/out/external/MockChatCompletionAdapter.java) — 1차 도입 후에도 유지될 fallback/test 어댑터
    - [FigmaCommentDomainClassifier](../../src/main/java/com/umc/product/figma/application/service/FigmaCommentDomainClassifier.java) — LLM 호출 책임 캡슐화 + 캐시 도입 지점
- 외부 문서
    - [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html)
    - [Spring AI OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html)
    - [Spring AI Vertex AI Gemini Chat](https://docs.spring.io/spring-ai/reference/api/chat/vertexai-gemini-chat.html)
    - [Gemini 1.5 Flash Pricing](https://ai.google.dev/pricing)
    - [OpenAI Pricing](https://openai.com/api/pricing/)
