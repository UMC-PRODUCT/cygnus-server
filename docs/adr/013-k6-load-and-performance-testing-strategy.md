# ADR-013: k6 기반 부하·성능 테스트 도입 전략 및 도메인 우선순위

## Status

Proposed (2026-05-08)

## Context

본 ADR 작성 시점(2026-05-08) 기준 UMC PRODUCT 서버는 다음과 같은 운영 조건을 가진다.

- 단일 인스턴스(`local` 기본 profile, 운영은 별도 단일 노드) + PostgreSQL 18 + Caffeine L1 + JPA/QueryDSL.
- 관측 인프라는 Prometheus(`9090`), OTLP tracing, Sentry, Loki 까지 갖춰져 있어 latency / error / DB 풀 사용량 등의 응답 측 지표는 이미 수집된다.
- 그러나 **사전 부하 테스트로 검증된 SLO 가 존재하지 않는다.** "어느 endpoint 가 어느 RPS 에서 무너지는가" 의 근거 데이터가 비어 있어, 실제 운영 spike (예: 프로젝트 신청 마감 직전, 출석 인증 마감 직전) 가 도래하기 전까지 한도를 알 수 없다.
- 동시에 virtual-thread 비동기 같은 구조 변경의 효과 검증을 운영 사이클의 자연 트래픽으로만 관찰하고 있어, 변경 전후의 정량적 비교가 어렵다.

도메인 분포는 다음과 같다 (web Controller 수 / persistence Adapter 수 기준, 5/8 일 빌드 트리 기반).

| 도메인                                                                    | Controllers    | Persistence Adapters | 운영 특성                                                                 |
|------------------------------------------------------------------------|----------------|----------------------|-----------------------------------------------------------------------|
| organization (school/chapter/gisu/studyGroup)                          | 9              | 8                    | 학교/기수/챕터/스터디그룹 계층 read 가 거의 모든 다른 도메인의 lookup 으로 깔린다 — 캐시·N+1 의 1차 변수 |
| curriculum                                                             | 7              | 8                    | 챌린저 일정·진도와 직결, schedule 도메인과 join                                     |
| project                                                                | 5              | 7                    | **신청 마감 직전 spike** 가 가장 가파른 도메인 — application form / matching round   |
| community (post/comment/scrap/trophy/report)                           | 5 (+1 Comment) | 5                    | read-heavy 피드, 정렬·페이지네이션의 N+1 위험                                      |
| challenger                                                             | 5              | 3                    | record 제출(매주 마감 spike), search, point 적립                              |
| authentication                                                         | 5              | 2                    | 로그인/토큰 갱신 — 모든 트래픽의 사전 단계, 평소 baseline 1차 후보                          |
| notice                                                                 | 4              | 3                    | 공지 게시 직후 fan-out read                                                 |
| figma                                                                  | 4              | 4                    | 외부 동기/스케줄러 — 비동기 처리 효과 검증 대상                                          |
| schedule (V2)                                                          | 2              | 2                    | 캘린더 range 조회, 다른 도메인이 중첩 호출                                           |
| 기타 (term, storage, member, audit, survey, notification, authorization) | 1~2            | 1~6                  | spike 가능성 또는 관측 우선순위 낮음                                               |

이 ADR 이 결정해야 하는 것은 다음이다.

- k6 를 부하·성능 테스트 도구로 채택할지, 아니면 JMeter / Gatling / Locust 등 대안을 택할지.
- **도메인 단위 우선순위** 와 **시나리오(smoke / load / stress / spike / soak / breakpoint) 별 적용 전략** 을 어떻게 구성할지.
- 결과를 어디에/어떻게 기록·비교할지 (단발성 측정에 그치지 않고, 비동기 처리 같은 구조 변경 전후의 회귀 비교가 가능한 형태로).
- 테스트 대상 환경 (local / staging / production) 과 데이터 격리 / 인증 토큰 발급 / cleanup 정책.
- CI 통합 범위 — PR 당 자동 실행할지, 주간 정기로 돌릴지.

## Decision

우리는 다음을 결정한다.

### 1. 도구는 k6 (Grafana k6, OSS) 를 채택한다

- 스크립트 언어가 ES module 형태의 JS 라 백엔드 팀이 추가 학습 비용이 가장 적다 (JMeter XML / Scala DSL 학습 비용 회피).
- 기존 Prometheus / Grafana 스택과 자연스럽게 연동된다 (`k6 run --out experimental-prometheus-rw=...`). 이미 운영 중인 대시보드와 동일한 데이터 모델로 결과를 합친다.
- thresholds (p95 < 500ms 등) 가 first-class 라 SLO 검증을 스크립트 내에 명시할 수 있다.
- TestContainers / docker-compose 와 같은 환경에서 단일 바이너리로 실행 가능하다.

