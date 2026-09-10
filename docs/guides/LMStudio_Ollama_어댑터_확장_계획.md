# LMStudio / Ollama 자가 호스팅 어댑터 확장 계획

> 작성일: 2026-05-08
> 대상: `com.umc.product.llm` 도메인의 `ChatCompletionPort` 어댑터 확장.
> 목적: 현재 mock / openai / vertexai-gemini / google-genai 4종 어댑터 구조에서 LMStudio · Ollama 같은 self-hosted 추론 서버를 추가할 때의 변경 폭과 5건 commit 단위 실행계획을 정리.

## 0. TL;DR

LMStudio 와 Ollama **둘 다 OpenAI-compatible REST API (`POST /v1/chat/completions`) 를 표준 제공** 한다. 따라서:

- **신규 starter 의존성 추가 불필요** — 기존 `spring-ai-starter-model-openai` 로 충분.
- **신규 어댑터 1 개** (`SpringAiOpenAiCompatibleChatCompletionAdapter`) 만 추가하면 LMStudio / Ollama 둘 다 같은 코드 경로로 동작.
- 기존 `SpringAiOpenAiChatCompletionAdapter` (실 OpenAI 용) 는 손대지 않는다 — `@ConditionalOnProperty` 가 단일 활성화를 보장하므로 충돌 없음.
- provider 라벨을 yaml 에서 받도록 해 메트릭 (`llm.active.provider.info{provider=...}`) 에서 LMStudio / Ollama 가 구분된다.

총 5 건 commit 으로 구현 + 테스트 + 운영 가이드 마무리. 동작 변경 (외부 OpenAI → self-hosted) 은 환경변수 `LLM_PROVIDER=openai-compatible` 로 토글된다.

## 1. 현재 어댑터 구조

```
ChatCompletionPort (out port)
  ├─ MockChatCompletionAdapter             provider=mock
  ├─ SpringAiOpenAiChatCompletionAdapter   provider=openai            (실 OpenAI)
  ├─ SpringAiGeminiChatCompletionAdapter   provider=vertexai-gemini   (Vertex AI)
  └─ SpringAiGoogleGenAiChatCompletionAdapter provider=google-genai   (Google AI Studio)
```

활성화 메커니즘:

```java
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "<value>")
```

`LlmFallbackConfig` 가 어떤 어댑터도 등록되지 않은 경우 `MockChatCompletionAdapter` 로 안전 fallback 한다 (WARN 로그 + `llm.active.provider.info{fallback="true"}` gauge).

dependency: `spring-ai-starter-model-openai` / `spring-ai-starter-model-vertex-ai-gemini` / `spring-ai-starter-model-google-genai`. **Ollama starter 는 없다.**

## 2. LMStudio / Ollama 의 인터페이스 비교

| 항목 | LMStudio | Ollama |
|------|----------|--------|
| OpenAI-compat 엔드포인트 | ✅ `POST /v1/chat/completions` | ✅ `POST /v1/chat/completions` (v0.1.32+) |
| Native 엔드포인트 | (없음) | `POST /api/chat`, `POST /api/generate` |
| 기본 포트 | `1234` | `11434` |
| API key | 불필요 (서버가 무시) | 불필요 (서버가 무시) |
| 모델 관리 | LMStudio GUI / CLI | `ollama pull <model>` |
| Streaming | OpenAI SSE 호환 | OpenAI SSE 호환 |
| Token usage | `usage` 필드 채움 | v0.1.32+ 채움 (이전 버전은 누락) |
| 한국어 모델 가용성 | gguf 변환 모델 다양 | `ollama pull mistral / llama3 / qwen2.5` 등 |

핵심 사실: **두 서버 모두 OpenAI-compat 만으로 분류 호출 (`/v1/chat/completions`) 을 100% 만족한다.** Ollama 의 native API 가 모델 관리 등 추가 기능을 주지만, 일반 배치 분류 use case 에는 OpenAI-compat 으로 충분.

## 3. 어댑터 변경 필요성 평가

본 절의 결론을 먼저: **신규 어댑터 1 개를 추가한다 (옵션 B).** 다음 4 옵션을 비교.

### 옵션 A — 기존 OpenAI 어댑터의 base-url 만 외부화

