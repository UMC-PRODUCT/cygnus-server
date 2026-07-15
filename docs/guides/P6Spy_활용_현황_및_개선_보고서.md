# P6Spy 활용 현황 및 개선 보고서

> 작성일: 2026-05-13
> 대상 모듈: `com.umc.product.global.config`
> 관련 ADR: [ADR-016 — JSON 구조화 로깅](../adr/016-structured-json-logging-with-mdc.md)

---

## 1. 요약

현재 본 프로젝트는 `p6spy-spring-boot-starter` 를 통해 두 가지 목적으로 P6Spy 를 사용하고 있어요.

1. **개발자용 SQL 가독화** — Hibernate 의 `format_sql` 대신 P6Spy 로 실제 바인딩된 SQL 을 사람이 읽을 수 있게 출력 (`P6SpyConfig.P6SpyFormatter`).
2. **요청 단위 쿼리 통계 수집** — `JdbcEventListener` 로 query/update/batch 의 실행 횟수·시간을 ThreadLocal 에 누적 (`QueryStatsJdbcEventListener` + `QueryStatsHolder`) → `LoggingInterceptor.afterCompletion` 에서 MDC `queryCount` / `queryTimeMs` 로 출력 (ADR-016 의 `api_request_completed` 이벤트 일부).

기본 구조는 잘 짜여 있지만, 다음 영역에서 **버그 1건 + 운영/관측/보안 측면의 개선 여지 8건**이 확인되었어요. 본 보고서는 그 각각을 P1~P4 우선순위로 분류하고, 조치 방향을 제안합니다.

---

## 2. 현재 활용 현황

### 2.1 의존성

[dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)

```kotlin
// SQL 출력용 P6Spy
add("implementation", "com.github.gavlyukovskiy:p6spy-spring-boot-starter:${version("p6spy-spring-boot-starter")}")
```

`gavlyukovskiy` 스타터는 DataSource 를 자동으로 `P6DataSource` 로 래핑하고, `@Component` 로 등록된 `JdbcEventListener` 빈을 자동 수집해요.

### 2.2 JPA 측 설정