### 2. 부하 테스트 디렉터리 구조와 기록 위치

> **갱신 노트 (2026-07-22, load-test v1):** 실행 코드 위치는 `loadtest/k6/` 로 확정하고, 개별 시나리오를 직접 `k6 run` 하는 대신 **단일 `script.js` entrypoint 가 `__ENV.PROFILE × __ENV.SCENARIO` 로 실행 대상을 고르는** 구조로 구현했다. 이 ADR 뒤쪽 예시에 나오는 `k6 run loadtest/scenarios/...` 형태는 `run-umc-k6 <profile> <scenario> <rate> <duration>` 로 대체되었다. v1 은 부하 유형 smoke/load/stress/soak 를 구현하고 spike/breakpoint 는 후속으로 둔다. 실행·확장 방법은 `loadtest/README.md` 참조.

```text
loadtest/k6/
├── script.js                 # 단일 entrypoint. __ENV.PROFILE × __ENV.SCENARIO 로 실행 대상 선택
├── config/
│   └── profiles.js           # smoke/load/stress/soak 별 options·thresholds·기본 RATE/DURATION
├── lib/
│   ├── auth.js               # 부하 전용 토큰 발급(TEST-007) + VU 단위 캐시
│   ├── data.js               # seed.json 로드 전용 (k6 는 시딩하지 않음)
│   ├── http.js               # 공통 headers·checks·error tagging
│   └── metrics.js            # 시나리오별 custom metrics
├── scenarios/
│   ├── smoke/{health-check,project-read}.js
│   ├── load/{project-read,application-submit}.js
│   ├── stress/project-read.js
│   └── soak/project-read.js
└── data/
    ├── seed.example.json     # 형태 참조 (커밋)
    └── seed.json             # prepare-data.sh 산출물 (gitignore)
```