```yaml
spring:
  ai:
    openai:
      base-url: ${OPENAI_BASE_URL:https://api.openai.com}
      api-key:  ${OPENAI_API_KEY:}
```

LMStudio 면 `OPENAI_BASE_URL=http://localhost:1234` + dummy api-key.

장점:
- 어댑터 추가 0. 변경 폭 최소.

단점:
- `ChatCompletionResult.provider` 가 항상 `"openai"` 로 찍힘. 메트릭 (`llm_chat_completion_total{provider="openai"}`) 에서 실 OpenAI 와 self-hosted 가 같은 라벨로 합산됨.
- `LlmCallGuard` / `LlmRateLimiter` 가 동일 instance 라 RPM 한도가 다른 두 path 를 한 가드로 통제. 실 OpenAI 와 self-hosted 의 latency / cost 모델이 완전히 달라 같이 통제하기 어색.
- `LLM_PROVIDER` 가 `openai` 한 값으로 두 시나리오를 표현 → 운영자가 "지금 어디 붙어 있는지" 환경변수만으로 즉시 판별 불가.

선택 안 함: 메트릭 / 라벨 분리 비용을 회수하지 못함.

### 옵션 B — OpenAI-compat 전용 신규 어댑터 1 개 (LMStudio / Ollama 공용) ✅

신규 `SpringAiOpenAiCompatibleChatCompletionAdapter` 추가. `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai-compatible")`. provider 라벨은 `app.llm.openai-compatible.label` (default `local-lmstudio`) 에서 받음 — 운영자가 LMStudio 면 `local-lmstudio`, Ollama 면 `local-ollama` 로 설정.

장점:
- provider 라벨이 yaml 에서 결정되므로 메트릭에서 외부 OpenAI 와 self-hosted 가 즉시 구분.
- 기존 OpenAI 어댑터는 변경 없음 → 기존 운영 환경 회귀 위험 0.
- LMStudio ↔ Ollama 교체는 환경변수 (`LLM_OPENAI_COMPAT_BASE_URL`, `LLM_OPENAI_COMPAT_LABEL`) 만 바꾸면 됨.

단점:
- 어댑터 코드 한 벌 추가 (사실상 OpenAI 어댑터의 사본 + 다른 OpenAiApi 인스턴스).
- Spring AI 의 OpenAI auto-config (`spring.ai.openai.*`) 와 분리해 자체 `OpenAiApi` / `OpenAiChatModel` 인스턴스를 빌드해야 함 (실 OpenAI 어댑터와 동시 활성화될 일은 없지만, base-url 충돌을 원천 차단하기 위함).

선택 이유:
호출 경로가 동일 (`/v1/chat/completions`) 이라 LMStudio / Ollama 를 한 어댑터로 묶는 게 자연스럽고, provider 라벨 분리로 메트릭 / 운영 가시성은 모두 챙긴다. 옵션 C 의 어댑터 2 개 분리 비용을 들일 만한 차이는 두 서버 사이에 없다.

### 옵션 C — LMStudio / Ollama 각자 어댑터

`SpringAiLmStudioChatCompletionAdapter` + `SpringAiOllamaChatCompletionAdapter` 각각 분리.

장점:
- provider 라벨이 코드 상수로 결정 (yaml 에서 변경 불가) → 휴먼 에러 (잘못된 라벨) 차단.

단점:
- 동일 OpenAI-compat 코드 두 벌. 어느 한쪽 변경 시 다른 쪽 sync 부담.
- 향후 또 다른 self-hosted 서버 (vllm, TGI 등) 가 추가될 때마다 어댑터 클래스가 늘어남.

선택 안 함: 두 서버의 호출 path 가 동일하므로 분리 가치가 낮다. 라벨 휴먼 에러는 properties 검증에서 허용 라벨 enum 으로 잡으면 충분.

### 옵션 D — Spring AI Ollama starter 도입 (Ollama 만 native API)

`spring-ai-starter-model-ollama` 의존성 추가. Ollama 는 native `/api/chat` 사용. LMStudio 는 별도 OpenAI-compat path.

장점:
- Ollama native API 의 추가 기능 (model 관리, structured output 일부) 활용 가능.
- Ollama 0.x 의 token usage 누락 같은 OpenAI-compat 한계를 native 로 우회.