[application.yml:94](../../src/main/resources/application.yml#L94)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        format_sql: true
        show_sql: false  # p6spy가 있어서 꺼둠
```

`show_sql` 은 P6Spy 와 중복되므로 꺼둔 상태. `format_sql=true` 는 Hibernate 가 내부적으로 포맷할 때만 의미가 있고, 현재 stdout 으로 가는 SQL 은 P6Spy 가 직접 포맷하므로 사실상 무용지물에 가까운 옵션이에요(잘못된 건 아니지만 의도가 흐려져 있음).

> 참고: `decorator.datasource.p6spy.*` 네임스페이스의 스타터 전용 설정은 현재 **하나도 잡혀 있지 않음**. 즉 모든 동작이 스타터의 기본값(전 환경에서 모든 SQL 을 INFO 로 stdout) 이에요.

### 2.3 메시지 포맷터

[P6SpyConfig.java](../../src/main/java/com/umc/product/global/config/P6SpyConfig.java)

- `@PostConstruct` 로 `P6SpyOptions.setLogMessageFormat(P6SpyFormatter.class.getName())` 등록.
- `[category] | elapsed ms | sql` 한 줄 포맷.
- `statement` 카테고리에 한해 Hibernate `BasicFormatterImpl` / `DDLFormatterImpl` 로 보기 좋게 포맷.

### 2.4 요청 단위 쿼리 통계

세 클래스가 한 묶음으로 동작해요.

| 파일                                                                                                                     | 역할                                                                                                                                                               |
|------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [QueryStatsHolder.java](../../src/main/java/com/umc/product/global/config/QueryStatsHolder.java)                       | `ThreadLocal<long[]>` 에 [count, totalMs] 누적. `init` / `record` / `clear` API 제공                                                                                  |
| [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java) | `JdbcEventListener` 구현. `onAfterExecuteQuery`, `onAfterExecuteUpdate`, `onAfterExecuteBatch` 에서 `QueryStatsHolder.record(elapsed)` 호출. `SQLException` 발생 시 기록 제외 |
| [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)                   | `preHandle` 에서 `QueryStatsHolder.init()`, `afterCompletion` 의 `finally` 에서 MDC 에 `queryCount` / `queryTimeMs` 를 push 한 뒤 `QueryStatsHolder.clear()`              |

### 2.5 출력 경로 — ADR-016 과의 결합

- 모든 P6Spy 로그 라인은 logback root 의 `INFO` 임계값을 통해 흘러나가요.
- `local` → `LOCAL_CONSOLE` 어펜더 (텍스트, 컬러). 사람이 IntelliJ 에서 바로 확인.
- `dev` / `staging` / `prod` → `CONSOLE_JSON` 어펜더 (한 줄 JSON) + `LOKI`. 즉 운영 환경에서도 모든 SQL 한 줄이 그대로 JSON 로그 1줄로 변환되어 Loki 까지 전송되고 있어요.
- `LoggingInterceptor` 가 만들어낸 `event=api_request_completed` 이벤트의 `queryCount`/`queryTimeMs` 필드는 LogQL `| json | queryCount > 30` 형태로 검색 가능.

---

## 3. 문제점 및 개선 제안

### P1 — 포맷터의 이중 호출 (잠재 버그)

[P6SpyConfig.java:24-26](../../src/main/java/com/umc/product/global/config/P6SpyConfig.java#L24-L26)

```java
sql =

formatSql(category, sql);
return String.

format("[%s] | %d ms | %s",category, elapsed, formatSql(category, sql));
```

`formatSql` 을 두 번 호출하고 있어요. `category=="statement"` 인 경우 이미 포맷된 SQL 을 Hibernate `BasicFormatterImpl` 로 한 번 더 통과시키게 되는데, Hibernate 포맷터는 멱등하지 않아 줄바꿈/들여쓰기가 한 차례 더 들어가거나 token 이 깨질 수 있어요. statement 외 카테고리는 무영향이지만 의도된 흐름이 아니에요.

**조치 방향:**

- 한 줄로 정리.

```java
String formatted = formatSql(category, sql);
return String.

format("[%s] | %d ms | %s",category, elapsed, formatted);
```

`elapsed` 0 인 라인이 굉장히 많이 나오는 일도 자주 있는데, P6SpyFormatter 가 호출 비용에 직접 영향을 주므로 이중 호출 제거 자체로 약간의 성능 이득도 있어요.

---

### P1 — 운영 환경에서 모든 SQL 이 stdout/Loki 로 흘러나간다 (관측·비용·민감정보)

`decorator.datasource.p6spy.enable-logging` / `log-filter.pattern` 등 환경별 출력 제어가 비어 있어요. 결과적으로 `prod` 에서도 **모든 SELECT/INSERT/UPDATE 한 줄이 JSON 로그**로 변환되어 Loki 에 적재되고 있어요. 다음 세 가지 비용이 누적돼요.

1. **저장 비용**: ADR-016 §Consequences 에서 짚었듯 텍스트 한 줄 200–300B 가 JSON 으로 늘면 600–900B. 거기에 SQL 본문까지 들어가니 한 라인이 1–3KB 수준. 한 요청당 쿼리 10–30 개 × P95 트래픽을 곱하면 Loki ingestion 의 가장 큰 단일 source 가 P6Spy 일 가능성이 높아요.
2. **검색 노이즈**: `application=~".*umc-product-api"` 로 검색했을 때 `event=api_request_completed` 1줄을 찾는 데 그 앞뒤 N개의 SQL 로그가 결과를 가려요.
3. **민감정보 노출**: `QueryStatsJdbcEventListener.onAfterExecuteUpdate` 는 `info.getSqlWithValues()` 를 그대로 `log.debug` 로 찍어요(아래 P2 항목). `prod` 의 `LOGGING_APP_LEVEL` 이 DEBUG 로 올라가는 사고가 한 번이라도 나면 `INSERT INTO member (email, password_hash, ...)` 류 SQL 의 실제 값이 그대로 운영 로그에 흘러나가요. P6Spy 자체의 `statement` 카테고리도 prepared SQL 의 ?를 값으로 치환한 형태로 출력하기 때문에 동일한 위험이 있어요(ADR-016 §10 정책 위배).

**조치 방향 — 환경별 설정 분리:**

`application.yml` 에 다음을 추가하고, 환경별 override 를 두는 것을 권장해요.

```yaml
decorator:
  datasource:
    p6spy:
      enable-logging: true            # local/dev: true, prod: false (또는 slow query only)
      logging: slf4j
      log-format: "[%(category)] | %(executionTime) ms | %(sql)"
      multiline: true                 # 가독성 (local 만)
      tracing:
        include-parameter-values: true  # local: true / dev,staging,prod: false (★ 핵심)
      # 슬로우 쿼리 임계값. 0 미만은 비활성, 200ms 이상만 로깅하려면 200.
      log-filter:
        pattern: "^(?!.*pg_catalog).*$"  # Flyway/health-check 노이즈 제거 패턴
```

`prod` 프로필 (또는 환경변수) 에서는 다음을 강제하는 것을 권장해요.

- `decorator.datasource.p6spy.enable-logging: false` — 라인 단위 SQL 로깅 끔. 통계 수집(`JdbcEventListener`)은 그대로 동작.
- 만약 슬로우 쿼리만 보고 싶다면 `tracing.slow-query.enable-logging: true` + threshold ms 를 따로 잡거나, 자체 `JdbcEventListener` 안에서 `elapsed > 200ms` 일 때만 WARN 로그를 남기는 방식.

이 한 가지 변경만으로도 Loki ingestion 의 절반 이상을 줄일 수 있을 가능성이 높아요. ADR-014(자체 호스팅 모니터링 이관)의 retention 비용에도 직접 영향이 있어요.

---

### P2 — `QueryStatsJdbcEventListener` 가 SQL with values 를 그대로 로깅

[QueryStatsJdbcEventListener.java:34-50](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java#L34-L50)

```java
log.debug(
    "[executeUpdate] sql={}, elapsed={} ms",
    info.getSqlWithValues(),timeElapsedNanos /1_000_000.0
    );
```

`getSqlWithValues()` 는 `?` 가 치환된 실제 SQL 이에요. 의도는 디버깅 보조겠지만, 다음 두 가지 이유로 위험 부담이 더 커요.

1. **이중 로깅**: P6Spy 가 이미 같은 SQL 을 `[statement]` 카테고리로 한 줄 찍고 있어요. 이 라인은 그것의 중복이고 카테고리/포맷도 다르기 때문에 운영 분석을 더 어렵게 만들어요.
2. **민감정보**: 위 P1 항목과 동일. `dev` 의 `com.umc.product=DEBUG` 설정에서는 이 라인이 매번 출력되고 있어요. 인증/이메일 인증/회원가입 트래픽 디버깅 중에 토큰/메일/해시가 화면에 흘러나갈 위험.

**조치 방향:**

- `log.debug(...)` 호출 자체를 제거. 통계 수집(`record(...)`)만 남기기.
- 슬로우 쿼리 표시가 필요하면 별도 임계값 비교 후 WARN 으로 분리 (다음 항목 참고).

---

### P2 — 슬로우 쿼리 자동 표시 / 알람 부재

현재 `QueryStatsJdbcEventListener` 는 모든 쿼리를 동일한 가중치로 누적해요. 시간 임계값을 넘긴 개별 쿼리를 운영자가 인지할 수 있는 1차 신호가 없어요. ADR-013(k6 부하 테스트) 결과를 응답시간 P95/P99 로 분석할 때 "어느 SQL 이 느렸나"가 자동으로 떨어지면 분석 비용이 크게 줄어요.

**조치 방향:**

`JdbcEventListener` 안에 임계값 비교를 추가.

```java
private static final long SLOW_QUERY_THRESHOLD_NANOS = 200_000_000L; // 200ms

@Override
public void onAfterExecuteQuery(PreparedStatementInformation info, long timeElapsedNanos, SQLException e) {
    record(timeElapsedNanos, e);
    if (e == null && timeElapsedNanos > SLOW_QUERY_THRESHOLD_NANOS) {
        log.warn("slow_query",
            kv("event", "slow_query"),
            kv("elapsedMs", timeElapsedNanos / 1_000_000L),
            kv("sql", sanitize(info.getSql())) // 파라미터 미포함 prepared SQL
        );
    }
}
```

- ADR-016 §MDC 키 표준에 맞춰 `event=slow_query` 로 분류.
- `sanitize` 는 prepared SQL 형태(`SELECT * FROM member WHERE id = ?`) 만 남기고 파라미터 값은 제외. `info.getSql()` 이 prepared, `info.getSqlWithValues()` 가 bound — 후자는 절대 사용 금지.
- 임계값은 환경변수 (`P6SPY_SLOW_QUERY_THRESHOLD_MS`) 로 분리해 운영 중 조정 가능하게.

---

### P2 — Micrometer 메트릭 미발행 (대시보드 1급 객체 부재)

현재 `QueryStatsHolder` 가 모은 정보는 **요청 1회짜리** 로그 1줄 뿐이에요. 시계열 집계는 LogQL 로 `| json | unwrap queryCount` 같은 비싼 경로를 거쳐야 가능해요. Prometheus 의 1급 메트릭으로 발행하면 P95/P99/heatmap 을 사실상 무료로 얻을 수 있어요.

**조치 방향:**

`JdbcEventListener` 에 Micrometer `Timer` / `DistributionSummary` 를 주입해 누적 발행.

```java
@Component
public class QueryMetricsJdbcEventListener extends JdbcEventListener {

    private final Timer queryTimer;
    private final Counter slowQueryCounter;

    public QueryMetricsJdbcEventListener(MeterRegistry registry) {
        this.queryTimer = Timer.builder("jdbc.query.duration")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
        this.slowQueryCounter = Counter.builder("jdbc.query.slow.total")
            .register(registry);
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation info, long timeElapsedNanos, SQLException e) {
        queryTimer.record(timeElapsedNanos, TimeUnit.NANOSECONDS);
        if (timeElapsedNanos > SLOW_QUERY_THRESHOLD_NANOS) slowQueryCounter.increment();
    }
}
```

기대 효과:

- Grafana 패널에 `histogram_quantile(0.95, sum(rate(jdbc_query_duration_seconds_bucket[5m])) by (le))` 가 바로 들어감.
- 기존 `QueryStatsJdbcEventListener` 는 요청 단위 ThreadLocal 누적만 담당하도록 책임을 좁히면 SRP 도 개선돼요(두 리스너로 분리하거나, 같은 클래스에서 두 책임을 명확히 구분).

---

### P2 — `LoggingInterceptor` 에서 쿼리 카운트 임계 초과 시 WARN 표식 없음

[LoggingInterceptor.java:114-141](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java#L114-L141)

현재 `api_request_completed` 이벤트는 `queryCount` 가 1이든 100이든 동일하게 `INFO` 로 찍혀요. N+1 트래픽이 운영에서 발생해도 alerting 룰이 별도로 잡혀 있지 않으면 노이즈에 묻혀요.

**조치 방향:**

`afterCompletion` 안에서 임계값을 넘기면 WARN 으로 승격, 별도 이벤트로 분류.

```java
if(queryCount >30||queryTimeMs >500){
    MDC.

put("event","api_request_heavy_db");
    log.

warn(EVENT_REQUEST_COMPLETED);
}else if(ex !=null){
    ...
    }
```

LogQL 에서 `event=api_request_heavy_db` 한 줄이면 N+1 후보 엔드포인트 리스트가 나와요. CQRS 정책상 query service 가 read-only 인 점을 감안하면, 같은 endpoint 가 반복적으로 heavy_db 를 찍는 경우 fetch join 누락 신호로 보면 됩니다.

---

### P3 — Flyway / Hibernate validate / health-check 노이즈

부팅 시 Flyway 마이그레이션 SQL, `ddl-auto: validate` 가 발행하는 메타데이터 쿼리, Hikari keepalive 의 `SELECT 1` 등이 P6Spy 출력에 그대로 흘러나오고 있어요. 부팅 직후 로그가 수백 줄로 부풀어 첫 요청 분석을 방해해요.

**조치 방향:**

위 P1 항목의 `log-filter.pattern` 에 다음 패턴을 포함시키거나, `JdbcEventListener` 단에서 카테고리 + SQL prefix 로 필터링.

- `^select 1$` (Hikari keepalive)
- Flyway 의 `flyway_schema_history` 접근 SQL
- `pg_catalog.*` 메타데이터 조회

---

### P3 — 테스트 실행 시 P6Spy 가 동작해 빌드 로그가 비대해짐

Testcontainers 기반 통합 테스트에서 `gavlyukovskiy` 스타터가 그대로 활성화되어, `./gradlew test` 출력에 SQL 이 수천 줄 찍혀요. 테스트 실패 원인을 찾기 어렵게 만들고, CI 로그 retention 비용도 증가.

**조치 방향:**

`src/test/resources/application.yml` (또는 `application-test.yml`) 에 다음을 추가.

```yaml
decorator:
  datasource:
    p6spy:
      enable-logging: false
```

또는 Hibernate 의 statistics 만 활성화하는 별도 test profile 로 분리.

---

### P3 — 실패한 쿼리는 통계에서 빠진다

[QueryStatsJdbcEventListener.java:56-60](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java#L56-L60)

```java
private void record(long timeElapsedNanos, SQLException e) {
    if (e == null) {
        QueryStatsHolder.record(timeElapsedNanos);
    }
}
```

`SQLException` 이 발생한 쿼리는 누적되지 않아요. 의도가 "성공한 쿼리만 카운트" 였다면 OK 지만, 실제로는 실패 쿼리도 응답 지연에 기여하므로 `queryTimeMs` 가 실제 응답시간보다 작아 보일 수 있어요.

**조치 방향(택1):**

- 실패도 시간만 기록하되, 별도 `queryErrorCount` 필드를 둬서 LogQL 에서 분리 검색 가능하게.
- 또는 그대로 두되, 정책을 [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java) 의 Javadoc 에 명시. (현재는 어디에도 적혀 있지 않아서 향후 디버깅 시 혼선 소지가 있어요.)

---

### P3 — `application.yml` 의 `format_sql: true` 사실상 무효

[application.yml:92](../../src/main/resources/application.yml#L92)

`org.hibernate.SQL: WARN` 으로 잡혀 있고 Hibernate 의 SQL 출력 자체가 꺼져 있으므로 `format_sql` 은 적용 대상이 없어요. P6Spy 가 자체적으로 Hibernate 의 `BasicFormatterImpl` 을 호출하기는 하지만 이건 `format_sql` 설정과 무관해요.

**조치 방향:**

- 의도가 "보기 좋게 출력" 이면 `format_sql` 은 제거하고 (`P6SpyConfig` 가 이미 같은 일을 함), 주석으로 "포맷은 P6SpyConfig 가 담당" 을 명시.

---

### P4 — `QueryStatsJdbcEventListener` 안의 주석 처리된 `log.trace`

[QueryStatsJdbcEventListener.java:21-25](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java#L21-L25) 의 주석 처리된 로그는 정리하는 것이 좋아요. 코드의 의도가 트랜잭션 별 통계 수집인지 라인별 로깅인지 모호해져요.

---

## 4. 권장 액션 묶음

가장 효과 대비 변경 비용이 작은 묶음 순으로 정리.

| 묶음                                    | 변경 파일                                                           | 효과                                                                                          |
|---------------------------------------|-----------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| **A. 빠른 위생 작업** (1 시간)                | `P6SpyConfig`, `QueryStatsJdbcEventListener`, `application.yml` | P1 버그 1건 + P3 정리 3건. `formatSql` 이중 호출 제거, `SqlWithValues` log.debug 제거, 주석/`format_sql` 정리 |
| **B. 환경별 P6Spy 출력 제어** (2 시간)         | `application.yml` + `application-<profile>.yml` (또는 환경변수)       | 운영 Loki ingestion / 비용 / 민감정보 위험을 한 번에 해결. ADR-014 비용 정책과 정합                                |
| **C. 슬로우 쿼리 + heavy_db 이벤트** (3–4 시간) | `QueryStatsJdbcEventListener`, `LoggingInterceptor`             | N+1 / 슬로우 쿼리 발견을 LogQL 한 줄로 표면화                                                             |
| **D. Micrometer 메트릭화** (4–6 시간)       | 새 `QueryMetricsJdbcEventListener` + Grafana 패널 1개               | P95/P99 SQL 시간을 Prometheus 1급 메트릭으로. ADR-013 부하 테스트 분석 자동화                                  |

A/B 는 동일 PR 로 묶어도 안전해요. C/D 는 ADR 한 줄로도 가치 정리가 가능하므로 별도 PR (또는 `feat: structured slow_query event` / `feat: jdbc query metrics`) 로 분리하는 것을 권장해요.

---

## 5. 부록 — 권장 설정 예시 (참고용)

`application.yml` (공통):

```yaml
decorator:
  datasource:
    p6spy:
      enable-logging: ${P6SPY_ENABLE_LOGGING:false}
      logging: slf4j
      multiline: false
      log-format: "[%(category)] | %(executionTime) ms | %(sql)"
      tracing:
        include-parameter-values: ${P6SPY_INCLUDE_PARAMS:false}
```

환경별 권장값:

| Profile   | `P6SPY_ENABLE_LOGGING` | `P6SPY_INCLUDE_PARAMS` |
|-----------|------------------------|------------------------|
| `local`   | `true`                 | `true` (로컬 디버깅 편의)     |
| `test`    | `false`                | `false`                |
| `dev`     | `true`                 | `false`                |
| `staging` | `false` (슬로우만)         | `false`                |
| `prod`    | `false` (슬로우만)         | `false`                |

슬로우 쿼리는 **로깅 옵션이 아니라 `JdbcEventListener` 에서 임계값 기반 분기**로 처리하는 것을 권장해요. 그쪽이 MDC traceId / event 분류와 자연스럽게 결합되고, ADR-016 의 `event=slow_query` 라인 한 줄로 표준화돼요.

---

## 6. 참고 자료

- [ADR-016 — 구조화 JSON 로깅 (MDC 키 표준)](../adr/016-structured-json-logging-with-mdc.md)
- [ADR-014 — 자체 호스팅 모니터링 스택 이관](../adr/014-self-hosted-monitoring-stack-migration.md)
- [ADR-013 — k6 기반 부하·성능 테스트](../adr/013-k6-load-and-performance-testing-strategy.md)
- [dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)
- [P6SpyConfig.java](../../src/main/java/com/umc/product/global/config/P6SpyConfig.java)
- [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java)
- [QueryStatsHolder.java](../../src/main/java/com/umc/product/global/config/QueryStatsHolder.java)
- [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)
- [gavlyukovskiy/spring-boot-data-source-decorator README](https://github.com/gavlyukovskiy/spring-boot-data-source-decorator)
