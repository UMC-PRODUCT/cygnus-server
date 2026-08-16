# LLM 테스트 케이스

- 테스트 파일: 4개
- 테스트 케이스: 15개
- 분류 기준: `Controller`, `UseCase`, `Repository`, `E2E`, `Scheduler`, `Domain`, `External Adapter`, `Support`

| 카테고리 | 케이스 수 |
|---|---:|
| UseCase / Application Service | 11 |
| External Adapter | 4 |

## UseCase / Application Service

### ChatCompletionServiceTest
- 테스트 설명: ChatCompletionService
- 위치: `src/test/java/com/umc/product/llm/application/service/ChatCompletionServiceTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [62](../../../src/test/java/com/umc/product/llm/application/service/ChatCompletionServiceTest.java#L62) | ChatCompletionService / 가드가 차단 상태이면 즉시 실패하고 circuit-open 메트릭을 기록한다 (rate limiter 미호출) | 호출 complete(ChatCompleteCommand.freeForm("s", "u"))) | 실패: 예외 LlmDomainException; 검증 assertThat(counter).isNotNull(); assertThat(counter.count()).isEqualTo(1.0); |
| [80](../../../src/test/java/com/umc/product/llm/application/service/ChatCompletionServiceTest.java#L80) | ChatCompletionService / 정상 호출 시 rate limiter 의 acquire 가 호출된다 | 호출 complete(ChatCompleteCommand.freeForm("s", "u")) | 성공: ChatCompletionService / 정상 호출 시 rate limiter 의 acquire 가 호출된다 |
| [93](../../../src/test/java/com/umc/product/llm/application/service/ChatCompletionServiceTest.java#L93) | ChatCompletionService / 정상 응답이면 success 메트릭과 토큰 카운터를 증가시킨다 (응답 의미 검증은 호출자 책임) | 호출 complete(command) | 성공: 검증 assertThat(result.text()).isEqualTo("a"); assertThat(timer).isNotNull(); assertThat(timer.count()).isEqualTo(1L); assertThat(registry.find("llm.chat.completion.tokens.total") |
| [118](../../../src/test/java/com/umc/product/llm/application/service/ChatCompletionServiceTest.java#L118) | 어댑터가 LlmDomainException 을 던지면 failed 메트릭을 기록하고 가드 실패를 누적한다 | 호출 complete(ChatCompleteCommand.freeForm("s", "u"))) | 실패: 예외 LlmDomainException; 에러코드 LlmErrorCode.CHAT_COMPLETION_FAILED; 검증 assertThat(counter).isNotNull(); assertThat(counter.count()).isEqualTo(1.0); |

### LlmCallGuardTest
- 테스트 설명: LlmCallGuard
- 위치: `src/test/java/com/umc/product/llm/application/service/LlmCallGuardTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [36](../../../src/test/java/com/umc/product/llm/application/service/LlmCallGuardTest.java#L36) | LlmCallGuard / 초기 상태는 호출을 허용한다 | 조건 LlmCallGuard / 초기 상태는 호출을 허용한다 | 성공: 검증 assertThat(guard.allow()).isTrue(); |
| [42](../../../src/test/java/com/umc/product/llm/application/service/LlmCallGuardTest.java#L42) | LlmCallGuard / 연속 실패가 임계 미만이면 차단되지 않는다 | 조건 LlmCallGuard / 연속 실패가 임계 미만이면 차단되지 않는다 | 실패: 검증 assertThat(guard.allow()).isTrue(); |
| [51](../../../src/test/java/com/umc/product/llm/application/service/LlmCallGuardTest.java#L51) | LlmCallGuard / 연속 실패가 임계 도달하면 openDuration 동안 차단된다 | 조건 LlmCallGuard / 연속 실패가 임계 도달하면 openDuration 동안 차단된다 | 실패: 검증 assertThat(guard.allow()).isFalse(); assertThat(guard.allow()).isTrue(); |
| [65](../../../src/test/java/com/umc/product/llm/application/service/LlmCallGuardTest.java#L65) | LlmCallGuard / 성공 호출은 카운터와 차단 상태를 모두 초기화한다 | 조건 LlmCallGuard / 성공 호출은 카운터와 차단 상태를 모두 초기화한다 | 성공: 검증 assertThat(guard.allow()).isTrue(); |

### LlmRateLimiterTest
- 테스트 설명: LlmRateLimiter
- 위치: `src/test/java/com/umc/product/llm/application/service/LlmRateLimiterTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/llm/application/service/LlmRateLimiterTest.java#L12) | requestsPerMinute=0 이면 acquire 가 즉시 통과한다 | 조건 requestsPerMinute=0 이면 acquire 가 즉시 통과한다 | 성공: requestsPerMinute=0 이면 acquire 가 즉시 통과한다 |
| [26](../../../src/test/java/com/umc/product/llm/application/service/LlmRateLimiterTest.java#L26) | LlmRateLimiter / burst 개수만큼은 sleep 없이 즉시 acquire 한다 | 조건 LlmRateLimiter / burst 개수만큼은 sleep 없이 즉시 acquire 한다 | 성공: 검증 assertThat(elapsedMs).isLessThan(50); |
| [42](../../../src/test/java/com/umc/product/llm/application/service/LlmRateLimiterTest.java#L42) | LlmRateLimiter / burst 소진 후 토큰 리필 시간이 지나면 다음 acquire 가 통과한다 | 조건 LlmRateLimiter / burst 소진 후 토큰 리필 시간이 지나면 다음 acquire 가 통과한다 | 성공: LlmRateLimiter / burst 소진 후 토큰 리필 시간이 지나면 다음 acquire 가 통과한다 |

## External Adapter

### ChatPromptHelperTest
- 테스트 설명: ChatPromptHelper
- 위치: `src/test/java/com/umc/product/llm/adapter/out/external/ChatPromptHelperTest.java`

| 라인 | 테스트 케이스 | 입력/조건 | 기대 결과 |
|---:|---|---|---|
| [12](../../../src/test/java/com/umc/product/llm/adapter/out/external/ChatPromptHelperTest.java#L12) | 응답 정규화는 앞뒤 공백을 제거하고 null 은 빈 문자열로 처리한다 | 조건 응답 정규화는 앞뒤 공백을 제거하고 null 은 빈 문자열로 처리한다 | 성공: 검증 assertThat(ChatPromptHelper.normalizeResponse(" hello ")).isEqualTo("hello"); assertThat(ChatPromptHelper.normalizeResponse("\nx\n")).isEqualTo("x"); assertThat(ChatPromptHelper.normalizeResponse(null)).isEmpty(); |
| [23](../../../src/test/java/com/umc/product/llm/adapter/out/external/ChatPromptHelperTest.java#L23) | ChatPromptHelper / ChatResponse 메타데이터에서 토큰 사용량을 long 으로 추출한다 | 조건 ChatPromptHelper / ChatResponse 메타데이터에서 토큰 사용량을 long 으로 추출한다 | 성공: 검증 assertThat(ChatPromptHelper.extractPromptTokens(response)).isEqualTo(120L); assertThat(ChatPromptHelper.extractCompletionTokens(response)).isEqualTo(8L); |
| [38](../../../src/test/java/com/umc/product/llm/adapter/out/external/ChatPromptHelperTest.java#L38) | ChatPromptHelper / ChatResponse 또는 메타데이터가 부재하면 토큰 추출은 null 을 반환한다 | 조건 ChatPromptHelper / ChatResponse 또는 메타데이터가 부재하면 토큰 추출은 null 을 반환한다 | 성공: 검증 assertThat(ChatPromptHelper.extractPromptTokens(null)).isNull(); assertThat(ChatPromptHelper.extractCompletionTokens(null)).isNull(); assertThat(ChatPromptHelper.extractPromptTokens(response)).isNull(); |
| [49](../../../src/test/java/com/umc/product/llm/adapter/out/external/ChatPromptHelperTest.java#L49) | ChatPromptHelper / max-tokens override 가 있으면 그 값을, 없으면 properties 기본값을 반환한다 | 조건 ChatPromptHelper / max-tokens override 가 있으면 그 값을, 없으면 properties 기본값을 반환한다 | 성공: 검증 assertThat(ChatPromptHelper.resolveMaxOutputTokens(withOverride, properties)).isEqualTo(256); assertThat(ChatPromptHelper.resolveMaxOutputTokens(withoutOverride, properties)).isEqualTo(32); |