단점:
- LMStudio 와 Ollama 의 path 가 다름 → 코드 분기 증가, 메트릭 라벨 / 디버깅도 두 갈래.
- starter 의존성 추가는 build size + 빌드 시간 증가.
- 현재 배치 분류 use case 는 native API 의 추가 기능을 거의 안 씀 → 도입 가치 낮음.

선택 안 함: 현재 use case 에는 OpenAI-compat 으로 충분. 향후 Ollama native 가 정말 필요해지면 별도 어댑터로 분리 (본 옵션 D 를 그때 다시 채택).

## 4. 설계 — `SpringAiOpenAiCompatibleChatCompletionAdapter`

### 4.1 핵심 코드 구조

```java
@Slf4j
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai-compatible")
public class SpringAiOpenAiCompatibleChatCompletionAdapter implements ChatCompletionPort {

    private final OpenAiChatModel chatModel;
    private final LlmProperties properties;
    private final String providerLabel;

    public SpringAiOpenAiCompatibleChatCompletionAdapter(LlmProperties properties) {
        this.properties = properties;
        LlmProperties.OpenaiCompatible cfg = properties.openaiCompatible();
        // 기존 spring.ai.openai.* auto-config 와 독립된 자체 OpenAiApi / ChatModel 인스턴스 빌드.
        // base-url 만 다른 동일 프로토콜이므로 Spring AI 의 OpenAI 클라이언트가 그대로 동작.
        OpenAiApi api = OpenAiApi.builder()
            .baseUrl(cfg.baseUrl())
            .apiKey(cfg.apiKey())
            .build();
        this.chatModel = OpenAiChatModel.builder()
            .openAiApi(api)
            .build();
        this.providerLabel = cfg.label();
    }

    @Override
    public ChatCompletionResult complete(ChatCompleteCommand command) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
            .model(properties.model())
            .temperature(properties.temperature())
            .maxTokens(ChatPromptHelper.resolveMaxOutputTokens(command, properties))
            .build();
        String systemPrompt = command.systemPrompt() == null ? "" : command.systemPrompt();
        String userPrompt   = command.userPrompt()   == null ? "" : command.userPrompt();
        try {
            ChatResponse response = ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .chatResponse();
            String content = response == null || response.getResult() == null
                || response.getResult().getOutput() == null
                ? ""
                : response.getResult().getOutput().getText();
            String normalized = ChatPromptHelper.normalizeResponse(content);
            Long promptTokens = ChatPromptHelper.extractPromptTokens(response);
            Long completionTokens = ChatPromptHelper.extractCompletionTokens(response);
            log.debug("self-hosted 호출 성공: provider={}, model={}, length={}, promptTokens={}, completionTokens={}",
                providerLabel, properties.model(), normalized.length(), promptTokens, completionTokens);
            return ChatCompletionResult.of(normalized, providerLabel, promptTokens, completionTokens);
        } catch (Exception e) {
            log.warn("self-hosted 호출 실패: provider={}, model={}, error={}",
                providerLabel, properties.model(), e.toString());
            throw new LlmDomainException(LlmErrorCode.CHAT_COMPLETION_FAILED, e.getMessage());
        }
    }

    @Override
    public String providerName() {
        return providerLabel;
    }
}
```

### 4.2 properties 확장

```java
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
    String provider,
    String model,
    Double temperature,
    Integer maxOutputTokens,
    Retry retry,
    CircuitBreaker circuitBreaker,
    RateLimit rateLimit,
    OpenaiCompatible openaiCompatible   // 신규
) {

    public record OpenaiCompatible(
        String baseUrl,
        String apiKey,
        String label
    ) {
        private static final String DEFAULT_BASE_URL = "http://localhost:1234/v1"; // LMStudio default
        private static final String DEFAULT_API_KEY  = "local-key";                 // dummy
        private static final String DEFAULT_LABEL    = "local-lmstudio";

        public OpenaiCompatible {
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = DEFAULT_BASE_URL;
            if (apiKey  == null || apiKey.isBlank())  apiKey  = DEFAULT_API_KEY;
            if (label   == null || label.isBlank())   label   = DEFAULT_LABEL;
        }

        public static OpenaiCompatible defaults() {
            return new OpenaiCompatible(DEFAULT_BASE_URL, DEFAULT_API_KEY, DEFAULT_LABEL);
        }
    }
}
```