docs/loadtest/
├── README.md                # 결과 인덱스
├── runs/
│   └── YYYY-MM-DD-{label}/  # 실행 1회 = 디렉터리 1개
│       ├── summary.md       # 환경, 가설, 결과 요약, 후속 액션
│       ├── thresholds.json  # k6 가 생성한 raw 결과
│       └── grafana-link.md  # 실행 시점 dashboard 영구 링크
└── adr-followups/           # ADR 변경 전후 비교 (예: 비동기 처리 전환)
```

- 결과 디렉터리는 `docs/loadtest/runs/` 아래에 **실행 단위로 디렉터리를 만들어 영구 보존** 한다. 각 실행은 단순 raw output 이 아니라 `summary.md` 로 가설·환경·결론을 같이 남긴다 (ADR 의 `Context → Decision → Consequences` 와 같은 사고 형식을 작은 규모로 반복).
- ADR 의 결정이 부하 특성에 영향을 주는 경우 (비동기 도입 등), 해당 ADR 의 References 섹션에서 `docs/loadtest/adr-followups/` 의 비교 보고서를 역참조한다.

### 3. 도메인 우선순위 (3 단계)

#### Tier 1 — 즉시 도입 (Phase 1, 4~6 주)

운영 spike 가 가장 명확하거나, 모든 도메인의 사전 단계라 baseline 으로 깔리는 도메인.

1. **authentication** — 로그인 / 토큰 재발급. 모든 시나리오의 사전 단계로 reuse 되므로 가장 먼저 안정적인 token issuance helper 를 만들어야 한다. (smoke + load)
2. **project (application form / matching round)** — UMC 운영 사이클상 가장 가파른 spike 가 발생한다 ("신청 마감 23:59 직전 N분간 RPS 가 평소 수십 배"). spike 시나리오의 1순위.
3. **challenger (record / point)** — 매주 출석/제출 마감 직전 spike. 동일 endpoint 가 짧은 시간에 동일 사용자 다수의 write 를 받는 패턴.

#### Tier 2 — 후속 도입 (Phase 2, 다음 6~10 주)

read-heavy 또는 N+1 위험이 의심되는 도메인. Phase 1 으로 baseline tooling 이 정립된 뒤 진행.

4. **community (post / comment list)** — 정렬·페이지네이션·`@ManyToOne` 자식 fetch 의 N+1 가능성. load + soak.
5. **organization (school / chapter / gisu / studyGroup)** — lookup 으로 다른 도메인에 깔리는 read 라 캐시 hit ratio 측정이 가치 있다. load.
6. **curriculum + schedule (V2)** — 캘린더 range 조회는 결과 row 수가 큰 read 가 자주 발생한다. load + breakpoint.

#### Tier 3 — 선택적 / ADR 후속 (필요 시)

7. **figma + LLM 비동기 경로** — 비동기 처리 도입 전후 비교 전용. RPS 부담은 작지만 wall-clock 변화의 회귀 검증용.
8. **storage** — S3 / CloudFront signed URL 발급 latency. 외부 의존이라 부하 본체보다는 timeout / retry 동작 검증.
9. **notice / notification** — 공지 게시 직후 fan-out read. 운영 spike 가 작을 가능성이 높아 후순위.

위 우선순위는 컨트롤러/어댑터 수 단순 비례가 아니라, **운영 spike 의 가파름 × 사용자 영향도** 로 정렬한다. organization 이 컨트롤러 수가 가장 많지만, 단독 spike 보다는 다른 도메인의 lookup 에 깔리는 형태라 Tier 2 가 합리적이다.

### 4. 시나리오 매핑

각 도메인은 도메인 특성에 따라 **모든 시나리오 6 종을 무차별 적용하지 않는다**. 다음 매핑을 기본값으로 한다.

| 시나리오       | 목적                                            | 기본 적용 대상                                     |
|------------|-----------------------------------------------|----------------------------------------------|
| smoke      | 정상 흐름 확인 (VU=1, duration=1m)                  | 모든 Tier 1·2 도메인. CI 의 PR 단위 실행 후보.           |
| load       | 예상 평시 RPS 에서 SLO (p95 latency, error rate) 검증 | 모든 Tier 1·2 도메인. 주간 정기 실행.                   |
| stress     | 평시 × 3~5 배까지 ramping, 한계점 식별                  | project, challenger, community               |
| spike      | 평시 × 10~30 배 단발 폭증, recovery 시간 측정            | project (신청 마감), challenger (출석 마감)          |
| soak       | 평시 RPS 30~60 분 유지, 메모리/connection leak 식별     | community, organization, authentication      |
| breakpoint | 점진 증가 후 실패 임계치 측정                             | curriculum/schedule (range 조회), community 피드 |

### 5. CI / 자동화 통합

- **smoke 만 PR 단위 자동 실행** — `loadtest/k6/scenarios/smoke/` 하위 시나리오는 GitHub Actions 의 별도 job 으로 PR 시 staging 에 대해 실행(`run-umc-k6 smoke <scenario> ...`). p95 < SLO 임계 위반 시 fail.
- **load 는 main 머지 후 nightly** — 결과를 자동으로 `docs/loadtest/runs/<timestamp>/` 에 push.
- **stress / spike / soak / breakpoint 은 수동 실행** — capacity planning 또는 ADR 후속 검증 시점에만 운영자가 트리거. 자동 실행은 인프라 비용·운영 위험이 크다.

### 6. 환경 / 데이터 / 인증

- **테스트 대상 환경은 staging 을 1순위, local 은 보조** — staging 은 운영과 동일한 PostgreSQL 18 + 동일 인스턴스 사이즈 가정. local 은 docker-compose 의 PostgreSQL 로 빠른 반복 측정에만 사용.
- **production 직접 부하 테스트 금지** — 본 ADR 의 어떤 시나리오도 production 을 대상으로 자동 실행되지 않는다. soak/stress 가 production 에 필요하다고 판단되면 별도 ADR 로 정한다.
- **테스트 데이터 시딩** — 시딩은 `loadtest/scripts/prepare-data.sh` 가 유일하게 소유한다(k6 는 시딩하지 않음). v1 은 도메인 가드를 통과하는 SeedController API(`/test/seed/*`)를 순서대로 호출해 `loadtest/k6/data/seed.json` 을 산출한다. 대용량(10만+ 행)은 앱 이미지의 `seeder` 프로파일로 도는 Spring 벌크 시더(`SEED_STRATEGY=bulk`, JdbcTemplate 배치)가 담당한다 — 파일 아티팩트는 캐시일 뿐 원천이 아니므로 `pg_dump` 기반 sql 전략은 폐기했다. RDS snapshot 복원은 계층이 달라 `rds.tf` 의 `snapshot_identifier` 로 다룬다(반복 실행 가속 캐시). 상세: `loadtest/README.md` 의 "시딩" 절.
- **인증 토큰** — 부하 전용 access token 발급(TEST-007, `@Public`)을 `loadtest/k6/lib/auth.js` 가 VU 단위로 캐시해 재사용한다. "로그인 자체 성능" 을 재는 시나리오는 실제 로그인 API 를 호출하는 별도 시나리오로 둔다.

## Alternatives Considered

### 1. JMeter

장점:

- 업계 표준. GUI 가 있어 시나리오 시각적 편집이 가능.
- 풍부한 plugin 생태계.

단점:

- XML 기반 스크립트로 PR diff 가독성이 떨어진다.
- JVM 위에서 동작해 동일 부하를 만들기 위한 메모리/CPU 비용이 k6 보다 크다.
- 결과의 Prometheus/Grafana 통합이 plugin 의존이라 셋업 비용이 추가된다.

선택하지 않은 이유:
백엔드 팀이 동시에 운영해야 할 도구라 스크립트 가독성·diff 친화도가 결정적이다. JS 기반 k6 는 PR 리뷰가 가능한 형태다.

### 2. Gatling

장점:

- 단일 부하 생성기당 throughput 이 매우 높다.
- HTML 리포트가 강력.

단점:

- Scala DSL (또는 Java DSL) 학습 곡선. 본 팀의 핵심 언어 (Java/Spring) 와 인접하지만, 스크립트 언어가 또 하나 늘어나는 부담.
- Prometheus rw 직접 출력은 OSS 영역에서 약하다.

선택하지 않은 이유:
처리량 우위가 의미 있는 규모 (수십만 RPS) 에 도달할 가능성이 낮다. 본 시스템의 spike 는 수백~수천 RPS 범위로, k6 단일 노드로 충분하다.

### 3. Locust

장점:

- Python 기반 스크립트, 분산 worker 가 단순.
- 학습 곡선이 낮다.

단점:

- 단일 worker 당 throughput 이 k6 / Gatling 보다 낮다 — 동일 부하에 worker 노드가 더 많이 필요하다.
- thresholds 같은 SLO 명시가 first-class 가 아니라 후처리에 의존.

선택하지 않은 이유:
SLO 명시와 단일 노드 효율이 본 시스템 규모에 더 적합한 k6 가 우위.

### 4. 자체 스크립트 (Java + RestAssured 또는 ParallelStream)

장점:

- 추가 도구 0. 팀이 이미 익숙한 Java 코드만으로 시작 가능.

단점:

- 분산 부하 생성, ramping, thresholds, percentile 통계 등을 직접 구현해야 한다.
- JVM 오버헤드로 부하 생성기 자체가 병목이 될 수 있다.
- 결과 가시화 / 비교 / 영구 기록 체계를 직접 만들어야 한다.

선택하지 않은 이유:
부하 테스트 자체의 신뢰성이 도구의 성숙도에 비례한다. 자체 구현은 비용 대비 신뢰가 낮다.

### 5. 도메인 전체에 1차로 동일 시나리오 6 종 적용

장점:

- 누락 없이 모든 도메인을 한 번에 커버.

단점:

- 초기 운영 비용이 크다. 시나리오마다 environment / data 셋업이 다르고, threshold 미정의 endpoint 가 많아 fail/pass 판정 자체가 무의미해진다.
- 결과가 누적되기만 하고 의사결정에 활용되지 않으면 폐허화된다.

선택하지 않은 이유:
도메인 우선순위와 시나리오 매핑 (Decision §3·§4) 으로 좁혀, 의사결정에 직결되는 측정만 먼저 한다.

### 6. PR 단위 load 자동 실행 (smoke 가 아닌 load 까지)

장점:

- 회귀를 가장 빠르게 잡을 수 있다.

단점:

- staging 환경의 부하가 일상적이라 다른 통합 테스트 / QA 사용을 방해한다.
- 단일 PR 의 실패 원인이 도메인 코드인지 외부 변동인지 판정이 어렵다 (DB 캐시 상태, 외부 provider 응답성 등).
- CI 비용이 매우 커진다.

선택하지 않은 이유:
load 는 nightly / 수동에 두고, PR 단위는 smoke 까지만 실행해 빠른 회귀만 잡는다. capacity 회귀는 수동 트리거 또는 정기 실행으로 충분하다.

### 7. production 대상 정기 stress/spike 실행

장점:

- 실제 운영 환경의 한도를 정확히 안다.

단점:

- 운영 사용자에게 실제 영향이 가는 위험. 분리된 격리 인프라가 필요해 비용이 크다.
- 한 번의 사고로 본 ADR 채택의 정당성을 잃는다.

선택하지 않은 이유:
staging 과 운영의 구성 차이 (DB 사이즈, 인스턴스 사양) 를 보정하는 별도 보정값으로 갈음한다. production 직접 부하는 별도 ADR 에서 chaos engineering 컨텍스트로 다룬다.

## Consequences

### Positive

- 비동기 처리 같은 구조 변경의 효과를 정량적으로 비교할 수 있는 베이스라인이 생긴다.
- 운영 spike (프로젝트 신청 마감, 출석 인증 마감) 도래 전에 endpoint 별 한도가 사전에 알려져, capacity 결정과 인프라 선조정이 가능해진다.
- N+1 / connection pool 부족 / cache miss 같은 잠재적 결함이 운영 사고로 노출되기 전에 식별된다.
- 결과가 `docs/loadtest/runs/` 아래 영구 보존되어, 1 년 뒤 "왜 이 SLO 가 이 값인가" 의 근거를 추적할 수 있다.

### Negative

- 부하 스크립트 자체가 유지보수 비용이 된다. endpoint 시그니처가 바뀌면 스크립트도 같이 갱신해야 한다.
- staging 환경 비용이 늘어난다. nightly load 가 추가되고, 스트레스/스파이크 수동 실행 시 일시적으로 부하가 큰 환경 점유가 발생한다.
- 시드 데이터 격리를 위해 별도 schema/DB 운영이 필요해, 운영 데이터와의 동기 정책 (스키마 마이그레이션 시점) 을 정의해야 한다.
- 결과 해석은 도구가 자동으로 해주지 않는다. summary.md 작성을 게을리하면 raw 데이터만 쌓이고 의사결정에 쓰이지 않는다.

### Neutral / Trade-offs

- k6 의 OSS 단일 노드 모드로 시작하지만, 향후 RPS 가 단일 노드 한도를 넘으면 k6 Cloud / k6 Operator (Kubernetes) 로 이전해야 한다 — 그 시점에 별도 ADR 로 평가한다.
- smoke 만 PR 자동 실행이라, 작은 회귀(예: p95 가 200ms → 350ms 로 늘었지만 둘 다 SLO 임계 아래) 는 nightly 까지 누적된 뒤 발견될 수 있다. 의도된 trade-off.
- 도메인 우선순위는 운영 사이클에 종속이라, UMC 의 학기/기수 일정이 바뀌면 Tier 1·2 의 우선순위도 다시 정렬해야 한다.

## Implementation Notes

### Phase 1 도입 단계 (4~6 주)

1. 디렉터리 골격 생성 (`loadtest/`, `docs/loadtest/`).
2. 공통 helper 작성:
    - `loadtest/lib/auth.js` — `setup()` 단계에서 N 개 토큰 일괄 발급, VU 간 round-robin.
    - `loadtest/lib/checks.js` — 공통 status / latency check 와 threshold 정의.
    - `loadtest/lib/data/seed.sql` — 결정적 시드 (챌린저 1000, 기수 1, 챕터 5, 스터디그룹 20 등).
3. **smoke/authentication** 부터 작성 — 다른 모든 시나리오의 토큰 발급 helper 검증 포함.
4. **load/project** 의 application form 제출 시나리오, **spike/project** 의 마감 직전 폭증 시나리오 작성.
5. **load/challenger** 의 record 제출 / point 적립 시나리오, **spike/challenger** 의 출석 마감 시나리오 작성.
6. GitHub Actions workflow `loadtest-smoke.yml` 추가 — PR 단위 staging 대상 smoke 자동 실행.
7. 결과 영구 보존을 위한 `docs/loadtest/runs/` 의 첫 결과 1 회 push 후 `docs/loadtest/README.md` 의 인덱스 형식 확정.

### Phase 2 도입 단계 (다음 6~10 주)

- community / organization / curriculum 시나리오 작성.
- nightly load 자동 실행 (`loadtest-nightly.yml`).
- 비동기 처리 도입 전후의 효과 검증 보고서 작성.

### k6 실행 예시

```bash
# smoke (1 VU, 1m)
k6 run loadtest/scenarios/smoke/authentication-login.js

# load with thresholds
k6 run \
  --out experimental-prometheus-rw=http://prometheus.staging.internal/api/v1/write \
  --tag testid=2026-05-08-project-load-001 \
  loadtest/scenarios/load/project/application-form-submit.js

# spike (마감 직전 시뮬레이션, 평시 × 20)
k6 run loadtest/scenarios/spike/project/application-deadline.js
```

### 스크립트 골격 (예시 — project 신청 마감 spike)

```javascript
// loadtest/scenarios/spike/project/application-deadline.js
import http from "k6/http"
import {check, sleep} from "k6"
import {issueTokens, pickToken} from "../../../lib/auth.js"

export const options = {
    scenarios: {
        deadline_spike: {
            executor: "ramping-arrival-rate",
            startRate: 5,
            timeUnit: "1s",
            preAllocatedVUs: 200,
            maxVUs: 500,
            stages: [
                {target: 5, duration: "1m"},     // 평시
                {target: 200, duration: "30s"},  // 마감 30초 전 폭증
                {target: 200, duration: "1m"},   // 마감 직전 유지
                {target: 5, duration: "1m"},     // 마감 후 복귀
            ],
        },
    },
    thresholds: {
        http_req_failed: ["rate<0.01"],
        http_req_duration: ["p(95)<800", "p(99)<2000"],
    },
}

export function setup() {
    return {tokens: issueTokens(200)}
}

export default function (data) {
    const token = pickToken(data.tokens)
    const res = http.post(
        `${__ENV.BASE_URL}/api/v1/projects/{projectId}/applications`,
        JSON.stringify({ /* fixture */}),
        {headers: {Authorization: `Bearer ${token}`, "Content-Type": "application/json"}},
    )
    check(res, {"status is 201": (r) => r.status === 201})
    sleep(1)
}
```

### 결과 기록 템플릿 (`docs/loadtest/runs/<timestamp>/summary.md`)

```markdown
# 부하 테스트 결과: 2026-05-08 project application spike

