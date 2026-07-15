# 자체 호스팅 모니터링 스택 (ADR-014 Phase 1)

[ADR-014](../../docs/adr/014-self-hosted-monitoring-stack-migration.md) 의 Phase 1 docker-compose 스택. 앱이 이미 사용 중인 **OTLP push 송신을 그대로 두고 URL 만 본 스택의 endpoint 로 바꾸면** metrics / traces / logs 가 자체 호스팅 백엔드로 흘러 들어간다.

```
앱 ──OTLP──▶ otel-collector ─┬─▶ prometheus  (metrics)
                              ├─▶ tempo       (traces)
                              └─▶ loki        (logs, OTLP receiver)
```

## 빠른 시작

```bash
cd docker/monitoring
cp env.example .env
OTEL_INGEST_TOKEN=$(openssl rand -hex 32)
sed -i.bak "s/^OTEL_INGEST_TOKEN=.*/OTEL_INGEST_TOKEN=$OTEL_INGEST_TOKEN/" .env
docker compose up -d
```

Linux 호스트에서 `node-exporter`까지 함께 띄울 때:

```bash
docker compose --profile linux up -d
```

기본 접속:

- Grafana: <http://localhost:13000> (`.env` 의 `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD`)
- Prometheus: <http://localhost:19090>
- Loki: <http://localhost:13100>
- Tempo: <http://localhost:13200>
- OTel Collector OTLP: gRPC `:4317`, HTTP `:4318`

macOS 로컬에서 Homebrew `node_exporter`를 사용할 때:

```bash
brew install node_exporter
brew services start node_exporter
curl http://localhost:9100/metrics
docker compose up -d
```

Prometheus 컨테이너는 macOS 호스트의 Homebrew `node_exporter`를
`host.docker.internal:9100`으로 scrape 한다. Docker Desktop macOS에서는 이 hostname이
기본 제공되며, compose 에도 `host.docker.internal:host-gateway`를 명시해 Linux Docker
Engine 호환성을 확보한다.

Grafana 의 `Node Exporter Host Metrics` 대시보드는 상단 `OS` 드롭다운 기본값이
`macOS`이다. Linux 홈서버 메트릭을 볼 때는 `Linux`로 변경하면 Linux node_exporter가
노출하는 metric label 기준 쿼리를 사용한다. 이 대시보드는 node_exporter 기본 collector
가 노출하는 메트릭만 사용한다. macOS memory pressure 또는 실제 CPU/GPU 온도처럼
node_exporter 기본 메트릭에 없는 값은 별도 textfile collector/custom exporter 없이
수집하지 않는다. 대신 기본 collector가 제공하는 CPU, memory, swap, filesystem,
disk, network, battery/power supply, host clock/OS info, collector success/duration 값을
표시한다. Power supply 는 배터리 잔량, 충전 상태, 전류
(`node_power_supply_current_ampere`), full/empty 예상 시간, optional voltage/temperature
metric 을 표시한다. watt 단위 power draw 는
`abs(node_power_supply_current_ampere) * node_power_supply_voltage_volt` 로 계산하는
패널을 두었지만, macOS IOKit 이 voltage field 를 반환하지 않는 Mac 에서는 값이 비어
있다. SoC/CPU/GPU power consumption 을 안정적으로 watt 로 수집하려면 macOS
`powermetrics` 기반 custom exporter 또는 textfile collector 가 추가로 필요하다.

macOS watt power collection 선택지는 다음과 같다.

- node_exporter 기본 collector 만 사용할 경우: IOKit 이 `node_power_supply_voltage_volt`
  와 `node_power_supply_current_ampere` 를 모두 반환하는 호스트에서만 배터리/전원 watt 를
  `abs(current_ampere) * voltage_volt` 로 계산할 수 있다. 현재 로컬 Mac 은
  `current_ampere` 만 노출하고 `voltage_volt` 는 노출하지 않아 `Power Draw Estimate`
  패널이 비어 있는 것이 정상이다.
- CPU/GPU/ANE power draw 를 watt 로 보고 싶을 경우: `sudo powermetrics -i 1000 -n 1 --samplers cpu_power,gpu_power,ane_power --format plist`
  결과를 파싱하는 별도 exporter 또는 node_exporter textfile collector writer 를 운영해야 한다.
  `powermetrics` 는 root 권한이 필요하므로 운영 시 전용 wrapper 와 제한된 sudoers 설정을
  별도로 검토한다.