기존 `LlmProperties` 의 compact constructor 에서 `openaiCompatible == null` 면 `OpenaiCompatible.defaults()` 로 보강.

### 4.3 application.yml

```yaml
app:
  llm:
    provider: ${LLM_PROVIDER:mock}   # mock | openai | vertexai-gemini | google-genai | openai-compatible
    openai-compatible:
      base-url: ${LLM_OPENAI_COMPAT_BASE_URL:http://localhost:1234/v1}
      api-key:  ${LLM_OPENAI_COMPAT_API_KEY:local-key}
      label:    ${LLM_OPENAI_COMPAT_LABEL:local-lmstudio}
```

- LMStudio: 기본값 그대로 (1234 포트 + label `local-lmstudio`).
- Ollama: `LLM_OPENAI_COMPAT_BASE_URL=http://localhost:11434/v1` + `LLM_OPENAI_COMPAT_LABEL=local-ollama`.
- 다른 OpenAI-compat 서버 (vllm, TGI 등): base-url + label 만 변경.

`spring.ai.openai.*` 키는 변경하지 않는다 (실 OpenAI 어댑터가 사용). 신규 어댑터는 자체 `OpenAiApi.builder()` 로 분리되어 영향받지 않음.

### 4.4 LlmFallbackConfig 갱신

```java
log.warn(
    "활성화된 LLM 어댑터가 없어 mock 어댑터로 fallback 합니다. "
        + "현재 app.llm.provider={} 입니다. 후보값: mock | openai | vertexai-gemini | google-genai | openai-compatible. "
        + "...",
    properties.provider()
);
```

후보값 문자열에 `openai-compatible` 추가만.

## 5. Commit 단위 실행계획

각 commit 은 단독으로 빌드/테스트 통과해야 하며, Conventional Commits 규칙 (`<type>: <subject>`) 을 따른다. PR 1 건으로 묶어 `[Feat] LLM 도메인에 self-hosted (LMStudio / Ollama) 어댑터 추가` 로 머지.

### Commit 1 — `chore: LlmProperties 에 openai-compatible 설정 블록 추가`

- `LlmProperties` record 에 `OpenaiCompatible` 중첩 record 신설 (`baseUrl`, `apiKey`, `label`).
- compact constructor 에서 null 시 `OpenaiCompatible.defaults()` 로 보강.
- `application.yml` 에 `app.llm.openai-compatible.*` 키 추가.
- 어떤 어댑터도 아직 사용하지 않으므로 동작 변경 없음. 기존 테스트는 새 필드를 무시하거나 `defaults()` 로 통과.
- 영향 파일: `LlmProperties.java`, `application.yml`, 일부 테스트 fixture.

### Commit 2 — `feat: OpenAI-compatible self-hosted LLM 어댑터 추가 (LMStudio / Ollama 공용)`

- `SpringAiOpenAiCompatibleChatCompletionAdapter` 신설.
- `@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai-compatible")`.
- 자체 `OpenAiApi.builder()` + `OpenAiChatModel.builder()` 로 Spring AI auto-config 와 독립.
- `providerName()` 은 `properties.openaiCompatible().label()` 그대로 반환.
- 본 commit 시점에 default `LLM_PROVIDER=mock` 이라 새 어댑터는 아직 활성화되지 않음 → 운영 환경 회귀 0.
- 영향 파일: 신규 어댑터 1개.

### Commit 3 — `chore: LlmFallbackConfig 후보 provider 목록에 openai-compatible 반영`

- `LlmFallbackConfig` 의 WARN 로그 후보값 문자열에 `openai-compatible` 추가.
- `MOCK_PROVIDER` 같은 기존 상수는 변경 없음.
- `llm.active.provider.info{provider, fallback}` gauge 는 자동 갱신 (Commit 2 의 어댑터가 활성화되면 라벨이 자동으로 `local-lmstudio` / `local-ollama` 등으로 표시됨).
- 영향 파일: `LlmFallbackConfig.java`.

