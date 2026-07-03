# LLM 결제 시 rate limit / 요금 보고서

> 작성일: 2026-05-08
> 작성 기준: 2026-01 (모델 가격 정책 최종 확인 시점). **가격은 분기 단위로 변동**되므로, 결제 의사결정 전에 본 보고서 §6 의 공식 링크에서 최신값을 다시 확인할 것.
>
> 본 프로젝트는 [LlmProperties.java](../../src/main/java/com/umc/product/llm/adapter/out/external/LlmProperties.java) 의 `app.llm.provider` 한 값으로 4 종 어댑터 (mock / openai / vertexai-gemini / google-genai) 중 하나를 활성화한다. 본 보고서는 결제 활성화 시 각 provider 별 rate limit / 단가 변화를 정리하고, 배치형 LLM 호출량 예시 기준 월 비용을 시뮬레이션한다.

---

## 0. 핵심 요약 (TL;DR)

| 항목                          | 결론                                                                                                                                        |
|-----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **현재 활성 모델**                | `google-genai` + `gemini-2.5-flash-lite` (free tier, 15 RPM)                                                                              |
| **본 프로젝트 사전 페이싱**           | `app.llm.rate-limit.requests-per-minute=10` ([application.yml:360](../../src/main/resources/application.yml#L360)) — 결제해도 이 값 안 올리면 효과 없음 |
| **가장 cost-effective 결제 옵션** | AI Studio Tier 1 (Google Cloud 빌링 연결만) → flash-lite RPM 15 → **4,000** (266×), 월 예상 < $1                                                  |
| **정확도 더 필요할 때**             | Gemini 2.5 Flash 또는 GPT-4o-mini, 월 $1~3                                                                                                   |
| **고품질 분류 필요할 때**            | Gemini 2.5 Pro, 월 $2~5 (§1 예시 호출량 기준)                                                                                                      |

---

## 1. 예시 호출 패턴 (비용 시뮬레이션의 입력)

배치형 분류/요약 작업을 기준으로 한 보수적 호출 프로파일.

```
호출 형태:    classifyBatch (댓글 N건 → JSON 배열 응답)
호출 시점:    스케줄러 sync + admin preview ad-hoc
캐시 레이어:  L1 Caffeine + L2 Redis/DB persistent
                → 동일 입력 재호출 0
```

**기본 가정 (보수적 추정)**:

| 변수           | 값               | 근거                                                |
|--------------|-----------------|---------------------------------------------------|
| 신규 처리 대상 수   | 100 건/일         | 운영 데이터 100건/일을 가정한 보수적 예시                         |
| batch 묶음 크기  | 10~25 건         | classifyBatch 응답 토큰 한도 (32~512)                   |
| 일 호출 수       | **~12 건**       | 100 / 평균 batch 8                                  |
| 호출당 input    | 1,200 token     | system prompt(400) + candidates(200) + 댓글 묶음(600) |
| 호출당 output   | 80 token        | JSON 배열 N개 항목                                     |
| 일 input 합계   | 14,400 token    |                                                   |
| 일 output 합계  | 960 token       |                                                   |
| **월 input**  | **~430K token** |                                                   |
| **월 output** | **~30K token**  |                                                   |

> 운영 규모가 늘면 (가령 댓글 1,000/일 → 월 input 4.3M, output 300K) 본 추정의 10× 로 비례. §5 시뮬레이션에 그대로 곱해 쓰면 된다.

---

## 2. Provider 1 — Google AI Studio (`google-genai`)

본 프로젝트 기본값. Google AI Studio (Gemini Developer API) 키로 호출.

### 2.1 Tier 시스템

결제 활성화 = Google Cloud 빌링 계정 연결. 계정 구분 없이 자동으로 다음 tier 적용.

| Tier       | 진입 조건                  | 모델별 RPM 배수     |
|------------|------------------------|----------------|
| **Free**   | 빌링 미연결                 | 기본값 (15 RPM 등) |
| **Tier 1** | 빌링 연결 즉시               | ~25× ~ 250×    |
| **Tier 2** | 누적 소비 $250 + 30 일 경과   | Tier 1 의 ~2×   |
| **Tier 3** | 누적 소비 $1,000 + 30 일 경과 | Tier 2 의 ~3×   |

### 2.2 모델별 한도 / 단가

> 표의 RPM/TPM 은 2026-01 기준 공식값. RPD = requests per day.

| 모델                        | Tier   | RPM       | TPM  | RPD    | Input $/1M                              | Output $/1M                     |
|---------------------------|--------|-----------|------|--------|-----------------------------------------|---------------------------------|
| **gemini-2.5-flash-lite** | Free   | 15        | 1M   | 1,500  | $0                                      | $0                              |
|                           | Tier 1 | **4,000** | 4M   | 무제한    | **$0.10**                               | **$0.40**                       |
|                           | Tier 2 | 10,000    | 10M  | 무제한    | 동일                                      | 동일                              |
| **gemini-2.5-flash**      | Free   | 10        | 250K | 250    | $0                                      | $0                              |
|                           | Tier 1 | **1,000** | 1M   | 10,000 | **$0.30**                               | **$2.50**                       |
|                           | Tier 2 | 2,000     | 3M   | 무제한    | 동일                                      | 동일                              |
| **gemini-2.5-pro**        | Free   | 5         | 250K | 100    | $0                                      | $0                              |
|                           | Tier 1 | **150**   | 2M   | 1,000  | **$1.25**(<200K ctx) / **$2.50**(>200K) | **$10**(<200K) / **$15**(>200K) |
|                           | Tier 2 | 1,000     | 5M   | 50,000 | 동일                                      | 동일                              |

### 2.3 부가 비용

- **prompt cache** 적용 시 input 토큰의 cached 분에 25% 할인 (2.5 시리즈 기본 지원).
- **grounded with Google Search**: 호출당 별도 비용 ($35/1K 호출). 본 프로젝트 미사용.

---

## 3. Provider 2 — Vertex AI Gemini (`vertexai-gemini`)

같은 Gemini 모델을 Google Cloud Vertex AI 채널로 호출. **본 프로젝트 어댑터 [SpringAiGeminiChatCompletionAdapter](../../src/main/java/com/umc/product/llm/adapter/out/external/SpringAiGeminiChatCompletionAdapter.java) 가 이미 매핑되어 있어**, `LLM_PROVIDER=vertexai-gemini` + GCP service account JSON 만 주입하면 즉시 전환 가능.

### 3.1 한도

Vertex AI 는 **RPM 단위가 아니라 region 별 QPM/TPM 쿼터** 로 운영. 콘솔에서 quota 증액 요청 가능 (영업일 1~3 일).

| 모델                    | 기본 region 쿼터 (QPM) | 증액 한도       |
|-----------------------|--------------------|-------------|
| gemini-2.5-flash-lite | 1,000              | 협상 가능 (수만~) |
| gemini-2.5-flash      | 600                | 협상 가능       |
| gemini-2.5-pro        | 100                | 협상 가능       |

### 3.2 단가

**AI Studio Tier 1 과 동일** (Google 공식 정책상 single rate). 즉 §2.2 표의 단가가 그대로 적용.

### 3.3 AI Studio vs Vertex AI 선택 기준

| 상황                                         | 추천                                                      |
|--------------------------------------------|---------------------------------------------------------|
| 월 비용 < $50, 단순 운영                          | **AI Studio (`google-genai`)** — API 키 한 줄로 끝           |
| 운영 규모 ↑ / GCP 인프라 통합 / region pin / VPC SC | **Vertex AI (`vertexai-gemini`)** — 쿼터 증액 / IAM / 감사 로그 |
| Gemma 모델 사용                                | Vertex AI **Model Garden** 만 가능 (현 어댑터로는 불가, 별도 작업 필요)  |

---

## 4. Provider 3 — OpenAI (`openai`)

본 프로젝트 어댑터 [SpringAiOpenAiChatCompletionAdapter](../../src/main/java/com/umc/product/llm/adapter/out/external/SpringAiOpenAiChatCompletionAdapter.java) 매핑. `LLM_PROVIDER=openai` + `LLM_OPENAI_API_KEY` 주입 시 활성.

### 4.1 Tier 시스템

| Tier       | 진입 조건            |
|------------|------------------|
| Free trial | 신규 가입 시 한정 크레딧   |
| **Tier 1** | $5+ 결제           |
| Tier 2     | $50+ + 7일 경과     |
| Tier 3     | $100+ + 7일 경과    |
| Tier 4     | $250+ + 14일 경과   |
| Tier 5     | $1,000+ + 30일 경과 |

### 4.2 모델별 한도 / 단가 (Tier 1 기준)

| 모델                      | RPM | TPM  | RPD    | Input $/1M | Output $/1M | 비고                    |
|-------------------------|-----|------|--------|------------|-------------|-----------------------|
| **gpt-4o-mini**         | 500 | 200K | 10,000 | **$0.15**  | **$0.60**   | flash-lite 와 직접 비교 대상 |
| **gpt-4o**              | 500 | 30K  | —      | **$2.50**  | **$10.00**  | 2024-08 가격 인하 후       |
| **gpt-4.1-mini**        | 500 | 200K | —      | **$0.40**  | **$1.60**   | 2025 출시, 4o-mini 후속   |
| **gpt-4.1**             | 500 | 200K | —      | **$2.00**  | **$8.00**   | 4o 후속                 |
| **gpt-5-mini** (있을 시)   | 500 | 200K | —      | $0.25      | $2.00       | 2025-Q4 추정            |
| **gpt-5** (있을 시)        | 500 | 30K  | —      | $1.25      | $10.00      | 2025-Q4 추정            |
| **o4-mini** (reasoning) | 500 | 200K | —      | $1.10      | $4.40       | 분류 task 에는 과잉         |

> Tier 5 까지 올라가면 RPM 10,000+ / TPM 수십M. 본 프로젝트 호출량으로는 Tier 1 만 충분.

### 4.3 부가 사항

- **prompt caching** — automatic for prompts ≥1,024 tokens, cached portion 50% 할인.
- **batch API** (24h SLA) — 50% 할인. 분류 사이클이 5 분이라 본 프로젝트엔 부적합.

---

## 5. 본 프로젝트 호출량 기준 월 비용 시뮬레이션

§1 의 가정 (월 input 430K, 월 output 30K) 을 각 모델 단가에 곱한 결과.

### 5.1 일반 운영 가정 (월 input 430K / output 30K)

| 모델                         | Provider                | 월 input 비용 | 월 output 비용 | **월 합계**  |
|----------------------------|-------------------------|------------|-------------|-----------|
| gemini-2.5-flash-lite      | google-genai / vertexai | $0.043     | $0.012      | **$0.06** |
| gemini-2.5-flash           | google-genai / vertexai | $0.129     | $0.075      | **$0.20** |
| gemini-2.5-pro (<200K ctx) | google-genai / vertexai | $0.538     | $0.300      | **$0.84** |
| gpt-4o-mini                | openai                  | $0.065     | $0.018      | **$0.08** |
| gpt-4.1-mini               | openai                  | $0.172     | $0.048      | **$0.22** |
| gpt-4.1                    | openai                  | $0.860     | $0.240      | **$1.10** |
| gpt-4o                     | openai                  | $1.075     | $0.300      | **$1.38** |

### 5.2 운영 확대 가정 (10×, 월 input 4.3M / output 300K)

| 모델                    | **월 합계**   |
|-----------------------|------------|
| gemini-2.5-flash-lite | **$0.55**  |
| gemini-2.5-flash      | **$2.04**  |
| gemini-2.5-pro        | **$8.40**  |
| gpt-4o-mini           | **$0.83**  |
| gpt-4.1-mini          | **$2.20**  |
| gpt-4.1               | **$11.00** |
| gpt-4o                | **$13.75** |

### 5.3 캐시 효과 반영 후 (실제 운영 추정)

배치형 LLM 기능에 입력 단위 캐시 (L1 Caffeine / L2 Redis 또는 DB persistent) 를 적용하면 **동일 입력 재처리 = 0** 으로 수렴한다. 운영 정착 후 cache hit ratio ~70~90% 가정 시 위 비용의 **0.1~0.3×**.

§5.1 의 운영 비용은 이미 신규 댓글만 가정하므로 추가 할인 없음. §5.2 는 캐시 효과로 ~$2 ~ $5/월 수준이 현실적 상한.

---

## 6. Rate Limit 변화 (현재 → 결제 후)

### 6.1 본 프로젝트 사전 페이싱 (`LlmRateLimiter`)

[application.yml:360](../../src/main/resources/application.yml#L360) 의 token bucket 사전 페이싱.

```yaml
app.llm.rate-limit.requests-per-minute: 10  # 기본값
app.llm.rate-limit.burst: 5
```

> **결제로 provider 한도가 풀려도 이 값을 함께 올려야 효과**가 난다. 결제 활성화 시 동시에 환경 변수로 다음 갱신 권장.

### 6.2 결제 활성화 후 권장 페이싱

| 활성 모델                 | provider 한도 (paid Tier 1) | 권장 `LLM_RATE_LIMIT_RPM` | 마진     |
|-----------------------|---------------------------|-------------------------|--------|
| gemini-2.5-flash-lite | 4,000 RPM                 | **120**                 | 30× 마진 |
| gemini-2.5-flash      | 1,000 RPM                 | **60**                  | 16× 마진 |
| gemini-2.5-pro        | 150 RPM                   | **30**                  | 5× 마진  |
| gpt-4o-mini           | 500 RPM                   | **60**                  | 8× 마진  |

> 본 프로젝트 실제 호출 수가 0.5 RPM 도 안 되므로 위 권장값은 충분히 보수적. 단순히 "결제 직후 그대로 쓸 수 있는 값" 기준.

### 6.3 token-per-minute (TPM) 한도

§1 예시 호출당 input ~1,200 / output ~80 = ~1,300 token. 분당 호출이 60 회 가도 **78K TPM** 으로, paid Tier 1 의 1M TPM (flash) ~ 4M TPM (flash-lite) 한도 안에 충분히 들어옴.

---

## 7. 결제 활성화 절차

### 7.1 google-genai (AI Studio)

```
1. https://aistudio.google.com/app/apikey 접속
2. "Set up Billing" → Google Cloud 프로젝트의 빌링 계정 연결
3. Billing 활성화 → 자동으로 Tier 1 진입 (5~10분 내)
4. 추가 설정 없음. 기존 API Key 그대로 사용
```

전환 검증:

```bash
curl "https://generativelanguage.googleapis.com/v1beta/models?key=$LLM_GEMINI_API_KEY" | jq '.'
# tier 정보는 응답에 직접 안 나옴. 다음 호출 시 RPM 한도가 바뀌었는지로 확인.
```

### 7.2 vertexai-gemini

```
1. GCP 프로젝트의 빌링 활성화
2. Vertex AI API 활성화 (콘솔에서 "Enable")
3. service account JSON 발급 → GOOGLE_APPLICATION_CREDENTIALS 환경 변수
4. 본 프로젝트:
   LLM_PROVIDER=vertexai-gemini
   VERTEX_PROJECT_ID=<project>
   VERTEX_LOCATION=asia-northeast3  # 기본값
```

쿼터 증액: `IAM & Admin → Quotas` → Vertex AI 쿼터 검색 → 모델별 QPM 상향 요청.

### 7.3 openai

```
1. https://platform.openai.com/settings/organization/billing 접속
2. 결제수단 추가 → 최소 $5 충전 (자동 또는 수동)
3. Tier 1 진입 (즉시)
4. 본 프로젝트:
   LLM_PROVIDER=openai
   LLM_OPENAI_API_KEY=sk-...
```

### 7.4 결제 활성화 후 본 프로젝트 환경 변수 변경 묶음

```bash
# google-genai paid 전환 (가장 일반적)
LLM_PROVIDER=google-genai
LLM_MODEL=gemini-2.5-flash-lite     # 분류 정확도 ↑ 원하면 gemini-2.5-flash
LLM_GEMINI_API_KEY=<paid 키 — billing 연결된 GCP 프로젝트의 키>
LLM_RATE_LIMIT_RPM=120              # 사전 페이싱 마진 30×
LLM_RATE_LIMIT_BURST=30             # burst 도 함께 올림
```

배포 후 [LlmCallGuard](../../src/main/java/com/umc/product/llm/application/service/LlmCallGuard.java) 의 INFO 로그 ("LLM 활성 provider=...") 로 어떤 어댑터가 활성화됐는지 확인.

---

## 8. 추천 의사결정 매트릭스

| 운영 단계                  | 권장 provider               | 권장 모델       | 월 예상 비용 | 비고                                         |
|------------------------|---------------------------|-------------|---------|--------------------------------------------|
| **현재 (PoC)**           | mock 또는 google-genai free | flash-lite  | $0      | 15 RPM 한계로 batch 묶음 필수                     |
| **결제 직후 (1단계)**        | google-genai Tier 1       | flash-lite  | <$1     | 가장 cost-effective. RPM 4,000               |
| **정확도 부족 시 (2단계)**     | google-genai Tier 1       | flash       | $1~3    | input/output 단가 ~3×~6× 증가                  |
| **정확도 critical (3단계)** | google-genai Tier 1       | pro         | $1~10   | 분류 정확도 + 긴 context. RPM 150 → 사전 페이싱 30 권장 |
| **GCP 인프라 통합 시**       | vertexai-gemini           | 동일          | 동일      | IAM / 감사 / region pin                      |
| **OpenAI 비교 검증 시**     | openai Tier 1             | gpt-4o-mini | <$1     | A/B 비교 후 결정                                |

---

## 9. 주의사항 / 함정

1. **사전 페이싱 미갱신** — 결제만 하고 `LLM_RATE_LIMIT_RPM` 안 올리면 효과 0. 환경 변수 묶음으로 함께 변경.
2. **모델 ID 오타로 404** — Spring AI 는 model id 를 string 으로 그대로 전달한다. `gemma-4-31b` 같은 비공식 ID 는 즉시 404. AI Studio 에서 ListModels API 로 검증.
3. **prompt cache 미적용** — 본 프로젝트의 system prompt 는 ~400 token 으로 1,024 미만이라 OpenAI auto-cache 미적용. Gemini 는 explicit caching 필요. 호출량 ↑ 후 별도 도입 검토.
4. **fallback 도메인 발송 비용 누락** — 분류 매칭 실패 댓글이 fallback 채널로 가도 LLM 호출 1 회는 발생. 비용은 동일.
5. **circuit breaker open 시 mock fallback** — [LlmFallbackConfig](../../src/main/java/com/umc/product/llm/adapter/out/external/LlmFallbackConfig.java) 는 활성 ChatModel 빈이 없으면 mock 으로 떨어진다. 결제 후에도 환경 변수 오타로 mock 으로 떨어질 수 있으므로, 부팅 직후 `LLM 활성 provider=...` 로그를 반드시 확인.
6. **TPM 도 별개 한도** — RPM 만 보고 안심하면 안 됨. 본 프로젝트는 1,300 token/호출 수준이라 무시 가능하지만, prompt 가 길어지면 TPM 이 먼저 막힌다.
7. **가격은 분기 단위로 변동** — 본 보고서는 2026-01 기준. 결제 의사결정 시 §10 의 공식 페이지에서 재확인 필수.

---

## 10. 공식 가격 / 한도 페이지 (재확인용)

- **Google AI Studio (Gemini Developer API)**:
    - 가격: https://ai.google.dev/gemini-api/docs/pricing
    - Rate limits / Tier: https://ai.google.dev/gemini-api/docs/rate-limits
- **Vertex AI Gemini**:
    - 가격: https://cloud.google.com/vertex-ai/generative-ai/pricing
    - 쿼터: https://cloud.google.com/vertex-ai/generative-ai/docs/quotas
- **OpenAI**:
    - 가격: https://openai.com/api/pricing/
    - Rate limits / Tier: https://platform.openai.com/docs/guides/rate-limits

---

## 11. 결론

§1 의 배치형 호출량 예시 (월 input ~430K token / output ~30K token) 기준 결제 활성화 비용은 **모든 주요 모델에서 월 $0.06 ~ $1.38** 범위. 실질적으로 비용은 의사결정 변수가 아니며, **선택은 정확도 / 운영 인프라 통합 / provider 락인 의 trade-off** 로 수렴한다.

**행동 권고**:

1. **즉시**: `google-genai` 를 paid Tier 1 로 전환 (flash-lite 유지). 비용 < $1/월, RPM 15 → 4,000 (266×).
2. **2~4 주 운영 후**: fallback 채널에 분류 실패가 누적된 경우 `gemini-2.5-flash` 로 모델만 교체. 비용 +$1~2/월.
3. **장기**: GCP 인프라 통합이 진행되면 `vertexai-gemini` 로 provider 만 교체 (모델 / 비용 동일).

---

## 12. 본 보고서가 다루지 않은 항목

- Gemma / Llama / Claude / Mistral 등 본 프로젝트 어댑터 미지원 모델 — 별도 어댑터 PR 필요 (ADR 1 건 + 1~2 PR 분량).
- Spot / batch / committed-use 할인 — 본 프로젝트 호출량 규모로는 무의미.
- Egress / region 차이 비용 — Vertex AI 동일 region 사용 가정.
- 멀티 provider 동시 활성화 — 현 아키텍처는 `@ConditionalOnProperty` 단일 활성 (ADR-006 §Decision).