기본 대시보드의 API 집계 패널은 Grafana 상단 time range 를 기준으로 Prometheus
HTTP request counter / duration histogram 에서 요청 총량, 요청 수 Top 10,
p95 latency Top 10 을 계산한다. Top 10 테이블의 행 링크는 해당 `uri` 에 대응하는
Loki 로그를 열며, 로그의 `traceId` derived field 를 통해 Tempo 요청 flow 로 이동할 수 있다.

## GraphQL 대시보드

Grafana 의 **UMC PRODUCT** 폴더 > **UMC PRODUCT — GraphQL** 대시보드
(`config/grafana/dashboards/graphql.json`)에서 GraphQL 요청률, 오류율, p95/p99 지연,
operation type/outcome 분포, resolver와 DataLoader의 호출량·지연·오류를 확인할 수 있다.

GraphQL metric에는 요청 본문, variables, raw query를 넣지 않는다. `operationName`과
`executionId`는 trace의 high-cardinality attribute로만 기록되므로 Prometheus 패널에는
노출하지 않는다. 개별 요청의 operation name과 실행 흐름이 필요하면 대시보드 상단의
Tempo 링크에서 해당 trace를 확인한다.

## API 처리 흐름 보기 (Tempo + Node graph)

각 API 요청이 어떤 계층을 거치는지(`controller -> usecase -> adapter -> db`)와, 아웃박스 relay 가 원 요청 trace 와 어떻게 span link 로 이어지는지를 Tempo 로 추적한다. 계층별 span 은 앱의 `TraceFlowAspect` 가 자동 생성하며 `app.layer` / `app.domain` / `app.usecase` / `app.adapter.type` 태그를 단다.

### 방법 1: provisioned 대시보드

Grafana 의 **UMC PRODUCT** 폴더 > **API 처리 흐름 (Tempo)** 대시보드(`config/grafana/dashboards/api-flow.json`)를 연다. Traces 패널에서 trace 를 클릭하면 상세가 열리고, 상단 **Node graph** 패널에서 처리 흐름이 그래프로 표시된다.

### 방법 2: Explore 에서 TraceQL

Grafana > **Explore** > 데이터소스 **Tempo** > **TraceQL** 탭에서 아래 쿼리로 trace 를 찾은 뒤, 결과 trace 를 열고 **Node graph** 를 펼친다. 개별 span 의 **References** 에서 span link 로 연관 trace (예: 아웃박스 relay <--> 원 요청)로 이동할 수 있다.

| 목적                           | TraceQL                                             |
|------------------------------|-----------------------------------------------------|
| 특정 API 한 요청 (@Public, 테스트 편함) | `{ name = "http get /api/v1/schools/all" }`         |
| 특정 API 한 요청 (이메일 인증)         | `{ name = "http post /api/v1/auth/email-verification" }` |
| 도메인별 처리 흐름                   | `{ span.app.domain = "organization" }`              |
| 유스케이스 span 만                 | `{ span.app.usecase != "" }`                        |
| 아웃박스 relay (원 요청과 span link) — *`app.event-outbox.enabled=true` 일 때만* | `{ name = "outbox.relay.publish" }`                 |

> HTTP 서버 root span 이름은 **`http <method> <path>`** 형식이다(대문자 `GET` 이 아니라 소문자, 예: `http get /api/v1/schools/all`). trace 샘플링이 1.0 미만이면 HTTP 요청 일부가 누락될 수 있으니, 시연 시 같은 API 를 여러 번 호출한다.
>
> 계층 흐름 쿼리(`app.domain` / `app.usecase` 등)는 `TraceFlowAspect` 가 **모든 요청에 자동 생성**하므로 아웃박스 on/off 와 무관하게 동작한다. 반면 `outbox.relay.publish` span 은 **아웃박스가 활성화(`app.event-outbox.enabled=true`)된 경우에만** 생성되므로, 비활성 상태에서는 결과가 비어 있는 것이 정상이다.
>
> 집계형 서비스 토폴로지(여러 trace 를 합친 service graph)는 Tempo `metrics_generator` 활성화가 필요한 Phase 2 과제다. 위는 **요청 단위(per-trace) 처리 흐름**을 보는 방법이다.