### Commit 4 — `test: openai-compatible 어댑터 단위 + 통합 테스트 추가`

- `SpringAiOpenAiCompatibleChatCompletionAdapterTest` 신설.
- WireMock (또는 Spring 의 `MockRestServiceServer`) 으로 OpenAI-compat `/v1/chat/completions` 응답을 mocking.
- 검증 케이스:
  - `LLM_OPENAI_COMPAT_BASE_URL` 이 mock server 로 향하는지 (실제 요청 URL 검증).
  - 응답 파싱 후 `provider` 필드가 properties.label() 과 일치 (기본 `local-lmstudio`).
  - `LLM_OPENAI_COMPAT_LABEL=local-ollama` 로 override 시 응답의 provider 도 `local-ollama` 로 바뀜.
  - 5xx 응답 시 `LlmDomainException` 으로 변환됨.
  - token usage 가 응답에 없는 경우 `promptTokens` / `completionTokens` 가 null 로 노출됨 (Ollama 0.x 시나리오).
- 영향 파일: 신규 테스트 1 건.

### Commit 5 — `docs: ADR-008 amendment + LMStudio / Ollama 운영 가이드`

- ADR-008 (LLM 도메인 provider 전략) 본문에 amendment 추가:
  - "self-hosted (LMStudio / Ollama) 운영을 OpenAI-compat 단일 어댑터로 지원" 결정 기록.
  - Alternatives Considered 에 옵션 A / C / D 도 짧게 기록 (위 §3 요약).
- 신규 `docs/guides/LMStudio_Ollama_운영_가이드.md` 작성:
  - LMStudio 설치 (macOS / Windows) + 모델 다운로드 + Local Server 시작 + 포트 1234 확인.
  - Ollama 설치 (`brew install ollama`) + `ollama pull <model>` + `ollama serve` + 포트 11434 확인. v0.1.32+ 의 OpenAI-compat 활성화 확인.
  - application.yml / 환경변수 설정 (4.3 그대로 + `LLM_PROVIDER=openai-compatible` 토글).
  - 권장 한국어 분류 모델 (예: `qwen2.5:7b`, `mistral`, `llama3.1`) 과 분류 정확도 기대치.
  - mock 모드와의 차이 (실제 분류 vs 무작위 echo).
  - 알려진 한계: Ollama 0.x 의 token usage 누락 → `llm.chat.completion.tokens.total` 메트릭 0 으로 찍힘. 회피 방법 (Ollama 0.4+ 업그레이드).
  - 로컬 모델의 분류 정확도가 GPT-4 / Gemini 보다 낮을 수 있음 → 업무 fallback 채널 모니터링 권고.
  - 회로 차단 / rate limiter 가 그대로 적용됨을 명시.
- 영향 파일: ADR-008.md (편집), 신규 운영 가이드.

## 6. 위험 / 트레이드오프

| 항목 | 영향 | 완화책 |
|------|------|--------|
| 로컬 모델의 분류 정확도가 GPT-4 / Gemini 보다 낮음 | 업무 fallback 채널이 평소보다 채워짐 | preview API 로 분류 결과 사전 검증, fallback 채널 모니터링 강화. |
| 로컬 모델 응답 latency 가 GPU/CPU 에 의존 (단건 5~30s 가능) | LLM 호출 caller thread 차단 시간 증가 | 호출 비동기화를 함께 적용하면 영향 완화. |
| Ollama 0.x 의 token usage 필드 누락 | `llm_chat_completion_tokens_total` 메트릭 0 | 운영 가이드에 Ollama 0.4+ 업그레이드 권고. token 비용은 어차피 self-hosted 에선 의미 작음. |
| LMStudio / Ollama 가 OpenAI-compat 응답을 100% 모방하지 않을 가능성 | 응답 파싱 실패 → `LlmDomainException` | Commit 4 의 통합 테스트가 응답 호환성 검증. 운영 환경에선 부팅 직후 admin preview 1 회로 smoke test. |
| `local-key` 같은 dummy api-key 가 실 환경 secret store 에 누수될 가능성 | 보안 영향 0 (서버가 무시) 이지만 위생 떨어짐 | api-key 가 실제로 검증되지 않음을 가이드에 명시. 환경 변수 / properties 자체는 secret 으로 다루지 않아도 됨. |

