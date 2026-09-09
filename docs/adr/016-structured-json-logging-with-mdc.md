# ADR-016: API 로그를 MDC 기반 JSON 구조화 로그로 전환한다

## Status

Accepted (2026-05-12)

## Context

본 ADR 작성 시점(2026-05-12) 기준 UMC PRODUCT 서버의 로그는 Logback의 텍스트 패턴 기반으로 출력되고, Loki4j 어펜더를 통해 Grafana Cloud Loki로 push 되고 있다.

### 1. 현행 로깅 구조

#### 1.1 출력 포맷 (logback-spring.xml)

`src/main/resources/logback-spring.xml` 의 패턴은 다음과 같다.

```text
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId}] [%thread] %logger{40} : %msg%n
```

즉 모든 프로필에서 사람이 읽기 쉬운 한 줄 문자열 로그를 출력한다. ([logback-spring.xml §15-16](../../src/main/resources/logback-spring.xml#L15-L16))

- `local` 프로필은 `LOCAL_CONSOLE` (컬러 + 축약 traceId) + `LOKI` 두 어펜더.
- `dev` 프로필은 `ECS_CONSOLE` + `LOKI`.
- `prod` 프로필은 `ECS_CONSOLE` + `LOKI`.

#### 1.2 LoggingInterceptor 의 현행 로그 두 줄

[LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java) 는 요청·응답 시점에 다음과 같이 두 줄의 로그를 찍는다.

```text
[REQ] 💗 GET /forms/123/answers?cursor=10
[RES] ✅ 200 /forms/123/answers 842ms | Query Count: 12, Time: 65ms | IP: 1.2.3.4
```

문자열 안에 메서드, URI, 상태, durationMs, Query Count, Time, IP 가 한 문자열로 합쳐져 있다. 따라서:

- Loki/CloudWatch에서 `durationMs > 1000` 같은 **숫자 비교 쿼리가 불가능**하다 (regex 추출이 강제됨).
- `uriTemplate` 이 아니라 `requestURI` 기준이라 `/forms/123/answers` 와 `/forms/124/answers` 가 다른 라인으로 집계되어 P95·P99 같은 **API 단위 통계가 불가능**하다.
- 요청 ID는 Micrometer의 `traceId` 가 MDC 에 들어 있을 뿐, `requestId` / `userId` 가 MDC에 들어 있지 않다.
- 인증된 사용자라도 `memberId` 가 로그에 남지 않아, 한 사용자의 요청 흐름을 추적하려면 controller 로그를 따로 봐야 한다.

#### 1.3 인증 객체

[MemberPrincipal.java](../../src/main/java/com/umc/product/global/security/MemberPrincipal.java) 는 `memberId : Long` 만 들고 있는 OAuth2User 구현체다. [JwtAuthenticationFilter.java](../../src/main/java/com/umc/product/global/security/JwtAuthenticationFilter.java) 에서 인증이 완료된 뒤 `SecurityContextHolder` 에 들어간다. 즉 Interceptor 가 동작하는 시점에는 이미 인증이 끝나 있어, `MemberPrincipal.getMemberId()` 로 `userId` 를 꺼낼 수 있다.

#### 1.4 의존성

[dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)의 관측 의존성은 다음과 같다.

- `io.micrometer:micrometer-registry-prometheus`, `micrometer-registry-otlp`
- `net.logstash.logback:logstash-logback-encoder:9.0`
- `io.micrometer:micrometer-tracing-bridge-otel`, `opentelemetry-exporter-otlp`, `context-propagation`
- `io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0`

명시 버전은 Version Catalog에서 관리한다.

#### 1.5 운영 상의 한계

- 특정 `traceId` 가 아니라 임의의 키워드 (`durationMs > 1000`, `statusCode >= 500`, 특정 `userId`) 로 필터링하려면 LogQL `| regexp` 가 필요하다. cardinality 가 큰 정규식 추출은 Loki 에서 query latency 가 크다.
- ADR-014 에서 결정한 자체 호스팅 Grafana 스택으로 이관하면, Loki 의 chunk 가 자체 인프라에 누적된다. 텍스트 로그를 1년치 쌓으면 같은 정보를 JSON 으로 쌓는 것보다 검색 비용이 누적적으로 더 커진다.
- OpenTelemetry 로그에 들어가는 message 도 텍스트 한 줄이라 attribute 기반 search 에서 효용이 떨어진다.

### 2. 결정이 필요한 이유

다음 요인이 누적되어 있다.

1. **분산 추적 / 사용자 추적 / 성능 디버깅 요구사항이 커지고 있다.** LLM / GitHub / Apple / 카카오 / 이메일 등 외부 API 호출이 늘면서, 어느 외부 API 가 느린지, 어느 `uriTemplate` 이 느린지, 특정 `memberId` 가 실패하는 패턴이 있는지 데이터 기반으로 보고 싶다.
2. **로그 시스템 (Loki) 의 활용 한계가 보인다.** 현재 패턴 로그는 Loki label (`application`, `level`) 외의 모든 필드를 정규식으로 추출해야 한다. JSON 로그로 바꾸면 `| json | durationMs > 1000` 한 줄이면 된다.
3. **ADR-014 의 자체 호스팅 이관과 시점이 맞는다.** 어차피 collector / scrape 구조를 손대는 시점에 로그 포맷도 함께 정리하면 변경 비용이 한 번에 끝난다.
4. **사용자 식별이 traceId 만으로는 부족하다.** Micrometer 의 `traceId` 는 요청 1개에 한정된다. 같은 사용자의 30분 동안의 모든 실패를 보고 싶을 때 `userId` 가 MDC 에 있어야 한다.

다만 다음 비용을 함께 짊어진다.

- **로컬 개발 UX 저하**: JSON 한 줄은 사람이 읽기 어렵다. 로컬에서 무지성으로 JSON 로 바꾸면 개발 생산성이 떨어진다.
- **MDC 누수 위험**: ThreadLocal 기반 MDC는 finally 블록의 `MDC.clear()` 가 누락되면 스레드풀에서 이전 요청의 `userId` 가 다음 요청에 섞인다.
- **민감정보 노출 위험**: JSON 으로 필드를 잘게 쪼개면, 무심코 `request.body` / `Authorization` 헤더 / 인증번호 등을 MDC 에 넣고 그대로 stdout 으로 흘려보낼 수 있다. 텍스트 로그는 사람이 한번 더 의심하는 단계가 있지만, JSON 은 "그냥 한 필드 추가" 로 보여 경계가 약해진다.
- **logback-spring.xml 의 dual-config 부담**: 프로필별로 어펜더가 다르면 관리 대상이 늘어난다.

본 ADR 은 이 trade-off 를 가시화하고, **JSON 구조화 로그를 단계적·커밋 단위로 도입** 하는 결정을 명시한다.

## Decision

우리는 **`local` 은 기존 텍스트 로그를 유지하고, `dev` / `staging` / `prod` 는 `logstash-logback-encoder` 기반 JSON 단일 라인 로그로 출력**하기로 결정한다. 그리고 다음 운영 규약을 함께 채택한다.

> **보강 (2026-05-12, Accepted 승격 시):** 본 ADR 의 최초 작성 시점에는 `staging` 프로필 분기가 누락되어 있었다. 현재 [logback-spring.xml](../../src/main/resources/logback-spring.xml) 은 `local` / `dev` / `prod` 만 분기하므로, 본 ADR 의 Commit #3 에서 `staging` 프로필 분기를 **함께 신규 추가** 한다. `staging` 의 어펜더 구성은 `dev` 와 동일 (CONSOLE_JSON + LOKI, Sentry 없음) 로 한다.

1. **요청 문맥 (`memberId`, `method`, `path`, `uriTemplate`) 은 MDC 에 넣는다.** 모든 요청은 `LoggingInterceptor` 의 `preHandle` 에서 MDC 에 push, `afterCompletion` 의 finally 에서 `MDC.clear()` 로 비운다. 사용자 식별자는 일반 OAuth 컨벤션의 `userId` 가 아니라 우리 서비스 도메인 명명인 `memberId` 그대로 사용한다.
2. **요청 완료 1줄 (`api_request_completed`) 의 event 키를 표준화**한다. 기존 `[REQ]` / `[RES]` 두 줄 로그는 `event=api_request_started` / `event=api_request_completed` 의 구조화된 이벤트로 대체한다.
3. **민감 필드는 로그에 절대 남기지 않는다.** Authorization, accessToken, refreshToken, OAuth authorization code, identity token, 이메일 인증번호, request body 전문, response body 전문, 첨부파일 내용. 필요한 경우 길이 / 개수 같은 요약값만 남긴다.
4. **Loki4j 의 운영을 ADR-014 의 결정에 위임한다.** 본 ADR 은 어펜더 자체의 송신 경로를 손대지 않는다. `loki-logback-appender` 의 `<format><message>` 패턴만 JSON encoder 의 출력 (한 줄 JSON) 으로 교체한다.
5. **단일 요청 식별자는 Micrometer Tracing 의 `traceId` 를 그대로 사용한다.** 별도의 UUID 기반 `requestId` 는 발급하지 않는다.

> **보강 (2026-05-12, Accepted 승격 후 정정):** ADR 초안 시점에는 `traceId` (Tempo 분산 추적용) 와 `requestId` (Loki 검색용 fallback) 를 공존시킨다고 결정했으나, 이후 FE 가 이미 `X-Trace-Id` 응답 헤더와 `traceId` 키로 운영 중인 흐름을 깨지 않기 위해 단일 식별자 (`traceId`) 만 사용하기로 정정한다. Loki 의 단일 요청 검색은 `| json | traceId="..."` 한 줄로 동일한 효용을 얻는다. 응답 헤더는 기존대로 `X-Trace-Id` 만 노출하며, `X-Request-Id` 는 도입하지 않는다.

### 본 ADR 의 범위 밖

- ADR-014 에서 다루는 Grafana Cloud → 자체 호스팅 이관 자체.
- Sentry 의 error grouping 정책 변경.
- OpenTelemetry log signal (OTLP logs) 도입. 본 ADR 은 logback → stdout JSON 만 다룬다. OTLP logs 는 Phase 2 ADR 후보로 분리.
- 외부 API 호출 (`external_api_called`) 의 구조화 이벤트는 본 ADR 의 인프라 위에서 후속 작업으로 점진 추가한다 (커밋 #7 에서 hook 만 제공, 도메인별 적용은 별도 PR).

## Alternatives Considered

### 1. 현행 텍스트 패턴 + Loki regex 추출 유지

장점:

- 로컬 / 운영 양쪽에서 사람이 한 줄로 읽기 쉽다.
- 이미 동작하고 있어 변경 비용이 0.

단점:

- LogQL 에서 `durationMs > 1000`, `statusCode >= 500` 같은 숫자 비교가 정규식 추출 후에야 가능. 대규모 시간 범위 쿼리에서 query latency 가 크다.
- `path` 와 `uriTemplate` 의 구분이 없어 API 단위 P95 / P99 집계 자체가 어렵다.
- 새로운 필드를 추가할 때마다 LogQL 의 regex 도 같이 수정해야 한다.

선택하지 않은 이유:
현재 도메인 (LLM / GitHub / 외부 API / 인증 / 댓글) 이 커지면서, 정규식이 아니라 필드 기반 검색이 디버깅의 1차 도구가 되어야 한다. 텍스트 유지의 단기 이득보다 1년치 누적 쿼리 비용이 크다.

### 2. 모든 프로필을 JSON 로 통일

장점:

- 환경별 분기가 사라져 설정이 단순해진다.
- 로컬과 운영의 로그 포맷이 동일해 "로컬에서는 보이지만 운영에서는 보이지 않는다" 식의 차이가 사라진다.

단점:

- 로컬 개발자 콘솔에서 한 줄 JSON 은 사람이 읽기 어렵다. `jq` 파이프를 강제하거나 IDE 의 JSON pretty-print 가 필요.
- IDE 의 stacktrace 클릭 가능성이 줄어든다 (logger 의 한 줄에 stacktrace 가 escaped JSON 으로 들어감).

선택하지 않은 이유:
로컬은 사람이 즉시 읽는 환경, 운영은 기계가 검색하는 환경. 같은 포맷을 강제하면 둘 중 하나의 UX 가 무조건 손해본다. `local` 텍스트 / `dev,prod` JSON 분기가 작은 비용으로 둘 다 만족시킨다.

### 3. `log.info("event=X key1=v1 key2=v2")` 의 logfmt 한 줄 텍스트

장점:

- JSON 보다 사람이 읽기 좋다.
- Loki 의 `| logfmt` 파서가 그대로 지원.

단점:

- nested 구조 (예: `{event: ..., http: {method: ..., status: ...}}`) 를 표현할 수 없다.
- escape 규칙이 모호 (값에 공백·따옴표가 들어가면 깨진다).
- stacktrace 를 한 줄에 직렬화하기 어렵다.

선택하지 않은 이유:
현재의 외부 API 호출 / GitHub webhook / LLM provider 호출은 nested 필드가 자연스럽다. JSON 이 long-term 확장에 유리하고, Loki `| json` 파서도 마찬가지로 1급 지원이다.

### 4. ECS Logging Format (Elastic Common Schema) 어펜더 사용

장점:

- 필드명이 ECS 표준 (e.g. `http.request.method`, `http.response.status_code`) 으로 통일.
- Kibana / Elasticsearch 와 연동 시 변환 비용 0.

단점:

- 우리는 ELK 가 아니라 Loki 기반이다. ECS 의 nested 표기 (`http.request.method`) 가 Loki `| json` 에서 다소 어색.
- 필드명이 길어 한 줄 로그의 크기가 커진다.
- 운영자가 ECS 스키마를 학습해야 한다.

선택하지 않은 이유:
우리는 Elasticsearch 를 운영할 계획이 없고, ECS 의 학습 비용이 본 ADR 의 동기 (디버깅 / 검색) 에 추가 이득을 주지 않는다. logstash-logback-encoder 의 평탄한 flat 필드명이 LogQL `| json` 과 더 잘 맞는다.

### 5. SLF4J 2.x 의 fluent API + KeyValuePair 만 사용 (encoder 변경 없이)

장점:

- 코드 레벨에서 `log.atInfo().addKeyValue("userId", id).log("msg")` 로 키-값을 추가.
- encoder 를 안 바꿔도 SLF4J 가 자동 fluent 처리.

단점:

- 패턴 인코더는 KeyValuePair 를 `%kvp` 변환자로만 한 줄에 풀어쓴다 — JSON 화는 별개.
- 즉 출력 포맷 (텍스트 vs JSON) 은 여전히 encoder 가 결정한다.

선택하지 않은 이유:
fluent API 는 "코드 작성 방식" 의 선택이고, JSON 포맷은 "출력 방식" 의 선택이다. 두 결정은 독립적이며, 본 ADR 은 출력 포맷 결정이 우선. fluent API 도입은 별도로 도입할 수 있다 (커밋 #6 의 `StructuredArguments.kv(...)` 가 같은 효과를 제공).

## Consequences

### Positive

- Loki / CloudWatch / Kibana 어느 수집기를 쓰더라도 `| json` 한 줄로 필드 기반 검색이 가능해진다.
- `uriTemplate` 기준 P95 / P99 / count 집계가 LogQL 한 줄로 가능 (`| json | uriTemplate="..."`).
- 특정 `userId` 의 30분간 실패 흐름 / 특정 `requestId` 의 단일 요청 흐름을 1쿼리로 추적 가능.
- 외부 API 호출 (`provider=GITHUB operation=FETCH_PULL_REQUESTS`) 의 성공률 / 평균 응답시간을 dashboard 에 올릴 수 있다 (커밋 #7 의 hook).
- Sentry / Tempo / Loki 모두 같은 `traceId` 로 jump 가능해, incident 분석에서 도구 간 context 손실이 줄어든다.
- 로그 한 줄의 의미가 사람이 읽는 message 가 아니라 event name + 구조화된 필드가 되므로, 자동화된 alerting 룰 (e.g. `event=external_api_called result=FAILURE` 가 5분에 N 회 이상) 작성이 단순해진다.

### Negative

- 로컬은 텍스트, 운영은 JSON 으로 갈라지므로 "로컬에서 잘 보였는데 운영 로그에서 같은 string match 가 안 된다" 류의 혼동이 가능. message 본문에 의존하지 않고 event 이름으로 검색하도록 운영 규약을 함께 안내해야 한다.
- MDC 누수 위험이 새로 들어온다. `MDC.clear()` 를 빠뜨리면 스레드풀에서 이전 요청의 `userId` 가 다음 요청 로그에 섞인다. 본 ADR 은 `LoggingInterceptor.afterCompletion()` 의 `try/finally` 로 강제하고, 비동기 / scheduler / WebSocket 경로는 별도 처리 (커밋 #5).
- logstash-logback-encoder 의 의존성이 새로 추가된다 (deploy size 약 +300KB jar).
- 로그 1줄 크기가 텍스트 (~200~300B) → JSON (~600~900B) 으로 2~3배 증가. Loki ingestion / 저장 비용에 직접 영향. ADR-014 의 자체 호스팅 retention 정책에서 함께 고려.
- 기존 dashboard 의 LogQL 룰이 `| regexp` 기반이라면 모두 `| json` 기반으로 리팩토링해야 한다 (커밋 #9 에서 일괄).
- `[REQ] 💗` 같은 이모지 로그는 사라진다. 운영 로그를 사람이 한눈에 보는 UX 가 일부 손해.

### Neutral / Trade-offs

- `path` 와 `uriTemplate` 을 같이 남기므로 한 줄 로그 크기가 늘지만, 분석 효용이 더 크다.
- `traceId` 와 `requestId` 를 둘 다 남기므로 cardinality 가 두 배가 되지만, 둘 다 Loki **label 이 아니라 JSON body** 에 들어가므로 Loki cardinality 폭발에는 영향 없다 (ADR-014 §13).
- `local` 만 텍스트 유지하므로, 로컬에서 JSON 출력 결과를 확인하려면 일시적으로 `--spring.profiles.active=dev` 로 띄우는 운영 트릭이 필요.
- 새 어펜더의 timezone 은 `Asia/Seoul` 로 고정. CloudWatch / Loki 의 timestamp 표시가 KST 로 통일되지만, UTC 운영 도구와 jump 할 때 변환이 필요할 수 있다.

## Implementation Notes

### 커밋 단위 실행 계획

본 ADR 의 변경은 **9 개의 커밋으로 쪼개서 PR 1~3 개에 묶어 머지**한다. 각 커밋은 독립적으로 빌드 가능하고, 운영에 배포되어도 안전한 상태여야 한다. 회피해야 할 패턴은 "한 PR 에 logback-spring.xml + Interceptor + LoggingFilter 를 모두 바꿔서 무엇이 깨졌는지 모르는 상태" 다.

#### Commit #1 — `chore: add logstash-logback-encoder dependency`

목적: 라이브러리만 추가하고 아무 동작도 바꾸지 않는다. 별도 PR 로 분리해도 좋다.

변경 파일:

- [dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)
- [libs.versions.toml](../../gradle/libs.versions.toml)

내용:

```kotlin
// gradle/dependencies.gradle.kts
dependencies {
    // 기존
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("com.github.loki4j:loki-logback-appender:1.5.2")
    implementation("io.micrometer:micrometer-registry-otlp")

    // 신규 — JSON 구조화 로그 encoder
    add("implementation", "net.logstash.logback:logstash-logback-encoder:${version("logstash-logback-encoder")}")
    // ...
}
```

검증:

- `./gradlew build` 가 성공한다.
- 기존 로그 출력 포맷이 그대로 (텍스트) 인지 확인.

#### Commit #2 — `feat: introduce JSON console appender for dev/prod profiles`

목적: 새로운 `CONSOLE_JSON` 어펜더를 추가하되 **아직 어떤 root 에도 연결하지 않는다**. 즉 새 어펜더는 정의만 되고 동작하지 않는 dead-code 상태. 어펜더 정의 자체에 오타가 있어도 운영에는 영향 없다.

변경 파일:

- [logback-spring.xml](../../src/main/resources/logback-spring.xml)

내용:

```xml
<springProperty name="APP_NAME" source="spring.application.name" defaultValue="umc-product-api"/>
<springProperty name="ENV" source="spring.profiles.active" defaultValue="local"/>

<appender name="CONSOLE_JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp>
                <fieldName>timestamp</fieldName>
                <timeZone>Asia/Seoul</timeZone>
            </timestamp>
            <logLevel><fieldName>level</fieldName></logLevel>
            <loggerName><fieldName>logger</fieldName></loggerName>
            <threadName><fieldName>thread</fieldName></threadName>
            <message><fieldName>message</fieldName></message>
            <mdc/>
            <stackTrace><fieldName>stackTrace</fieldName></stackTrace>
            <globalCustomFields>
                {"service":"${APP_NAME}","environment":"${ENV}"}
            </globalCustomFields>
        </providers>
    </encoder>
</appender>
```

검증:

- 기존 `local` / `dev` / `prod` root 의 어펜더 참조는 그대로다.
- 빌드 / 부팅 / 로그 포맷이 모두 변경 전과 동일.

#### Commit #3 — `feat: switch dev/prod root appender to CONSOLE_JSON`

목적: 실제 포맷 전환. 이 커밋부터 `dev` / `prod` 의 stdout 이 JSON 한 줄이 된다.

변경 파일:

- [logback-spring.xml](../../src/main/resources/logback-spring.xml)

내용:

```xml
<springProfile name="local">
    <root level="INFO">
        <appender-ref ref="LOCAL_CONSOLE"/>
        <appender-ref ref="LOKI"/>
    </root>
</springProfile>

<springProfile name="dev">
    <logger name="com.umc.product" level="DEBUG"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON"/>
        <appender-ref ref="LOKI"/>
    </root>
</springProfile>

<springProfile name="prod">
    <logger name="com.umc.product" level="${LOGGING_APP_LEVEL:-INFO}"/>
    <root level="INFO">
        <appender-ref ref="CONSOLE_JSON"/>
        <appender-ref ref="LOKI"/>
    </root>
</springProfile>
```

주의:

- `loki4j` 의 `<format><message>` 패턴은 본 커밋에서 변경하지 않는다. Loki 로 가는 메시지는 여전히 기존 `${FULL_LOG_PATTERN}` 텍스트. stdout (CloudWatch / kubectl logs) 만 JSON 으로 바뀐다. Loki 송신을 JSON 으로 바꾸는 작업은 ADR-014 의 OTel Collector 이관과 함께 다룬다.
- 배포 직후 운영 dashboard 에서 `[REQ]` / `[RES]` regex 기반 패널이 깨질 수 있다. **이 커밋의 배포 전에 커밋 #9 (LogQL 룰 업데이트) 의 PR 을 함께 준비**해 둔다.

검증:

- `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun` 으로 띄운 뒤 stdout 이 `{"timestamp":...,"level":"INFO",...}` 한 줄인지 확인.
- `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun` 의 stdout 은 여전히 텍스트.

#### Commit #4 — `refactor: rename LoggingInterceptor methods and unify request log to api_request_completed`

목적: 기존 `LoggingInterceptor` 를 JSON 로그 친화적인 구조로 다듬는다. 클래스명 / 패키지는 그대로 두고 (Java 컴파일 영향 0), 로그 라인의 message 와 MDC 사용만 바꾼다.

변경 파일:

- [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)

내용 (요지):

```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute(START_TIME_ATTR, Instant.now());
    request.setAttribute(CLIENT_IP_ATTR, extractClientIp(request));
    QueryStatsHolder.init();

    // 1. requestId 발급 + MDC 등록
    String requestId = UUID.randomUUID().toString();
    MDC.put("requestId", requestId);
    MDC.put("method", request.getMethod());
    MDC.put("path", request.getRequestURI());

    // 2. 인증된 사용자만 userId 를 MDC 에 등록 (커밋 #4 에서는 placeholder, 본 등록은 커밋 #5)
    // MDC.put("userId", ...);

    // 3. 응답 헤더로 외부에 노출 (디버깅용)
    response.setHeader("X-Request-Id", requestId);

    String traceId = MDC.get("traceId");
    if (traceId != null) {
        response.setHeader("X-Trace-Id", traceId);
    }

    log.info("api_request_started");
    return true;
}

@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    try {
        Instant startTime = (Instant) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) {
            return;
        }

        long durationMs = Duration.between(startTime, Instant.now()).toMillis();
        long queryCount = QueryStatsHolder.getQueryCount();
        long queryTimeMs = QueryStatsHolder.getTotalTimeMs();
        QueryStatsHolder.clear();

        String uriTemplate = (String) request.getAttribute(
            HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        MDC.put("event", "api_request_completed");
        MDC.put("uriTemplate", uriTemplate != null ? uriTemplate : "UNKNOWN");
        MDC.put("statusCode", String.valueOf(response.getStatus()));
        MDC.put("durationMs", String.valueOf(durationMs));
        MDC.put("queryCount", String.valueOf(queryCount));
        MDC.put("queryTimeMs", String.valueOf(queryTimeMs));
        MDC.put("clientIp", (String) request.getAttribute(CLIENT_IP_ATTR));

        if (ex != null) {
            MDC.put("exception", ex.getClass().getSimpleName());
            log.error("api_request_completed", ex);
        } else {
            log.info("api_request_completed");
        }
    } finally {
        MDC.clear();
    }
}
```

주의 / 제거 항목:

- 기존 `log.info("[REQ] 💗 {} {}", method, fullPath)` / `log.info("[RES] {} {} {} {}ms ...", ...)` 의 이모지·문자열 결합은 모두 제거. 사람이 봐서 좋았던 정보는 모두 MDC 필드로 분리되어 JSON 에 포함된다.
- `getStatusEmoji()` 도 제거.
- `MDC.clear()` 는 절대 누락 금지. `try/finally` 강제.
- query string 의 `URLDecoder.decode(...)` 는 본 커밋에서 제거하지 않지만, 디코딩된 query string 을 MDC 에 넣지 않는다 (민감 파라미터가 들어올 수 있음).

검증:

- 단위 테스트 추가: MDC 가 `preHandle` 에서 set, `afterCompletion` finally 에서 clear 되는지.
- 실제 dev 환경 배포 후 stdout JSON 에 `traceId`, `method`, `path`, `uriTemplate`, `statusCode`, `durationMs` 가 들어가는지 grep.

#### Commit #5 — `feat: put memberId into MDC after JWT authentication`

목적: 인증된 사용자의 `memberId` 를 MDC `memberId` 키로 등록한다. 서비스 도메인 명명을 그대로 사용 (`userId` 아님). `JwtAuthenticationFilter` 가 인증을 끝낸 직후가 가장 빠른 시점이지만, Filter 는 Interceptor 보다 앞에서 동작하므로 두 가지 선택이 있다.

선택 A — Filter 에서 등록 (권장):

```java
// JwtAuthenticationFilter.doFilterInternal()
SecurityContextHolder.getContext().setAuthentication(authentication);
MDC.put("userId", String.valueOf(memberId));
```

선택 B — Interceptor 의 preHandle 에서 SecurityContextHolder 조회:

```java
private void putUserIdToMdcIfAuthenticated() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return;
    if (auth.getPrincipal() instanceof MemberPrincipal mp) {
        MDC.put("userId", String.valueOf(mp.getMemberId()));
    }
}
```

본 ADR 은 **선택 B 를 채택**한다. 이유:

- Filter 에서 MDC 를 넣으면, SecurityContext 가 cleared 되는 시점 (보통 Servlet 컨테이너 종료) 과 MDC clear 시점 (Interceptor afterCompletion) 이 어긋날 수 있다.
- Interceptor 는 `try/finally` + `MDC.clear()` 로 lifecycle 이 한 곳에 모인다.
- Filter 는 인증 검사 외의 책임을 갖지 않는 게 더 단순.

변경 파일:

- [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java) — `preHandle` 마지막에 `putUserIdToMdcIfAuthenticated()` 호출.

주의:

- `MemberPrincipal.getMemberId()` 는 Long. MDC 값은 String 이므로 `String.valueOf(...)`.
- 익명 사용자 (`principal == "anonymousUser"`) 의 경우 MDC `memberId` 를 넣지 않는다 (검색 시 `memberId is null` 로 익명 트래픽 식별).

#### Commit #6 — `chore: drop access-log noise from JwtAuthenticationFilter`

목적: JSON 로그가 들어왔으니, 기존 텍스트 시대의 진단용 한 줄 로그 (`log.info("JWT TOKEN Authenticated: memberId={}", memberId)`) 를 제거하거나 DEBUG 로 낮춘다. `api_request_completed` JSON 에 `memberId` 가 자동으로 들어가므로 중복이다.

변경 파일:

- [JwtAuthenticationFilter.java](../../src/main/java/com/umc/product/global/security/JwtAuthenticationFilter.java#L42) — `log.info(...)` 를 `log.debug(...)` 로 변경 또는 삭제.

또한 다른 패키지에서 같은 패턴의 `log.info("userId={}, ...")` 가 있는지 일괄 검토 후 별도 PR 가능. 본 커밋은 가장 시끄러운 한 줄만 처리.

#### Commit #7 — `feat: add ExternalApiCallLogger helper for structured external_api_called events`

목적: GitHub / Apple / Kakao / LLM provider / 이메일 등 외부 API 호출의 성공·실패·duration 을 통일된 JSON 이벤트로 남길 수 있게 hook 을 제공한다. 본 커밋은 helper 만 추가하고, 도메인별 적용 PR 은 분리.

권장 위치:

- `src/main/java/com/umc/product/global/logging/ExternalApiCallLogger.java` (신규)

API 예시:

```java
public final class ExternalApiCallLogger {

    private static final Logger log = LoggerFactory.getLogger("external_api");

    public static <T> T measure(
        String provider,
        String operation,
        Supplier<T> call
    ) {
        long start = System.currentTimeMillis();
        try {
            T result = call.get();
            log.info(
                "external_api_called",
                kv("provider", provider),
                kv("operation", operation),
                kv("result", "SUCCESS"),
                kv("durationMs", System.currentTimeMillis() - start)
            );
            return result;
        } catch (RuntimeException e) {
            log.warn(
                "external_api_called",
                kv("provider", provider),
                kv("operation", operation),
                kv("result", "FAILURE"),
                kv("durationMs", System.currentTimeMillis() - start),
                kv("errorClass", e.getClass().getSimpleName())
            );
            throw e;
        }
    }
}
```

주의:

- `kv` 는 `net.logstash.logback.argument.StructuredArguments.kv` 의 static import.
- 예외 메시지는 그대로 찍지 않는다. 메시지에 OAuth code, accessToken, 사용자 입력이 섞일 수 있다. `errorClass` 만 남기고, 메시지가 꼭 필요한 경우 `errorMessageHash` (SHA256 짧은 prefix) 같은 우회 수단을 검토.

도메인별 적용 PR (분리):

- GitHub PR fetch / repository sync (010 ADR)
- LLM provider 호출 (008 ADR, 012 ADR 의 비동기 경로 포함)
- Apple / Kakao 인증
- 이메일 / Discord webhook

#### Commit #8 — `feat: scrub sensitive headers and bodies from any incidental logging`

목적: 본 ADR §10 의 민감정보 정책을 코드 레벨에서 강제한다.

변경 영역:

- 전역 `RestClient` / `WebClient` 의 request-logging interceptor 가 있는지 점검 ([RestClientConfig.java](../../src/main/java/com/umc/product/global/config/RestClientConfig.java)).
- 있으면 `Authorization`, `Cookie`, `X-Api-Key` 헤더는 `***` 로 마스킹.
- request body / response body 를 통째로 찍는 코드가 있으면 길이 + content-type 요약으로 대체.
- 인증번호 / 이메일 인증 코드 / OAuth authorization code 를 직접 다루는 controller / service 에서 해당 변수를 `log.debug` 에라도 넣고 있는 부분이 있는지 grep 후 제거.

본 커밋은 변경 범위가 도메인 전반에 걸칠 수 있어 단독 PR 로 분리하는 게 안전하다.

검증 방법:

- `git grep -nE 'log\\.(info|debug|warn|error).*(token|code|authorization)' src/main/java` 에서 hit 가 0 인지 확인.
- 단위 테스트: mock interceptor 에 `Authorization: Bearer xxx` 를 흘려보내고, 로그 캡처 결과에 `xxx` 가 포함되지 않는지 assert.

#### Commit #9 — `chore: update Grafana dashboards and LogQL rules to | json parser`

목적: 운영 dashboard / alerting rule 을 `| regexp` 에서 `| json` 으로 마이그레이션. JSON 전환의 효용은 dashboard 가 따라와야 비로소 실현된다.

변경 영역:

- Grafana provisioning (현재 디렉터리 골격만 있음: [docker/monitoring/config/grafana/provisioning/](../../docker/monitoring/config/grafana/provisioning/)) 에 JSON 기반 panel JSON 들을 추가.
- 기존 alerting rule (있다면) 의 `| regexp` 부분을 `| json | durationMs > 1000` 식으로 교체.

운영 LogQL 예시:

```logql
# 1초 이상 느린 요청
{application=~".*umc-product-api"}
| json
| durationMs > 1000

# 특정 uriTemplate 의 P95
quantile_over_time(0.95,
  {application=~".*umc-product-api"}
  | json
  | uriTemplate="/forms/{formId}/answers"
  | unwrap durationMs
  [5m]
)

# 5xx 에러 시계열
sum by (uriTemplate) (
  count_over_time(
    {application=~".*umc-product-api"}
    | json
    | statusCode >= 500
    [5m]
  )
)

# 특정 traceId 의 모든 로그 (응답 헤더 X-Trace-Id 와 동일 값)
{application=~".*umc-product-api"} | json | traceId="9f1a2c7b..."
```

본 커밋은 ADR-014 의 자체 호스팅 이관과 시점이 겹친다. 현재 Grafana Cloud 에 직접 panel 을 만드는 작업 + ADR-014 의 self-hosted Grafana provisioning 디렉터리 모두 같은 LogQL 을 공유한다.

### MDC 키 표준

본 ADR 채택 후, 다음 키 이름을 표준으로 한다. 새 코드는 이 이름만 사용한다.

| 키 | 출처 | 수명 | 용도 |
|---|---|---|---|
| `traceId` | Micrometer Tracing | 요청 1개 | Tempo 분산 추적 + Loki 단일 요청 식별 (응답 헤더 `X-Trace-Id` 로 노출) |
| `spanId` | Micrometer Tracing | span 1개 | Tempo 내부 |
| `memberId` | LoggingInterceptor | 요청 1개 | 인증된 사용자 식별자. 서비스 도메인 명명 (`memberId`) 그대로 사용. 익명 사용자는 비워둔다 |
| `method` | LoggingInterceptor | 요청 1개 | HTTP 메서드 |
| `path` | LoggingInterceptor | 요청 1개 | 실제 URI (예: `/forms/123`) |
| `uriTemplate` | LoggingInterceptor | 응답 시점 | 매칭된 패턴 (예: `/forms/{formId}`) |
| `statusCode` | LoggingInterceptor | 응답 시점 | HTTP 상태 코드 |
| `durationMs` | LoggingInterceptor | 응답 시점 | 처리 시간 |
| `queryCount` | LoggingInterceptor | 응답 시점 | SQL 호출 수 |
| `queryTimeMs` | LoggingInterceptor | 응답 시점 | SQL 누적 시간 |
| `clientIp` | LoggingInterceptor | 응답 시점 | 클라이언트 IP |
| `event` | 로그 작성자 | 로그 1줄 | 이벤트 분류 (e.g. `api_request_completed`, `external_api_called`) |
| `provider`, `operation`, `result`, `retryCount`, `externalStatusCode` | 호출자 | 로그 1줄 | 외부 API 호출 컨텍스트 |

### 비동기 / Scheduler / WebSocket 경로의 MDC

`LoggingInterceptor` 는 HTTP 동기 요청에만 동작한다. 다음 경로는 별도로 다룬다 (본 ADR 의 범위 밖이지만 후속 작업 가이드).

- `@Async` 메서드: Micrometer 의 `context-propagation` 이 이미 의존성에 들어 있으므로 [AsyncConfig.java](../../src/main/java/com/umc/product/global/config/AsyncConfig.java) 의 TaskDecorator 가 MDC 를 자동 전파하도록 설정. 미설정 시 자식 스레드에서 MDC 가 비어있다.
- `@Scheduled` 잡: 스케줄러 시작 시 `MDC.put("event", "scheduled_job_started")` 같은 별도 사이클로 관리. requestId 는 무의미하므로 생성하지 않음.
- STOMP / WebSocket ([ADR-011](011-inquiry-domain-with-websocket-stomp.md)): inbound channel interceptor 에서 별도 MDC 부여. message 단위로 `event=ws_message_received` 같은 이벤트 추가.

### 로컬에서 JSON 출력 확인

로컬에서 운영 포맷을 사람 눈으로 보고 싶다면:

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun | jq .
```

또는 IntelliJ 의 run configuration 에 `Active Profiles=dev` 로 임시 전환.

### 롤백 절차

- 커밋 #3 (root 어펜더 전환) 만 revert 하면 stdout 이 즉시 텍스트로 복귀.
- 커밋 #4 (Interceptor MDC) 를 별도 revert 하면 `api_request_completed` 이벤트가 사라지고 `[REQ]/[RES]` 두 줄로 복귀.
- 커밋 #1 / #2 는 동작 영향이 없어 revert 불필요.

### 적용 후 운영 점검 (배포 +1 주)

- [ ] Loki 에서 `| json` 파싱이 100% 의 라인에 성공하는가? (`json_parse_error` 가 0 인지)
- [ ] `api_request_completed` 의 `durationMs` 가 P50 / P95 / P99 dashboard 에 표시되는가?
- [ ] MDC 누수 점검: 같은 스레드의 연속된 요청에서 `memberId` 가 잘못 섞이는 사례가 없는가? (특정 사용자의 요청에서 다른 `memberId` 가 1% 이상 검출되면 누수 의심)
- [ ] Sentry 의 issue 그루핑이 깨지지 않았는가? (Sentry 는 logger message 를 사용하므로 `api_request_completed` 가 너무 잦으면 group 하나에 몰릴 수 있음 — minimum level 을 다시 조정)
- [ ] 민감 필드 검색: `| json |~ "(?i)(bearer |access[_-]?token|authorization=)"` 가 0 line 인지 정기 점검.

## References

- 관련 ADR
    - [ADR-014: 모니터링 스택을 Grafana Cloud 에서 자체 홈서버로 이관한다](014-self-hosted-monitoring-stack-migration.md) — 본 ADR 의 JSON 로그는 ADR-014 의 self-hosted Loki 이관 시점과 정렬되며, LogQL `| json` 룰을 공유한다.
    - [ADR-013: k6 기반 부하·성능 테스트 도입 전략](013-k6-load-and-performance-testing-strategy.md) — 본 ADR 의 `uriTemplate` / `durationMs` 필드가 k6 결과와 동일 축에서 비교 가능해진다.
    - [ADR-011: 문의 도메인 WebSocket/STOMP 채택](011-inquiry-domain-with-websocket-stomp.md) — Interceptor 외 경로의 MDC 처리 가이드.
    - [ADR-010: GitHub App OAuth 및 Webhook 통합](010-github-app-oauth-and-webhook-integration.md) — `external_api_called` 의 첫 적용 후보.
    - [ADR-008: LLM 도메인 provider 전략](008-llm-domain-provider-strategy.md) — 동일.
- 기존 코드 / 설정
    - [dependencies.gradle.kts (관측 의존성)](../../gradle/dependencies.gradle.kts)
    - [libs.versions.toml (의존성 버전)](../../gradle/libs.versions.toml)
    - [src/main/resources/logback-spring.xml](../../src/main/resources/logback-spring.xml)
    - [src/main/resources/application.yml §252-298 (management / tracing / metrics)](../../src/main/resources/application.yml#L252-L298)
    - [src/main/java/com/umc/product/global/config/LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)
    - [src/main/java/com/umc/product/global/config/WebMvcConfig.java](../../src/main/java/com/umc/product/global/config/WebMvcConfig.java)
    - [src/main/java/com/umc/product/global/security/MemberPrincipal.java](../../src/main/java/com/umc/product/global/security/MemberPrincipal.java)
    - [src/main/java/com/umc/product/global/security/JwtAuthenticationFilter.java](../../src/main/java/com/umc/product/global/security/JwtAuthenticationFilter.java)
- 외부 자료
    - [logstash-logback-encoder README](https://github.com/logfellow/logstash-logback-encoder)
    - [SLF4J MDC documentation](https://www.slf4j.org/manual.html#mdc)
    - [Loki — JSON parser & label_format](https://grafana.com/docs/loki/latest/query/log_queries/#parser-expression)
    - [Spring Framework — HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/HandlerMapping.html#BEST_MATCHING_PATTERN_ATTRIBUTE)
    - [net.logstash.logback.argument.StructuredArguments](https://github.com/logfellow/logstash-logback-encoder#event-specific-custom-fields)
