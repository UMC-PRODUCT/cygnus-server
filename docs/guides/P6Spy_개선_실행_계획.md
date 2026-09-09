# P6Spy 개선 실행 계획 (Commit-by-commit)

> 작성일: 2026-05-13
> 관련 ADR: [ADR-016 — JSON 구조화 로깅](../adr/016-structured-json-logging-with-mdc.md), [ADR-014 — 자체 호스팅 모니터링 이관](../adr/014-self-hosted-monitoring-stack-migration.md), [ADR-013 — k6 부하 테스트](../adr/013-k6-load-and-performance-testing-strategy.md)

---

## 0. 원칙

- **각 커밋은 독립적으로 빌드 가능**해야 해요. `./gradlew build` 가 통과하지 않으면 커밋을 쪼개거나 합치세요.
- **각 커밋은 운영에 배포되어도 안전한 상태**여야 해요. "다음 커밋이 와야 의미가 있다"는 식의 in-flight 상태를 만들지 마세요.
- 커밋 메시지는 Conventional Commits — `feat:`, `fix:`, `refactor:`, `chore:`, `docs:`, `test:`.
- AI 작성자/Co-authored-by 트레일러 금지 (CLAUDE.md §6).
- 본 계획은 **3개 PR · 총 12 커밋**으로 쪼개요.
    - PR-1: 위생 + 환경별 출력 제어 (5 커밋)
    - PR-2: 슬로우 쿼리 + N+1 표면화 (4 커밋)
    - PR-3: Micrometer 메트릭화 + Grafana 패널 (3 커밋)

---

## PR-1 — 위생 + 환경별 P6Spy 출력 제어

목표:

- **P1 버그 / P1 운영비용·민감정보 / P2 SqlWithValues / P3 정리 3건**을 한 PR 에 묶어 정리.
- 운영 환경에서 라인 단위 SQL 로깅을 **즉시 끄고**, 통계 수집(JdbcEventListener) 만 남기는 상태로 만든다.
- 슬로우 쿼리 이벤트(PR-2) 가 들어오기 전에 "라인 로깅이 꺼져도 가시성이 사라지지 않는다"는 안전선을 먼저 깔아둔다 (= `api_request_completed` 의 `queryCount` / `queryTimeMs` 가 이미 동작 중이므로 이 PR 만 머지해도 안전).

### Commit #1 — `fix: remove duplicate formatSql call in P6SpyFormatter`

목적: P1 버그 해결. statement 카테고리에서 Hibernate 포맷터가 두 번 적용되는 잠재 버그 제거.

변경 파일:

- [P6SpyConfig.java](../../src/main/java/com/umc/product/global/config/P6SpyConfig.java)

변경 내용:

```java
@Override
public String formatMessage(int connectionId, String now, long elapsed, String category,
                            String prepared, String sql, String url) {
    String formatted = formatSql(category, sql);
    // [카테고리] | 실행시간 ms | SQL
    return String.format("[%s] | %d ms | %s", category, elapsed, formatted);
}
```

검증:

- 단위 테스트 추가 (`P6SpyConfigTest`):
    - `category="statement"` + `SELECT * FROM member` → 한 차례만 포맷된 결과인지 (개행/들여쓰기 개수 검증).
    - `category="commit"` → 변환 없이 그대로 반환되는지.
- `./gradlew test` 통과.

### Commit #2 — `refactor: drop sqlWithValues debug logging from QueryStatsJdbcEventListener`

목적: P2 — `info.getSqlWithValues()` 를 `log.debug` 로 찍는 라인 제거. 통계 수집 책임만 남긴다. 민감정보 노출 위험 차단 + P6Spy 라인과의 이중 로깅 제거.

변경 파일:

- [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java)

변경 내용:

- `onAfterExecuteQuery` 의 commented-out `log.trace` 블록 삭제 (P4 정리).
- `onAfterExecuteUpdate` / `onAfterExecuteBatch` 의 `log.debug("[executeUpdate] sql={}, elapsed={} ms", info.getSqlWithValues(), ...)` 호출 삭제.
- 세 메서드는 `record(timeElapsedNanos, e)` 단일 호출만 남도록 정리.
- 클래스 Javadoc 에 "성공한 쿼리만 통계에 누적 (실패는 별도 추적 없음). 자세한 정책은 §부록 참조" 명시.
- `@Slf4j` 어노테이션은 더 이상 사용 안 함 → 제거.

