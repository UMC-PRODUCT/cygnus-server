# ADR-014: 모니터링 스택을 Grafana Cloud 에서 자체 홈서버로 이관한다

## Status

Proposed (2026-05-09)

## Context

본 ADR 작성 시점(2026-05-09) 기준 UMC PRODUCT 서버는 아래 4 종의 관측 데이터를 모두 외부 SaaS 로 송신하고 있다.

### 1. 현행 모니터링 스택 현황

#### 1.1 Metrics (Micrometer → Grafana Cloud Mimir)

- 수집 어댑터: `io.micrometer:micrometer-registry-prometheus` + `io.micrometer:micrometer-registry-otlp` ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts))
- 노출 경로 두 가지:
    - Pull: `:9090/actuator/prometheus` ([application.yml §253-263](../../src/main/resources/application.yml#L253-L263)). 현재 사내에서 scrape 하는 주체가 명시되어 있지 않다 (운영 시 Grafana Cloud agent 가 pull 하지 않고 push 만 사용 중).
    - Push: `management.otlp.metrics.export.url=${PROM_URL}` 로 OTLP 형식 push. `Authorization: Basic ${PROM_AUTH}` 헤더 사용, step 30s ([application.yml §270-280](../../src/main/resources/application.yml#L270-L280)).
- 태그: `application=${profile}-umc-product-api`, `instance=${HOSTNAME}` ([application.yml §282-285](../../src/main/resources/application.yml#L282-L285)).
- 활성 메트릭: jvm, process, http, jdbc, system, tomcat, hikaricp, logback, cache. spring.security 만 비활성. ([application.yml §287-298](../../src/main/resources/application.yml#L287-L298))

#### 1.2 Tracing (Micrometer Tracing → OTel → Grafana Cloud Tempo)

- 어댑터: `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` + `context-propagation` ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts))
- 엔드포인트: `management.otlp.tracing.endpoint=${TEMPO_URL:http://localhost:4318}`, 인증은 `Basic ${TEMPO_AUTH}` ([application.yml §270-274](../../src/main/resources/application.yml#L270-L274))
- sampling: `TRACE_SAMPLING_PROBABILITY` 기본 1.0 — **모든 요청 trace 가 외부로 송신** 된다. 운영에서 줄여 운영 중일 가능성이 있으나 환경변수 미세팅 시 100% 가 디폴트.

#### 1.3 Logs (logback → OpenTelemetry Collector)

- 어댑터: `logstash-logback-encoder` + `opentelemetry-logback-appender-1.0` ([dependencies.gradle.kts](../../gradle/dependencies.gradle.kts))
- 엔드포인트: `${OTEL_LOGS_URL:${OTEL_URL:http://localhost:4318}/v1/logs}`, 인증은 `OTEL_LOGS_AUTH_HEADER` ([application.yml](../../src/main/resources/application.yml))
- 출력: local은 텍스트, dev/staging/prod는 JSON stdout을 사용하며 모든 profile이 OTLP logs를 함께 전송한다 ([logback-spring.xml](../../src/main/resources/logback-spring.xml)).

#### 1.4 도커 자산 / 로컬 스택

- `docker/monitoring/config/grafana/provisioning/datasources/` 디렉터리는 존재하지만 **빈 디렉터리** 다. 즉 로컬에서 자체 Grafana 를 띄우는 자산은 골격만 잡혀 있고 실제 datasource 정의가 없다.
- `docker-compose.yml` 은 PostgreSQL 1 컨테이너만 정의 ([docker-compose.yml](../../docker-compose.yml)). monitoring stack 컨테이너는 정의되지 않음.

#### 1.6 알람 / 알림 채널

- `app.webhook.discord/slack/telegram` 가 application 코드에 포함되어 비즈니스 알림에 이미 사용 ([application.yml §325-336](../../src/main/resources/application.yml#L325-L336)).
- 인프라 알람 (CPU 높음, JVM heap 임계 등) 은 현재 Grafana Cloud Alerting 을 사용한다고 가정 (코드 외부 구성).

### 2. 결정이 필요한 이유

다음 동기들이 누적되어 있다.

1. **비용 / 한도** — Grafana Cloud free tier 의 metric series, log volume, trace span retention 한도가 운영 누적과 함께 좁아진다. 다음 단계는 paid tier 로 월 비용이 발생한다.
2. **데이터 주권 / 익명화** — 댓글/검색어/사용자 이름 등 PII 가 로그에 포함될 수 있는 도메인 (figma comment, community) 이 본격화되면서, "외부 SaaS 가 데이터를 보유한다" 는 사실 자체의 운영 부담이 커진다.
3. **로컬 개발 noise** — local profile 도 LOKI 어펜더가 활성화되어 있어, 환경변수가 잘못 세팅되면 개인 노트북 로그가 Grafana Cloud 의 운영 워크스페이스에 섞인다 ([logback-spring.xml §55-60](../../src/main/resources/logback-spring.xml#L55-L60)). 자체 호스팅이면 도달성 자체로 이 문제가 해결된다.
4. **학습 / 연속성** — 팀 내에 자체 호스팅 운영 경험을 누적할 가치. 운영 사이클이 일정한 학생 기반 팀 (UMC) 의 특성상 SaaS 의존을 완화하면 인계가 쉽다.

다만 자체 홈서버는 다음 위험을 동시에 가져온다.

- **가용성** — 가정용 회선의 ISP 정전 / 모뎀 재시작 / 동적 IP 변경 / NAS 디스크 사망. SaaS 의 SLA (99.9%) 와 비교 불가능.
- **외부 도달성** — 운영 서버는 클라우드 / ECS 환경에 있고, 홈서버는 가정 NAT 뒤다. 단순 port forwarding 은 보안 표면이 크고, 동적 IP 는 DNS 갱신 부담.
- **스토리지** — Mimir/Loki/Tempo 는 모두 cold storage (S3) backed 구조가 일반적이다. 단순 단일 노드 디스크로 가면 장기 보존이 어렵다.
- **운영 인력** — 알람을 받고 새벽에 일어나 디스크를 청소하는 사람이 누가인지 미정. SaaS 는 이 책임이 SaaS 에 있다.

본 ADR 은 이 두 압력 — "비용/주권 압력 vs 운영 위험" — 을 가시화하고, **이관을 단계적으로 채택** 하는 결정을 명시한다.

## Decision

우리는 **Grafana Cloud 4 종 (Mimir / Loki / Tempo / Alerting) 을 자체 홈서버 기반 Grafana OSS 스택으로 단계적으로 이관** 하기로 결정한다. **Sentry 는 본 ADR 범위에서 제외** 하고 SaaS 로 유지한다 (별도 ADR 후보).

이관은 다음 4 단계로 진행한다.

### Phase 0 (즉시): dual-write 토대 구축

운영 트래픽을 끊지 않은 채 로컬에서 자체 스택을 검증할 수 있도록, 송신 엔드포인트를 환경변수 1쌍이 아니라 **2쌍 (Cloud / Self-hosted)** 으로 분리할 수 있는 형태로 만든다.

- `application.yml` 의 `PROM_URL` / `TEMPO_URL` / `LOKI_URL` 을 그대로 유지.
- 이관 기간 한정으로 OTel Collector 를 운영 서버 옆에 sidecar 로 띄워 **fan-out (Cloud + Self-hosted 병행 송신)** 한다. 애플리케이션은 Collector 1곳만 바라보고, fan-out 라우팅은 Collector 의 `routing` processor 가 담당.

### Phase 1 (4~6 주): 홈서버 인프라 셋업 + 도달성 확보

홈서버 1대에 OSS 스택 docker-compose 로 구성하고, 운영 서버에서 안전하게 도달 가능한 채널을 확보한다.

- 홈서버 스펙 권장 (실측 후 조정): CPU 4 core, RAM 16 GB, NVMe SSD 1 TB. UPS 필수.
- OS: Ubuntu Server LTS, automatic-security-upgrades 활성.
- Docker compose 스택: `prometheus`, `loki`, `tempo`, `grafana`, `alertmanager`, `caddy` (reverse proxy + ACME), `node_exporter` (홈서버 자체 모니터링).
- 외부 도달성: **Cloudflare Tunnel 채택** (Decision §Alternatives §3). 가정 NAT 뒤에서도 outbound 연결 1 개로 도달, 동적 IP / 방화벽 구성 불필요.
- 인증: 모든 송신 endpoint 는 Basic Auth + TLS, Grafana 자체는 OAuth2 (Google 또는 GitHub) 로 보호.

### Phase 2 (6~10 주): traffic shift + 검증

운영 서버의 OTel Collector fan-out 가중치를 점진적으로 옮긴다.

1. **W2 까지** Cloud 100% / Self 0% (Self 는 sanity check 만 — 데이터 도착 / Grafana 대시보드 표시 확인).
2. **W4 까지** Cloud 100% / Self 100% (병행 송신, 데이터 일치성 비교).
3. **W6 까지** Cloud 50% / Self 100% — Cloud sampling rate 점진 감소, Self 는 풀 데이터.
4. **W8** Cloud 0% / Self 100% — Cloud 정지.
5. **W10** Cloud 워크스페이스 retention (보통 30일) 종료를 기다린 뒤 계정 정리.

### Phase 3 (장기): 장기 보존 / 백업

- Loki / Tempo 는 chunk 를 로컬 디스크 + **off-site object storage (예: AWS S3 또는 Cloudflare R2)** 의 backed 구성으로 운영. 홈서버 디스크 사망 시 7~30 일 retention 은 잃을 수 있어도 그 이전 데이터는 보존.
- Prometheus 는 **VictoriaMetrics 또는 Mimir single-binary** 로 교체 검토 — Prometheus 단일 노드의 long-term 보존 한계 (~14d) 를 넘기 위함.
- 백업 잡: 매일 새벽 grafana DB / dashboard JSON / alertmanager config 를 git repository 또는 NAS 로 sync.

### Phase 4 (Sentry 통합 검토)

Sentry 는 본 ADR 범위 밖이지만, Phase 2 종료 후 Glitchtip (Sentry-호환 OSS) / 자체 호스팅 Sentry 검토를 별도 ADR 로 분리한다. 본 ADR 의 이관과 동시 진행은 운영 위험을 키운다.

## Alternatives Considered

### 1. 현행 Grafana Cloud 유지

장점:

- 운영 부담 0. SLA 99.9%, 디스크 / 네트워크 / 업그레이드 모두 SaaS 책임.
- free tier 한도 안에서는 비용도 0.
- 이미 동작하고 있으므로 추가 변경 비용 없음.

단점:

- 사용량이 늘면 paid tier 로의 비용 곡선이 가팔라진다.
- 데이터 주권: PII 가 외부에 누적된다.
- 로컬 개발 시 환경변수 누락이 운영 워크스페이스 오염으로 이어질 수 있다.

선택하지 않은 이유:
"비용 / 주권 / 학습 누적" 의 세 압력 중 어느 하나도 단기에는 폭발하지 않지만, 1~2 년 누적되면 paid tier 전환이 강제된다. 그 시점에서 자체 호스팅을 시작하면 데이터 이관 비용이 더 크다. 이관 자체를 미루지 않는다.

### 2. VPS (클라우드 호스트, EC2 / Hetzner) 위 OSS 스택

장점:

- 가용성: 가정용 회선 / 정전 / 동적 IP 위험이 사라진다.
- 외부 도달성: public IP, 99.9% SLA, 표준 Caddy/Nginx + Let's Encrypt 만으로 도달.
- 디스크 / 네트워크는 클라우드 provider 책임.

단점:

- 월 비용 발생 (4 core / 16 GB / 1 TB 기준 월 4~7 만원). Grafana Cloud paid tier 와 비용 차이가 작거나 역전될 수 있다.
- 데이터 주권 측면에서는 SaaS 와 본질적 차이가 작다 (여전히 외부 호스트의 디스크).

선택하지 않은 이유:
사용자가 본 ADR 을 요청한 동기에 "자체 홈서버" 가 명시되어 있다. 비용 / 데이터 주권 / 학습 누적 셋 모두 자체 홈서버가 우위이며, VPS 는 본 ADR 의 결정 후 가용성 문제가 누적된 시점에 fallback 으로 검토할 수 있다.

### 3. 홈서버 + Cloudflare Tunnel (채택)

장점:

- 가정용 NAT / 동적 IP / 방화벽 구성을 우회. outbound 연결 1 개로 운영 서버 → 홈서버 도달.
- TLS 종단을 Cloudflare 가 처리해 인증서 갱신 운영 부담 0.
- 홈 IP 노출이 0 — 공격자가 직접 가정 IP 를 공격할 수 없다.
- 무료 tier 에서 처리량이 충분하다 (월 트래픽 한도 없음, 단 캐시 룰 차이 있음).

단점:

- Cloudflare 라는 또 다른 외부 의존이 생긴다 (그러나 SaaS 가 데이터를 저장하는 게 아니라 통과만 한다).
- Cloudflare 장애 시 운영 서버에서 홈서버 도달 불가.
- mTLS 등 강한 인증을 Cloudflare 위에서 추가 구성하려면 Zero Trust 설정이 필요.

선택한 이유:
홈서버 운영의 가장 큰 진입 장벽인 "가정 NAT / 동적 IP" 를 비용 0 으로 해결한다. 보안 표면이 작고, 운영 인력 없이도 ACME 갱신 부담이 없다.

### 4. 홈서버 + DDNS + 직접 port forwarding

장점:

- Cloudflare 같은 추가 외부 의존이 없다.
- 도메인 기반 표준 구성.

단점:

- 가정 라우터의 port forwarding / UPnP 가 ISP 의 CGNAT 환경에서 동작하지 않을 수 있다 (IPv4 부족 환경에서 ISP 가 CGNAT 적용한 경우 가정 라우터에 public IP 가 없다).
- 홈 IP 가 외부에 직접 노출되어 공격 표면이 크다.
- 동적 IP 의 DDNS TTL 동안 도달 불가 구간이 발생할 수 있다.

선택하지 않은 이유:
CGNAT 가능성, 보안 표면, 운영 부담 모두 Cloudflare Tunnel 보다 열위. 자체 ASN / 광케이블이 있다면 다시 검토할 수 있지만 가정 환경의 기본 가정과 어긋난다.

### 5. 홈서버 + Tailscale / WireGuard mesh

장점:

- 운영 서버와 홈서버를 같은 가상 네트워크에 묶어, 인증/암호화가 mesh 자체로 제공된다.
- 외부 노출이 0.

단점:

- 운영 서버 (ECS / Fargate / EC2) 가 어느 환경이냐에 따라 Tailscale 사이드카 / userspace mode 운영이 추가된다.
- ECS task 의 짧은 lifecycle 과 Tailscale auth key 의 운영이 충돌할 수 있다 (재기동마다 인증 갱신).
- Cloudflare Tunnel 보다 운영 책임이 한 단계 늘어난다 (mesh 구성원 추가/삭제 / key rotation).

선택하지 않은 이유 (현 시점):
Cloudflare Tunnel 로 outbound 1개 채널이면 충분한 본 ADR 의 트래픽 패턴 (운영 서버 → 홈서버, 단방향 push) 에 비해 mesh 는 과한 구성이다. 운영 서버에서 SSH 같은 양방향 도달이 필요해지면 그때 도입한다.

### 6. Hybrid (hot data 는 자체 호스팅, long-term 은 Grafana Cloud)

장점:

- 최근 7~14 일 디버깅용 hot data 는 자체에서 빠르게 조회. 장기 보존만 Cloud 비용 부담.
- 자체 호스팅 디스크 부담이 낮아진다.

단점:

- 운영 도구가 둘로 쪼개진다 — 같은 incident 의 분석에서 어느 시점은 Self-hosted Grafana, 어느 시점은 Cloud Grafana 를 봐야 한다.
- 데이터 주권 동기는 부분적으로만 해소.

선택하지 않은 이유 (현 시점):
이관의 동기가 "비용 + 주권 + 학습" 인 만큼 절반만 이관하면 셋 다 절반만 해소된다. 단일 책임 / 단일 운영 도구가 본 ADR 의 의도에 더 부합한다. Phase 3 의 long-term object storage 를 R2/S3 로 분리하는 것으로 비용 부담은 어느 정도 해소된다.

### 7. Sentry 도 동시에 자체 호스팅 (Glitchtip)

장점:

- 외부 SaaS 의존이 한 번에 모두 사라진다.
- error / metrics / logs / traces 가 모두 한 인프라 위에 있다.

단점:

- Sentry 자체 호스팅은 docker compose 만으로도 8+ 컨테이너로, 본 ADR 의 4 종 이관과 동시 진행은 운영 위험을 키운다.
- Glitchtip 은 호환성이 좋지만 Sentry 의 일부 SDK 기능 (예: profiling) 미지원.

선택하지 않은 이유 (현 시점):
본 ADR 의 4 종 이관이 안정화된 후 별도 ADR 로 분리. 동시 도입은 실패 격리가 어렵다.

### 8. Self-hosted 인데 Prometheus 만 사용 (Loki/Tempo 미도입)

장점:

- 운영 책임이 1/3 로 준다.
- Prometheus 단일 노드 운영 경험은 흔하고 자료가 많다.

단점:

- 로그 / trace 가 사라지면 트러블슈팅의 가장 큰 도구를 잃는다. 본 시스템은 LLM / 외부 API / 비동기 트랜잭션이 많아 trace 가 디버깅의 1차 수단이다.
- 대시보드의 metric ↔ log ↔ trace 간 jump (Grafana 의 핵심 UX) 가 사라진다.

선택하지 않은 이유:
"이관" 이 아니라 "기능 축소" 가 된다. 본 ADR 의 동기 (비용/주권) 와 별도로 관측 가능성 자체를 깎는 결정이라 별개 검토 대상.

## Consequences

### Positive

- 운영 트래픽이 늘어나도 SaaS paid tier 비용이 발생하지 않는다.
- 모든 metric / log / trace 데이터가 자체 인프라에 저장되어 데이터 주권 / 익명화 정책의 단일 책임 라인이 명확해진다.
- 로컬 개발에서 환경변수가 잘못 세팅되어도 외부 SaaS 워크스페이스를 오염시킬 위험이 사라진다 (홈서버는 외부에서 도달하지 못함, 또는 Cloudflare Tunnel 인증 hostname 이 운영용이라 local 송신은 자연 차단).
- 팀이 OSS 모니터링 스택 운영 경험을 누적, 후속 인프라 / 백엔드 도메인 (예: 트래픽 분석, 데이터 파이프라인) 의 의사결정 비용이 줄어든다.
- Grafana 대시보드 / alerting 룰을 git repository 에 commit 해 IaC 기반 관리가 가능 (provisioning 디렉터리 활용).

### Negative

- 가용성 SLA 가 SaaS 99.9% 에서 가정 회선의 실측 가용성 (대략 99.0~99.5%) 으로 떨어진다. 본 시스템 자체의 SLA 가 아니라 **모니터링 스택의 SLA** 라는 점에 주의 — 운영 서버가 정상이어도 모니터링이 끊긴 동안의 incident 는 후행 분석이 어렵다.
- 디스크 / UPS / ISP / 동적 IP / 가정 정전 같은 가정 환경 위험이 모니터링 인프라의 신뢰에 직접 영향을 준다.
- 운영 책임자가 명확해야 한다 — 디스크 80% 알람을 받고 새벽에 청소하는 사람이 누구인지 사전에 합의되지 않으면 폐허화된다.
- Grafana Cloud 의 일부 매니지드 기능 (자동 SLO, on-call routing, IRM) 을 잃는다. 같은 효과를 OSS 도구 (Grafana OnCall, Alertmanager) 로 구현하려면 추가 학습 / 운영 비용.
- Cloudflare Tunnel 채택 시 외부 의존이 SaaS 1 → SaaS 1 (Cloudflare) 로 변하는 것이지 0 이 되는 것은 아니다.

### Neutral / Trade-offs

- 단일 홈서버 구성에서 시작하지만, 이후 트래픽이 늘면 storage backed 구성 (Loki S3, Tempo S3, Mimir + object storage) 으로 자연 확장된다. 본 ADR 의 Phase 3 결정.
- Prometheus 단일 노드의 long-term retention 한계 (~14 d) 를 넘기 위해 VictoriaMetrics 또는 Mimir single-binary 로의 전환이 Phase 3 에서 검토된다 — 이는 본 ADR 의 결정에 묶이지 않은 재량 영역.
- 사내 GitHub Actions 가 운영 환경의 secret 을 가지고 있고, Phase 0 의 dual-write 를 위해 환경변수가 일시적으로 늘어난다. 이관 종료 시 정리 잡 (cleanup PR) 이 필수.
- Sentry 와 Grafana 가 분리 운영되는 상태가 Phase 4 까지 유지된다. error 와 metric/log/trace 의 jump 가 두 도구 사이를 오가야 하는 운영 UX 부담은 그대로다.

## Implementation Notes

### Phase 0: dual-write 준비 (1~2 주)

OTel Collector 를 운영 서버와 동일 노드 (또는 sidecar 컨테이너) 에 띄운다.

```yaml
# otel-collector-config.yaml (운영 sidecar)
receivers:
    otlp:
        protocols:
            grpc: { endpoint: 0.0.0.0:4317 }
            http: { endpoint: 0.0.0.0:4318 }

exporters:
    otlphttp/cloud:
        endpoint: ${env:GRAFANA_CLOUD_OTLP_ENDPOINT}
        headers:
            Authorization: "Basic ${env:GRAFANA_CLOUD_OTLP_AUTH}"
    otlphttp/self:
        endpoint: https://otel.umc.it.kr/v1/otlp
        headers:
            Authorization: "Basic ${env:SELF_OTLP_AUTH}"

processors:
    batch:
        send_batch_size: 1024
        timeout: 5s
    routing/fanout:
    # routing processor 가 같은 데이터를 두 exporter 로 fan-out

service:
    pipelines:
        metrics:
            receivers: [ otlp ]
            processors: [ batch ]
            exporters: [ otlphttp/cloud, otlphttp/self ]
        traces:
            receivers: [ otlp ]
            processors: [ batch ]
            exporters: [ otlphttp/cloud, otlphttp/self ]
        logs:
            receivers: [ otlp ]
            processors: [ batch ]
            exporters: [ otlphttp/cloud, otlphttp/self ]
```

애플리케이션의 환경변수는 Collector 1곳만 가리키도록 변경.

```bash
export PROM_URL=http://otel-collector:4318/v1/metrics
export TEMPO_URL=http://otel-collector:4318
export LOKI_URL=http://otel-collector:4318/v1/logs   # 또는 loki4j 는 직접 self loki 로
```

> Loki4j 는 OTLP 가 아닌 Loki push API 를 직접 호출하므로, dual-write 를 위해서는 (a) loki4j 어펜더를 OTLP logging 으로 교체하거나, (b) 두 어펜더를 동시에 적용하는 선택이 있다. 본 ADR 은 OTel Collector 일원화를 위해 (a) 를 권장하되, Phase 1 에 한해 (b) 도 허용한다 (검증 기간 단축).

### Phase 1: 홈서버 docker-compose (예시)

```yaml
# docker-compose.monitoring.yml
services:
    prometheus:
        image: prom/prometheus:v2.55.0
        volumes:
            - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
            - prometheus-data:/prometheus
        command:
            - --storage.tsdb.retention.time=14d
            - --storage.tsdb.path=/prometheus
            - --enable-feature=otlp-write-receiver
        restart: unless-stopped

    loki:
        image: grafana/loki:3.2.0
        volumes:
            - ./loki/loki.yml:/etc/loki/local-config.yaml:ro
            - loki-data:/loki
        restart: unless-stopped

    tempo:
        image: grafana/tempo:2.6.0
        command: -config.file=/etc/tempo.yml
        volumes:
            - ./tempo/tempo.yml:/etc/tempo.yml:ro
            - tempo-data:/var/tempo
        restart: unless-stopped

    grafana:
        image: grafana/grafana:11.3.0
        volumes:
            - ./grafana/provisioning:/etc/grafana/provisioning:ro
            - grafana-data:/var/lib/grafana
        environment:
            - GF_SERVER_ROOT_URL=https://grafana.umc.it.kr
            - GF_AUTH_GOOGLE_ENABLED=true
            - GF_AUTH_GOOGLE_CLIENT_ID=${GOOGLE_OAUTH_CLIENT_ID}
            - GF_AUTH_GOOGLE_CLIENT_SECRET=${GOOGLE_OAUTH_CLIENT_SECRET}
            - GF_AUTH_GOOGLE_ALLOWED_DOMAINS=umc.it.kr
        restart: unless-stopped

    alertmanager:
        image: prom/alertmanager:v0.27.0
        volumes:
            - ./alertmanager/config.yml:/etc/alertmanager/config.yml:ro
        restart: unless-stopped

    cloudflared:
        image: cloudflare/cloudflared:latest
        command: tunnel --no-autoupdate run
        environment:
            - TUNNEL_TOKEN=${CLOUDFLARE_TUNNEL_TOKEN}
        restart: unless-stopped

    node_exporter:
        image: prom/node-exporter:v1.8.2
        pid: host
        network_mode: host
        volumes:
            - /:/host:ro,rslave
        command:
            - --path.rootfs=/host
        restart: unless-stopped

volumes:
    prometheus-data:
    loki-data:
    tempo-data:
    grafana-data:
```

`docker/monitoring/config/grafana/provisioning/datasources/datasources.yaml` 채워넣기:

```yaml
apiVersion: 1
datasources:
    -   name: Prometheus
        type: prometheus
        access: proxy
        url: http://prometheus:9090
        isDefault: true
    -   name: Loki
        type: loki
        access: proxy
        url: http://loki:3100
    -   name: Tempo
        type: tempo
        access: proxy
        url: http://tempo:3200
        jsonData:
            tracesToLogsV2:
                datasourceUid: loki
            serviceMap:
                datasourceUid: prometheus
```

### Phase 1: Cloudflare Tunnel 설정 요지

1. Cloudflare 계정에서 Zero Trust > Access > Tunnels 에 신규 tunnel 생성, `TUNNEL_TOKEN` 발급.
2. Public Hostname 매핑:
    - `grafana.umc.it.kr` → `http://grafana:3000`
    - `otel.umc.it.kr` → `http://otel-collector:4318` (운영 서버에서 push 받는 경로)
    - `prometheus.umc.it.kr` → `http://prometheus:9090` (관리자 한정, Access Policy 로 사내 Google 계정 제한)
3. Cloudflare Access Policy 로 `otel.umc.it.kr` 는 mTLS 또는 service token 인증, 그 외는 Google OAuth.
4. 홈서버는 inbound 포트 0개 (모든 연결은 outbound) — 가정 라우터 변경 0.

### Phase 2: traffic shift 운영 체크리스트

- [ ] dashboard parity: 같은 5분 윈도우의 동일 panel 이 Cloud / Self 에서 동일한 값을 보여주는가?
- [ ] alerting parity: 같은 룰이 양쪽에서 같은 firing 시점을 보고하는가?
- [ ] retention 합의: Self 의 metric 14d / log 30d / trace 7d 가 운영 디버깅에 충분한가? 부족하면 Phase 3 로 즉시 진입.
- [ ] cost monitoring: Cloudflare Tunnel egress, 홈 ISP 트래픽, 백업 object storage 비용을 첫 1개월 실측.

### Phase 3: object storage backed 구성 (장기)

- Loki 의 `boltdb-shipper` + S3/R2 backend 로 chunk 영구 보존.
- Tempo 의 backend 를 `local` → `s3` 로 전환.
- VictoriaMetrics single-node 또는 Mimir single-binary 로 metric long-term 보존.

### 환경변수 정리 (이관 종료 시점)

이관 종료 후 다음 변수의 운명:

| 변수                                               | Phase 0~2            | Phase 2 종료 후               |
|--------------------------------------------------|----------------------|----------------------------|
| `PROM_URL` / `PROM_AUTH`                         | Cloud → Self 로 점진 전환 | Self URL 만                 |
| `TEMPO_URL` / `TEMPO_AUTH`                       | Cloud → Self         | Self URL 만                 |
| `LOKI_URL` / `LOKI_USERNAME` / `GRAFANA_API_KEY` | Cloud → Self         | Self URL / Self credential |
| `GRAFANA_CLOUD_*`                                | dual-write 임시        | 제거                         |
| `CLOUDFLARE_TUNNEL_TOKEN` (홈서버)                  | 신규                   | 유지                         |

GitHub Actions 의 secret 도 같은 매핑으로 정리하고, 변경 PR 은 본 ADR 을 reference 한다.

### 운영 시 주의사항

- **로컬 profile 의 LOKI 어펜더**: 본 이관과 무관하게 즉시 점검 대상. local profile 에서는 LOKI 어펜더를 비활성화하거나 `LOKI_URL` 가 비어 있을 때 자동 disable 되도록 조정 ([logback-spring.xml §55-60](../../src/main/resources/logback-spring.xml#L55-L60)).
- **trace sampling**: `TRACE_SAMPLING_PROBABILITY=1.0` 디폴트는 Cloud 에서는 비용/한도 압력으로 보통 0.1 로 운영했을 가능성이 높다. Self 로 옮기면 비용 압력은 사라지지만 디스크 압력으로 대체되므로, Self Tempo 의 retention 과 함께 sampling rate 를 운영 데이터로 결정한다.
- **알람 갱신**: Cloud Grafana 의 alerting rule / contact point 를 Self Grafana 로 export → import. Discord webhook 은 이미 코드에 있으므로 alerting contact point 로 재사용.
- **백업**: Grafana DB (`grafana.db`), provisioning 디렉터리, alertmanager config, prometheus rule 파일은 git repository (`docker/monitoring/config/`) 에 commit. 주간 1회 cron 으로 dashboard JSON export → git push.
- **장애 시 fallback**: Phase 2 종료 직후 4 주 동안은 Cloud 워크스페이스를 read-only 로 유지해 비교/롤백 여지를 남긴다.

## References

- 관련 ADR
    - [ADR-013: k6 기반 부하·성능 테스트 도입 전략](013-k6-load-and-performance-testing-strategy.md) — k6 의 결과를 동일 Self-hosted Prometheus 로 송신하면 운영 메트릭과 부하 메트릭이 한 대시보드에 모인다.
- 기존 코드 / 설정
    - [application.yml — management 섹션](../../src/main/resources/application.yml#L252-L418)
    - [logback-spring.xml — Loki 어펜더](../../src/main/resources/logback-spring.xml)
    - [dependencies.gradle.kts — micrometer / opentelemetry 의존성](../../gradle/dependencies.gradle.kts)
    - [docker/monitoring/config/grafana/provisioning/datasources/](../../docker/monitoring/config/grafana/provisioning/datasources/) — 현재 빈 디렉터리, Phase 1 에서 채움.
- 외부 자료
    - [Grafana OSS docker-compose example](https://grafana.com/docs/grafana/latest/setup-grafana/installation/docker/)
    - [Loki self-hosted operations](https://grafana.com/docs/loki/latest/operations/)
    - [Tempo single-binary deployment](https://grafana.com/docs/tempo/latest/setup/deployment/)
    - [OpenTelemetry Collector — routing/fanout processor](https://opentelemetry.io/docs/collector/configuration/)
    - [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-networks/)
    - [Glitchtip (self-hosted Sentry-compatible)](https://glitchtip.com/) — Phase 4 검토 후보.