본 계획과 LLM 호출 비동기화는 별도로 적용할 수 있다. 둘 다 적용되면 self-hosted + 비동기화가 자연스럽게 결합한다.

## 7. NOT in scope

다음은 본 계획에서 의도적으로 다루지 않는다 — 별도 결정 / 별도 ADR 사항.

- **Ollama native API 어댑터** (`/api/chat`, `/api/generate`). model 관리 / structured output / streaming 의 추가 기능이 배치 분류 use case 에 가치를 주는 시점에 별도 어댑터로 분리. 본 계획 머지 후에도 옵션 D 는 항상 살아 있다.
- **로컬 모델 자동 다운로드 / Docker 이미지 내 ollama pull**. 운영 인프라 결정 (Docker 컴포즈 / k8s 사이드카) 과 결합하므로 분리.
- **로컬 모델 정확도 평가 + 분류 품질 기준 정의**. 모델별 정확도 비교 / fallback 채널 noise 감내 정책은 운영 데이터 누적 후 별도.
- **multi-provider race / fallback chaining** (실 OpenAI 가 실패하면 Ollama 로 자동 fallback). 본 계획은 단일 활성 어댑터 모델을 유지.
- **분산 rate limit / 회로 차단**. 다중 인스턴스 운영 시 별도 ADR. 본 계획은 단일 인스턴스 가정 유지.

## 8. 검증 시나리오 (PR 머지 전 smoke test)

1. **로컬 LMStudio 부팅** → 모델 1 개 (예: `qwen2.5-7b-instruct-q4_k_m.gguf`) 로딩 → Local Server 시작 (`http://localhost:1234`).
2. application 환경변수: `LLM_PROVIDER=openai-compatible` (그 외는 default).
3. application 부팅 → 시작 로그에서 `LLM 활성 provider=local-lmstudio (어댑터=SpringAiOpenAiCompatibleChatCompletionAdapter, configured=openai-compatible, fallbackEngaged=false)` 확인.
4. `/actuator/prometheus` 에서 `llm_active_provider_info{provider="local-lmstudio",fallback="false"} 1` 노출 확인.
5. LLM 호출이 붙은 preview 또는 smoke API 호출 → 응답에서 분류 결과가 후보 도메인 키에 매칭되는지 확인.
6. 환경변수만 `LLM_OPENAI_COMPAT_BASE_URL=http://localhost:11434/v1` + `LLM_OPENAI_COMPAT_LABEL=local-ollama` 로 바꿔 Ollama 로 교체 → 동일 흐름이 정상 동작 + provider 라벨이 `local-ollama` 로 자동 변경되는지 확인.

## 9. 참고

- 관련 ADR / 보고서
    - [ADR-008: LLM 도메인 provider 전략](../adr/008-llm-domain-provider-strategy.md) — 본 계획의 amendment 대상.
    - [LLM 결제 시 rate limit / 요금 보고서](LLM_결제_요금_및_RATE_LIMIT_보고서.md) — provider 별 rate limit / 단가 변화 정리.
- 핵심 코드
    - [SpringAiOpenAiChatCompletionAdapter](../../src/main/java/com/umc/product/llm/adapter/out/external/SpringAiOpenAiChatCompletionAdapter.java) — 신규 어댑터의 base 가 되는 기존 OpenAI 어댑터.
    - [LlmProperties](../../src/main/java/com/umc/product/llm/adapter/out/external/LlmProperties.java) — `OpenaiCompatible` 추가 대상.
    - [LlmFallbackConfig](../../src/main/java/com/umc/product/llm/adapter/out/external/LlmFallbackConfig.java) — 후보 목록 갱신 대상.
- 외부 자료
    - [LMStudio Local Server docs](https://lmstudio.ai/docs/local-server) — OpenAI-compat 명세.
    - [Ollama OpenAI compatibility](https://github.com/ollama/ollama/blob/main/docs/openai.md) — v0.1.32+ 에서 활성, 호환 범위 명시.
    - [Spring AI OpenAI client builder](https://docs.spring.io/spring-ai/reference/api/clients/openai-chat.html) — `OpenAiApi.builder()` 의 base-url 커스터마이즈.