## 앱 환경변수 매핑

운영 / 로컬 앱의 환경변수만 본 스택의 endpoint 로 바꾸면 OTLP 송신이 그대로 자체 호스팅 백엔드로 전달된다.

| 앱 환경변수                                                             | Grafana Cloud (기존)                                  | Self-hosted (본 스택)                |
|--------------------------------------------------------------------|-----------------------------------------------------|-----------------------------------|
| `OTEL_URL`                                                         | `https://otlp-gateway-*.grafana.net/otlp`           | `http://<host>:4318`              |
| `PROM_URL`                                                         | `https://otlp-gateway-*.grafana.net/.../v1/metrics` | 미설정 시 `${OTEL_URL}/v1/metrics` 사용 |
| `TEMPO_URL`                                                        | `https://otlp-gateway-*.grafana.net/.../v1/traces`  | 미설정 시 `${OTEL_URL}/v1/traces` 사용  |
| `OTEL_LOGS_URL`                                                    | 별도 구성 없음                                            | 미설정 시 `${OTEL_URL}/v1/logs` 사용    |
| `OTEL_AUTH_HEADER`                                                 | `Basic ...` 또는 `Bearer ...`                         | `Bearer <OTEL_INGEST_TOKEN>`      |
| `PROM_AUTH_HEADER` / `TEMPO_AUTH_HEADER` / `OTEL_LOGS_AUTH_HEADER` | 신호별 override                                        | 미설정 시 `OTEL_AUTH_HEADER` 사용       |

- `<host>` 는 운영 서버에서 본 스택에 도달 가능한 hostname. Phase 1 로컬 검증 단계에서는 `localhost`, 운영 이관 단계에서는 Cloudflare Tunnel 의 public hostname (예: `otel.umc.it.kr`) 으로 교체.
- 앱은 기본적으로 `OTEL_URL` 과 `OTEL_AUTH_HEADER` 만 설정하면 metrics / traces / logs 를 모두 Collector 로 보낸다.
- 개별 신호별 endpoint 를 분리해야 할 때만 `PROM_URL`, `TEMPO_URL`, `OTEL_LOGS_URL` 을 override 한다.
- compose 의 host port 는 기본적으로 `127.0.0.1` 에만 bind 된다. 외부 앱 서버에서 보내야 하면 Cloudflare Tunnel / Tailscale / WireGuard / reverse proxy 등으로 TLS 와 접근 제어를 먼저 둔다.

## 디렉터리 구조

```
docker/monitoring/
├── env.example               # 호스트 포트 / Grafana admin / Discord webhook (`.env.*` 가 gitignore 에 걸려 점 없는 이름 사용)
├── README.md                 # 본 파일
├── docker-compose.yml        # 스택 정의 (Commit 2 이후)
└── config/
    ├── prometheus/           # metric backend config
    │   └── rules/            # Prometheus alerting / recording rules
    ├── tempo/                # trace backend config
    ├── loki/                 # log backend config
    │   └── rules/fake/       # Loki Ruler LogQL rules (single-tenant tenant id: fake)
    ├── otel-collector/       # OTLP 게이트웨이 config
    ├── alertmanager/         # 알람 라우팅 / Discord webhook
    └── grafana/
        ├── dashboards/       # provisioned dashboards (JSON)
        └── provisioning/
            ├── datasources/  # prometheus / alertmanager / tempo / loki datasource
            └── dashboards/   # dashboard provider 정의
```

## 컴포넌트 책임

| 컴포넌트             | 역할                                                                                                                                            |
|------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `otel-collector` | 앱의 OTLP metrics / traces / logs 송신을 받아 prometheus / tempo / loki 로 분배. 향후 Cloud + Self dual-write 가 필요해지면 exporter 추가만 하면 된다.                 |
| `prometheus`     | OTLP write receiver (`/api/v1/otlp/v1/metrics`) 활성. 자체 scrape 는 prometheus / node-exporter / alertmanager / grafana / loki / otel-collector.  |
| `tempo`          | OTLP 4317/4318 native 수신. 로컬 디스크 backend, 기본 7일 보존.                                                                                           |
| `loki`           | OTLP logs receiver 를 통해 Collector 에서 들어온 로그 저장. 기본 30일 보존.                                                                                    |
| `grafana`        | Prometheus / Alertmanager / Loki / Tempo datasource 와 ADR-016 의 api-performance dashboard 자동 provisioning.                                    |
| `alertmanager`   | Prometheus alerting rule 의 라우팅. Discord webhook 으로 송신.                                                                                        |
| `node-exporter`  | macOS 로컬은 Homebrew `node_exporter`, Linux 운영은 `linux` profile 의 compose 컨테이너로 CPU / disk / network 메트릭을 노출. prometheus 가 `os` 라벨로 구분해 scrape. |