## 환경

- 대상: staging
- 인스턴스: t3.medium × 1, RDS db.t3.medium
- Git ref: <commit sha>
- 시드 데이터 버전: seed-v3

## 가설

- 평시 × 20 spike 에서도 p95 < 800ms, error < 1% 를 유지한다.

## 결과

- p50 / p95 / p99: ...
- 실패율: ...
- DB connection 사용량 peak: ...

## 후속 액션

- (예) HikariCP maximum-pool-size 를 10 → 20 으로 상향 검토.
- (예) 신청 form validate 의 N+1 가능성 확인 — 별도 issue.

## 첨부

- thresholds.json
- grafana-link.md
```

### 운영 시 주의사항

- staging DB 가 운영 DB 의 mini 복제본이 아니어서, 결과를 운영 RPS 한도로 직접 환산할 수 없다. 두 환경의 인스턴스 사양 차이 보정값 (대략 0.5~0.7) 을 summary.md 에 명시한다.
- 동일 endpoint 라도 시드 데이터의 row 수에 따라 latency 가 크게 달라지므로, summary.md 의 "시드 데이터 버전" 필드를 빠뜨리면 비교가 오염된다.
- spike 시나리오의 `preAllocatedVUs` 가 부족하면 k6 자체가 병목이 되어 측정값이 왜곡된다 — 운영자가 스크립트를 추가할 때 단계별 RPS 와 VU 의 관계를 확인해야 한다.
- 부하 생성기 (k6 가 도는 노드) 의 CPU / 네트워크 포화 여부도 결과의 일부이므로, 실행 시점의 부하 노드 메트릭을 같이 캡처한다.

## References

- 관련 ADR
    - [ADR-011: Inquiry 도메인과 WebSocket/STOMP 도입](011-inquiry-domain-with-websocket-stomp.md) — WebSocket 부하 테스트는 본 ADR 의 1차 범위 밖, 별도 ADR 후보.
    - [ADR-008: LLM 도메인 구현 (Spring AI + Gemini)](008-llm-domain-provider-strategy.md) — figma + LLM Tier 3 도메인의 호출량 가정.
- 외부 자료
    - [Grafana k6 Documentation](https://grafana.com/docs/k6/latest/)
    - [k6 Test Types: smoke / load / stress / spike / soak / breakpoint](https://grafana.com/docs/k6/latest/testing-guides/test-types/)
    - [k6 Prometheus Remote Write output](https://grafana.com/docs/k6/latest/results-output/real-time/prometheus-remote-write/)
    - [k6 thresholds](https://grafana.com/docs/k6/latest/using-k6/thresholds/)