검증:

- 통합 테스트(또는 수동 로컬 부팅): `dev` 프로필에서 `[executeUpdate]` / `[executeBatch]` 가 stdout 에 나오지 않는지 확인.
- `LoggingInterceptor` 의 `queryCount` / `queryTimeMs` 가 여전히 정상 집계되는지 (기존 인터셉터 테스트 통과).

### Commit #3 — `chore: drop dead hibernate.format_sql property`

목적: P3 — P6Spy 가 포맷을 담당하므로 Hibernate `format_sql=true` 는 무영향. 의도 명료화.

변경 파일:

- [application.yml](../../src/main/resources/application.yml#L92)

변경 내용:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_schema: public
        # SQL 출력 포맷은 P6SpyConfig 가 담당하므로 hibernate.format_sql 은 제거.
        # show_sql 도 P6Spy 출력과의 중복을 막기 위해 끔.
        show_sql: false
```

검증:

- 부팅 후 첫 SQL 출력이 P6Spy 포맷터의 한 줄 (`[statement] | N ms | ...`) 그대로인지 확인.

### Commit #4 — `feat: expose p6spy decorator config via env vars`

목적: P1 — 환경별 출력 제어가 가능하도록 `decorator.datasource.p6spy.*` 키를 `application.yml` 에 노출. **본 커밋은 기본값을 현재 동작과 동일**하게 두어 운영 영향을 0 으로 만든다 (실제 전환은 Commit #5).

변경 파일:

- [application.yml](../../src/main/resources/application.yml)

변경 내용 (datasource 블록 아래에 추가):

```yaml
decorator:
  datasource:
    p6spy:
      # 라인 단위 SQL 로깅. local/dev 는 true, staging/prod 는 false 권장.
      enable-logging: ${P6SPY_ENABLE_LOGGING:true}
      logging: slf4j
      multiline: false
      tracing:
        # ★ prepared statement 파라미터 값을 SQL 라인에 풀어쓸지.
        # local 만 true. dev 이상은 false (민감정보 노출 방지).
        include-parameter-values: ${P6SPY_INCLUDE_PARAMS:true}
```

`P6SPY_ENABLE_LOGGING` / `P6SPY_INCLUDE_PARAMS` 두 환경변수를 새로 도입.

**GitHub Actions / 배포 환경변수 동기화**: CLAUDE.md §3 "환경변수 변경 시에는 반드시 GitHub Actions 에 업데이트할 것" 정책에 따라 이 커밋의 PR description 에 다음을 명시.

| 환경      | `P6SPY_ENABLE_LOGGING` | `P6SPY_INCLUDE_PARAMS` |
|---------|------------------------|------------------------|
| local   | `true` (default)       | `true` (default)       |
| test    | `false`                | `false`                |
| dev     | `true`                 | `false`                |
| staging | `false`                | `false`                |
| prod    | `false`                | `false`                |

> 본 커밋에서는 **default 값을 변경하지 않는다**. 즉 환경변수 미주입 시 동작은 현재와 동일. 환경변수 주입은 Commit #5 의 배포에 맞춰 일괄 진행한다.

검증:

- `./gradlew bootRun` (local, 환경변수 미주입) → 현재와 동일한 P6Spy 출력.
- `P6SPY_ENABLE_LOGGING=false ./gradlew bootRun` → SQL 라인이 안 보이고 `api_request_completed` 의 `queryCount` 는 그대로 집계.

### Commit #5 — `chore: silence p6spy line logging in test profile`

목적: P3 — Testcontainers 통합 테스트 출력에서 SQL flood 제거. CI 로그 검색성 개선.

변경 파일:

- 새 파일 `src/test/resources/application-test.yml` (또는 기존 test profile 파일이 있다면 그 곳).

변경 내용:

```yaml
decorator:
  datasource:
    p6spy:
      enable-logging: false
      tracing:
        include-parameter-values: false
```

검증:

- `./gradlew test` 실행 후 console 출력에 `[statement] | ... | SELECT ...` 라인이 나타나지 않음.
- 통계 수집은 production code 와 동일하게 동작하므로 기존 테스트는 그대로 통과.

### PR-1 머지 후 배포 체크리스트

- [ ] `prod` / `staging` / `dev` 의 환경변수에 위 표 적용 (운영 작업).
- [ ] 배포 +1 시간 후 Loki ingestion bytes/sec 가 의미 있게 떨어지는지 확인.
- [ ] `event=api_request_completed` 의 `queryCount` / `queryTimeMs` 가 변함없이 들어오는지 확인.
- [ ] 로컬 개발자에게 "이제 SQL 라인은 local 만 보인다, dev 부팅 시 안 보이는 게 정상" 공지.

---

## PR-2 — 슬로우 쿼리 표면화 + 요청 단위 N+1 신호

목표:

- P2 — 슬로우 쿼리 자동 표시, P2 — 쿼리 카운트 임계 초과 시 WARN 승격.
- PR-1 에서 라인 SQL 로깅을 껐으므로, 이제 슬로우 쿼리만 별도 구조화 이벤트로 표면화해서 운영 가시성을 다시 확보한다.
- ADR-016 의 MDC 키 표준 + event 분류 규약을 그대로 따른다 (`event=slow_query`, `event=api_request_heavy_db`).

### Commit #6 — `feat: emit structured slow_query event from JdbcEventListener`

목적: 일정 ms 를 넘긴 쿼리 한 줄을 `event=slow_query` JSON 로그로 발행.

변경 파일:

- [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java)

변경 내용 (요지):

```java
@Slf4j
@Component
public class QueryStatsJdbcEventListener extends JdbcEventListener {

    private final long slowQueryThresholdMs;

    public QueryStatsJdbcEventListener(
        @Value("${app.observability.slow-query-threshold-ms:200}") long slowQueryThresholdMs
    ) {
        this.slowQueryThresholdMs = slowQueryThresholdMs;
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation info, long timeElapsedNanos, SQLException e) {
        record(timeElapsedNanos, e);
        logIfSlow(info, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation info, long timeElapsedNanos, int rowCount, SQLException e) {
        record(timeElapsedNanos, e);
        logIfSlow(info, timeElapsedNanos, e);
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation info, long timeElapsedNanos, int[] updateCounts, SQLException e) {
        record(timeElapsedNanos, e);
        // batch 는 prepared SQL 만 (값 미포함)
        logIfSlow(info.getSql(), timeElapsedNanos, e, updateCounts != null ? updateCounts.length : 0);
    }

    private void logIfSlow(StatementInformation info, long elapsedNanos, SQLException e) {
        if (e != null) return;
        long elapsedMs = elapsedNanos / 1_000_000L;
        if (elapsedMs < slowQueryThresholdMs) return;

        log.warn("slow_query",
            kv("event", "slow_query"),
            kv("elapsedMs", elapsedMs),
            kv("sql", info.getSql()) // ★ getSql() — prepared, 파라미터 미포함
        );
    }
}
```

> 주의: `info.getSql()` 는 `?` 가 그대로 남은 prepared SQL. **절대 `getSqlWithValues()` 를 쓰지 않는다** (민감정보 정책).

`application.yml` 에 임계값 추가:

```yaml
app:
  observability:
    slow-query-threshold-ms: ${SLOW_QUERY_THRESHOLD_MS:200}
```

검증:

- 단위 테스트: 임계값보다 짧은 elapsed → WARN 미발생. 긴 경우 1회만 발생.
- 통합 테스트(또는 수동): `pg_sleep(0.3)` 로 일부러 느린 쿼리를 던지면 `event=slow_query` 라인이 한 줄 떨어지는지.

### Commit #7 — `feat: escalate api_request_completed to api_request_heavy_db on threshold`

목적: 요청 1건 안에서 N+1 의심 트래픽을 자동 WARN 으로 승격.

변경 파일:

- [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)

변경 내용 (요지):

```java
private static final long HEAVY_DB_QUERY_COUNT = 30L;
private static final long HEAVY_DB_QUERY_TIME_MS = 500L;

// afterCompletion 안:
boolean heavyDb = queryCount > HEAVY_DB_QUERY_COUNT || queryTimeMs > HEAVY_DB_QUERY_TIME_MS;

if(ex !=null){
    MDC.

put(MDC_EVENT, EVENT_REQUEST_COMPLETED);
    MDC.

put(MDC_EXCEPTION, ex.getClass().

getSimpleName());
    log.

error(EVENT_REQUEST_COMPLETED, ex);
}else if(heavyDb){
    MDC.

put(MDC_EVENT, "api_request_heavy_db");
    log.

warn("api_request_heavy_db");
}else{
    MDC.

put(MDC_EVENT, EVENT_REQUEST_COMPLETED);
    log.

info(EVENT_REQUEST_COMPLETED);
}
```

임계값은 일단 상수로 두고, 운영 1주 데이터를 본 뒤 환경변수화 여부를 별도 PR 에서 결정.

검증:

- 단위 테스트: `queryCount=31` / `queryTimeMs=501` 시 WARN + `event=api_request_heavy_db`.
- 정상 요청은 기존과 동일하게 INFO + `event=api_request_completed`.

### Commit #8 — `docs: document slow_query and api_request_heavy_db events in ADR-016`

목적: 새 이벤트 두 개를 ADR-016 §MDC 키 표준 / event 표 에 추가. 향후 LogQL 룰 작성자가 검색 가능.

변경 파일:

- [docs/adr/016-structured-json-logging-with-mdc.md](../adr/016-structured-json-logging-with-mdc.md)

변경 내용:

- MDC 키 표준 표에 `elapsedMs`, `sql` 행 추가 (출처: P6Spy JdbcEventListener).
- "이벤트 명세" 절을 새로 추가하거나 기존 §Implementation Notes 에 다음 두 이벤트 명세 삽입.

```text
event=slow_query              — 단일 SQL 이 threshold 를 넘었을 때 WARN. 필드: elapsedMs, sql
event=api_request_heavy_db    — 요청 1건의 queryCount / queryTimeMs 가 임계를 넘었을 때 WARN.
```

### Commit #9 — `chore: provision LogQL rule for slow_query / heavy_db dashboards`

목적: PR-2 의 이벤트가 Grafana 에서 즉시 검색 가능하도록 provisioning 파일에 LogQL 패널 1개 추가.

변경 파일:

- `docker/monitoring/config/grafana/provisioning/dashboards/jdbc-slow-queries.json` (신규)

변경 내용: 다음 패널 2개 포함하는 dashboard JSON.

```logql
# 1) slow_query 발생 빈도 (5분 윈도우, sql 별)
sum by (sql) (
  count_over_time(
    {application=~".*umc-product-api"} | json | event="slow_query" [5m]
  )
)

# 2) heavy_db 의심 endpoint (uriTemplate 별)
topk(10,
  sum by (uriTemplate) (
    count_over_time(
      {application=~".*umc-product-api"} | json | event="api_request_heavy_db" [1h]
    )
  )
)
```

검증:

- `docker compose -f docker/monitoring/compose.yml up` 후 Grafana UI 에서 dashboard 가 자동 로드되는지.

### PR-2 머지 후 운영 점검

- [ ] 배포 +1 일 후, `event=slow_query` 의 `sql` cardinality 가 너무 높지 않은지 (한 패턴이 N+1 로 인해 수천 건이면 별도 dedup 필요).
- [ ] `event=api_request_heavy_db` 가 어떤 `uriTemplate` 에 집중되는지 → 후속 N+1 / fetch join 작업의 우선순위 입력.

---

## PR-3 — Micrometer 메트릭화 + 책임 분리

목표:

- P2 — JDBC 쿼리 시간을 Prometheus 1급 메트릭으로 노출. LogQL `| unwrap` 의존 제거.
- ADR-013 의 k6 부하 결과를 P95/P99 메트릭으로 자동 비교 가능하게.
- `QueryStatsJdbcEventListener` 의 책임을 "요청 단위 ThreadLocal 누적" 으로 좁히고, 메트릭 발행은 별도 리스너로 분리.

### Commit #10 — `feat: publish jdbc query duration to Micrometer`

목적: 모든 SQL 의 elapsed 를 `jdbc.query.duration` Timer 로 발행.

변경 파일:

- 신규 `src/main/java/com/umc/product/global/observability/QueryMetricsJdbcEventListener.java`

변경 내용 (요지):

```java
@Component
public class QueryMetricsJdbcEventListener extends JdbcEventListener {

    private final Timer queryTimer;
    private final Counter slowQueryCounter;
    private final long slowQueryThresholdNanos;

    public QueryMetricsJdbcEventListener(
        MeterRegistry registry,
        @Value("${app.observability.slow-query-threshold-ms:200}") long slowQueryThresholdMs
    ) {
        this.queryTimer = Timer.builder("jdbc.query.duration")
            .description("P6Spy 로 측정한 단일 JDBC 호출 시간")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(registry);
        this.slowQueryCounter = Counter.builder("jdbc.query.slow.total")
            .description("slow_query 임계값을 넘긴 SQL 호출 누적")
            .register(registry);
        this.slowQueryThresholdNanos = slowQueryThresholdMs * 1_000_000L;
    }

    @Override
    public void onAfterExecuteQuery(PreparedStatementInformation info, long elapsedNanos, SQLException e) {
        recordMetric(elapsedNanos);
    }
    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation info, long elapsedNanos, int rowCount, SQLException e) {
        recordMetric(elapsedNanos);
    }
    @Override
    public void onAfterExecuteBatch(StatementInformation info, long elapsedNanos, int[] counts, SQLException e) {
        recordMetric(elapsedNanos);
    }

    private void recordMetric(long elapsedNanos) {
        queryTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
        if (elapsedNanos > slowQueryThresholdNanos) {
            slowQueryCounter.increment();
        }
    }
}
```

> P6Spy 스타터는 `@Component` 로 등록된 `JdbcEventListener` 빈을 **모두 자동 수집**해요. 따라서 `QueryStatsJdbcEventListener` 와 공존 가능 (두 리스너가 같은 이벤트를 각자 받음).

검증:

- 부팅 후 `curl http://localhost:9090/actuator/prometheus | grep jdbc_query_duration` 으로 메트릭 노출 확인.
- k6 부하 후 `histogram_quantile(0.95, sum by (le) (rate(jdbc_query_duration_seconds_bucket[5m])))` 가 의미 있는 값으로 떨어지는지.

### Commit #11 — `refactor: narrow QueryStatsJdbcEventListener to per-request accounting`

목적: 책임 분리. `QueryStatsJdbcEventListener` 는 더 이상 슬로우 쿼리 WARN 발행을 담당하지 않고, `QueryMetricsJdbcEventListener` (메트릭) + 새 `SlowQueryJdbcEventListener` (구조화 로그) 로 책임을 쪼갠다.

변경 파일:

- [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java) — `logIfSlow` 제거, ThreadLocal 집계만 유지.
- 신규 `src/main/java/com/umc/product/global/observability/SlowQueryJdbcEventListener.java` — PR-2 Commit #6 의 `logIfSlow` 로직을 옮겨옴.

기대 구조:

```
JdbcEventListener
├── QueryStatsJdbcEventListener      → ThreadLocal 누적 (요청 단위 queryCount/queryTimeMs)
├── QueryMetricsJdbcEventListener    → Micrometer Timer / Counter (전역 P95/P99)
└── SlowQueryJdbcEventListener       → event=slow_query 구조화 로그 (한 줄 단위)
```

검증:

- 단위 테스트: 각 리스너가 독립적으로 동작 (한 리스너 비활성화 시 다른 리스너에 영향 없음).
- 기존 `LoggingInterceptor` 의 `queryCount` / `queryTimeMs` 값이 동일하게 집계.

### Commit #12 — `chore: provision jdbc query duration panel and link from ADR-013`

목적: ADR-013 의 k6 부하 결과 분석에서 즉시 참조 가능한 Grafana 패널 1개 추가 + ADR-013 References 에 패널 경로 추가.

변경 파일:

- `docker/monitoring/config/grafana/provisioning/dashboards/jdbc-query-duration.json` (신규)
- [docs/adr/013-k6-load-and-performance-testing-strategy.md](../adr/013-k6-load-and-performance-testing-strategy.md) — References 절에 패널 링크 추가.

패널 PromQL 예시:

```promql
# P95 단일 쿼리 시간 (전체)
histogram_quantile(0.95,
  sum by (le) (rate(jdbc_query_duration_seconds_bucket[5m]))
)

# slow_query 발생률
rate(jdbc_query_slow_total[5m])
```

### PR-3 머지 후 운영 점검

- [ ] Prometheus scrape 가 새 메트릭을 정상 수집 (스크랩 에러 0).
- [ ] k6 1회 부하 후 P95/P99 가 기대 범위에 들어오는지 baseline 갱신.
- [ ] LogQL `| json | event="slow_query"` 와 Prometheus `jdbc_query_slow_total` 의 카운트가 일치하는지 (cross check — 둘이 크게 어긋나면 한쪽 임계값 설정 오류).

---

## 일정 / 의존 관계 / 롤백 절차

### 의존 관계

```
PR-1 (위생 + env 출력 제어)
   └─ PR-2 (slow_query / heavy_db 이벤트)     ← PR-1 의 라인 로깅 off 가 선결
         └─ PR-3 (Micrometer 메트릭화)         ← PR-2 의 임계값 / 책임 분리 재사용
```

세 PR 은 순차 머지가 안전해요. 동시 진행 시 같은 클래스 (`QueryStatsJdbcEventListener`) 를 3 PR 이 동시에 손대므로 머지 충돌이 잦아져요.

### 예상 소요

| PR   | 코드 변경 | 테스트    | 운영 모니터링      | 합계            |
|------|-------|--------|--------------|---------------|
| PR-1 | 2 시간  | 1 시간   | +1 시간 dev 관찰 | 4 시간          |
| PR-2 | 3 시간  | 1.5 시간 | +1 일 dev 관찰  | 5 시간 + 1 일 관찰 |
| PR-3 | 4 시간  | 1.5 시간 | +k6 1회       | 6 시간          |

### 롤백 절차

- **PR-1 롤백**: 환경변수 `P6SPY_ENABLE_LOGGING=true` 만 재주입하면 라인 로깅 즉시 복귀. 커밋 revert 불필요.
- **PR-2 롤백**: Commit #6 / #7 만 revert. `event=api_request_completed` 가 다시 모든 요청에서 INFO 로 통일됨.
- **PR-3 롤백**: Commit #10 / #11 만 revert. `QueryStatsJdbcEventListener` 가 PR-2 시점 구조로 복귀. Micrometer 메트릭은 사라지지만 Prometheus 쪽 alert/dashboard 가 깨지지 않도록 패널을 readonly 로 두는 게 안전 (Commit #12 의 패널은 메트릭이 사라지면 빈 그래프가 됨 — 이 정도 영향은 허용).

---

## PR 본문 템플릿 (참고용)

CLAUDE.md §6 의 PR 제목 규약에 따라 다음 형식 사용:

- PR-1: `[Refactor] P6Spy 출력 위생 정리 및 환경별 라인 로깅 제어`
- PR-2: `[Feat] slow_query / api_request_heavy_db 구조화 이벤트 추가`
- PR-3: `[Feat] JDBC 쿼리 시간 Micrometer 메트릭화 + 책임 분리`

각 PR 본문에 다음 섹션을 포함 (저장소의 `.github/pull_request_template.md` 따름):

- 🚀 작업 내용 (5W1H)
- 🤔 작업 배경 — 본 계획 문서 링크
- 🧪 테스트 — 단위/통합/수동 체크리스트
- 📌 영향 범위 — 환경변수, 운영 대시보드, 롤백 절차

---

## 참고

- [ADR-016 §커밋 단위 실행 계획](../adr/016-structured-json-logging-with-mdc.md) — 본 계획의 커밋 단위 분리 방식의 레퍼런스
- [dependencies.gradle.kts](../../gradle/dependencies.gradle.kts)
- [LoggingInterceptor.java](../../src/main/java/com/umc/product/global/config/LoggingInterceptor.java)
- [QueryStatsJdbcEventListener.java](../../src/main/java/com/umc/product/global/config/QueryStatsJdbcEventListener.java)
- [QueryStatsHolder.java](../../src/main/java/com/umc/product/global/config/QueryStatsHolder.java)
- [P6SpyConfig.java](../../src/main/java/com/umc/product/global/config/P6SpyConfig.java)
- [logback-spring.xml](../../src/main/resources/logback-spring.xml)
- [application.yml](../../src/main/resources/application.yml)