## 기본 알림 Rule

다음 파일들은 compose 기동 시 자동으로 로드된다. Grafana UI 에서 같은 내용을 수동 등록할 필요가 없다.

- Prometheus rule: `config/prometheus/rules/default-alerts.yml`
- Loki LogQL rule: `config/loki/rules/fake/default-log-alerts.yml`
- 현재 Discord 로 전송되는 alert 목록: `docs/alerts.md`

포함 범위:

- 서버/타깃 down, CPU/메모리/디스크, 컨테이너 재시작
- API 5xx/4xx, p95/p99 latency, 트래픽 급감/급증
- HikariCP, PostgreSQL, Redis/Valkey
- ERROR/Exception, OAuth/login 실패 신호, 이메일 실패 로그
- OTel Collector, Loki, Prometheus, Alertmanager, Grafana
- Blackbox exporter 기반 SSL/HTTP health check rule

`up` 기반 API down, PostgreSQL, Redis/Valkey, blackbox rule 은 해당 scrape target 또는 exporter 메트릭이 들어올 때부터 평가된다. 현재 스택이 기본으로 scrape 하는 내부 컴포넌트는 `prometheus`, `node-exporter`, `alertmanager`, `grafana`, `loki`, `otel-collector` 이다.

Discord 알림 본문에는 `환경`, `severity`, `대상`이 함께 표시된다. 환경 값은 `environment` -> `deployment_environment` -> `service_name` -> `application` -> `job` 순서로 선택한다. 앱별 local/dev/prod 구분은 `service.name` 또는 `spring.application.name` 을 `local-umc-product`, `dev-umc-product`, `prod-umc-product` 로 맞추는 방식이 가장 단순하다.

## 운영 주의사항

- 본 스택은 **Phase 1 로컬 검증용** 이다. Phase 2 에서는 TLS / Cloudflare Tunnel / object storage backend 등이 추가된다 (ADR-014 §Phase 2~3).
- Grafana 의 `GRAFANA_ADMIN_PASSWORD` 는 운영 전 반드시 강한 값으로 교체.
- Loki / Tempo / Prometheus 의 retention 은 docker volume 디스크 크기에 직결된다. 운영 노드의 디스크 모니터링 (`node-exporter` 의 `node_filesystem_avail_bytes`) 필수.

## 서비스 식별자 통일 (OTel service.name)

모든 signal (metrics / traces / logs) 은 **`service.name`을 단일 식별자**로 사용한다 (Phase 2 정책).

| 설정                                                                  | 값                                                 |
|---------------------------------------------------------------------|---------------------------------------------------|
| `management.opentelemetry.resource-attributes.service.name`         | `${spring.application.name}` (기본 `x-umc-product`) |
| `management.opentelemetry.resource-attributes.loki.resource.labels` | `service.name` (Loki 에서 라벨로 승격)                   |
| `management.metrics.tags`                                           | `instance` 만 (instance 는 유지)                      |

이렇게 하면:

- Prometheus 메트릭도 `service_name` 라벨 보유.
- Loki 로그도 `service_name` 라벨 보유 (structured metadata 가 아니라 라벨로 승격).
- Grafana 대시보드가 Prometheus / Loki / Tempo 모두 `service_name` 기준으로 필터링 가능.

## 참고

- [ADR-014: 모니터링 스택을 Grafana Cloud 에서 자체 홈서버로 이관한다](../../docs/adr/014-self-hosted-monitoring-stack-migration.md)
- [ADR-016: API 로그를 MDC 기반 JSON 구조화 로그로 전환한다](../../docs/adr/016-structured-json-logging-with-mdc.md) — OTLP log attributes / dashboard 의 LogQL 룰
