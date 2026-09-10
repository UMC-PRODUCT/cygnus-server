# 부하 테스트 — 구조와 결정 기록

실행 중인 앱의 주소를 지정해 k6 시나리오를 실행한다.

현재 이 디렉터리에는 **두 개의 하네스**가 공존한다:

1. **공통 k6 하네스** (`k6/` + `scripts/prepare-data.sh`) — 지정한 앱에서 breakpoint·spike 등 한계 실측. 이 문서의 본체.
2. **notice 전/후 비교 하네스** (`run.sh` + `lib/` + `scenarios/notice/`) — 로컬에서 notice 성능 리팩토링 전/후를 동일 부하로 비교하는 미니 하네스. [아래 별도 섹션](#notice-전후-비교-하네스-runsh) 참조.

두 하네스는 진입점·시나리오·결과 기록 방식이 다르다. 시나리오가 늘어나면 공통 k6 하네스(`k6/`) 쪽으로 통합하는 것이 방향이다.

| 위치 | 역할 |
|------|------|
| `loadtest/k6/` | 공통 k6 실행 코드 (구조는 `loadtest/k6/README.md`) |
| `loadtest/scripts/prepare-data.sh` | API 시딩 후 `seed.json` 생성 |
| `loadtest/scripts/new-run.sh` | 실행 기록 템플릿 생성 |
| `loadtest/run.sh` · `lib/` · `scenarios/notice/` | notice 전/후 비교 미니 하네스 (로컬 실행) |
| `loadtest/compare.sh` | 두 실행(summary.json)의 p95/p99/에러율/RPS 비교 (jq 필요) |
| `docs/loadtest/runs/` | 실행 기록 (가설→환경→결과→결론) |
| `docs/adr/013` | 도구 선택·시나리오 우선순위의 원 전략 |

## 공통 k6 실행

저장소 루트에서 실행한다. 시딩이 필요한 시나리오는 테스트 API에 접근할 수 있는 앱 주소로 데이터를 먼저 준비한다.

```bash
SUT_URL=http://localhost:8080 loadtest/scripts/prepare-data.sh

cd loadtest/k6
mkdir -p out
BASE_URL=http://localhost:8080 PROFILE=smoke SCENARIO=health-check k6 run script.js
```

`health-check`는 시딩 없이 실행할 수 있다. `BASE_URL`을 대상 앱 주소로 바꾸고,
`PROFILE`, `SCENARIO`, `RATE`, `DURATION`으로 부하를 선택한다. 요약은 `out/last-summary.json`에 저장된다.

## 핵심 결정 기록

각 항목: **비교군 → 결정 → 이유.** 상세 근거가 더 필요하면 ADR-013 과 커밋 로그.

### 도구

- 비교: k6 vs nGrinder vs Gatling vs Locust (ADR-013)
- 결정: **k6**
- 이유: 본 시스템 spike 는 수백~수천 RPS 라 단일 노드로 충분. 코드 기반 시나리오 + thresholds 로 SLO 를 스크립트에 명시.

### 시나리오 선택

- 비교: 엔드포인트별 벤치마크 vs 사용자 행동 단위 / 어느 도메인부터 / 단일 vs 혼합 시작
- 결정: **home 부터, 성분(단일 시나리오 breakpoint) → 통합(spike·혼합) 순서**
- 이유:
  - UMC 트래픽은 **이벤트 구동** — 지원 오픈·공지 푸시·출석 순간에 집중 유입되고 평시는 한산하다. 위험은 평균 부하가 아니라 순간 폭주다.
  - home 은 전 유저 공통 진입점 + 1행동=3콜(부하 배수) + 최다 조인 집계(member/me) — **가장 넓고 가장 무거운 경로 하나**로 시작해 커버리지 대비 효율을 최대화.
  - 혼합에서 문제가 나오면 원인 분리가 안 된다 — **성분별 한계 실측이 먼저**, 혼합·spike 는 성분 수치가 있어야 해석 가능.
  - SLO 가 없는 상태라 breakpoint(점증)로 한계 처리량부터 실측 — 목표는 실측에서 역산한다.

### 부하 방식

- 비교: VU 고정(constant-vus) vs 도착률 고정(constant-arrival-rate) vs 점증(ramping) vs 급등(spike)
- 결정: **smoke 만 VU, 본 측정은 arrival-rate, 포화점 탐색은 breakpoint(점증), 이벤트 폭주 재현은 spike(급등+회복)**
- 이유: VU 방식은 SUT 가 느려지면 요청도 같이 줄어 한계가 낙관적으로 나온다(coordinated omission). 도착률 고정은 "SUT 가 얼마나 받아내나"를 정직하게 잰다. 한계 지점은 breakpoint 로 1→RATE 점증(thresholds 없음 — 언제 깨지는가가 결과). **spike 는 breakpoint 와 다른 테스트다** — 같은 rate 라도 완만 점증은 버티고 10초 급등은 죽을 수 있다(풀·JIT·캐시가 식은 상태에서 맞기 때문). 급등 후 rate 0 구간을 둬 회복까지 관찰한다.

### 시딩

- 비교: ① api 시더(SeedController) ② 손으로 SQL/pg_dump 아티팩트 ③ Spring 벌크 시더(JDBC)
- 결정: **① api(기본, ~수천) + ③ bulk(10만+). ② pg_dump 는 폐기.**
- 이유:
  - 원칙은 **생성기(코드)가 원천, 데이터 파일은 캐시** — 덤프/INSERT 파일을 관리 원천으로 삼으면 스키마 변경마다 조용히 썩는다.
  - api 시더는 도메인 가드를 통과해 스키마 변경에 자동 대응하지만 HTTP 왕복이라 10만+ 은 비현실적.
  - bulk 는 앱 이미지를 `seeder` 프로파일로 1회 실행해 JDBC 배치로 적재 — 전 엔티티가 IDENTITY 라 JPA 배치가 무력화되므로 JdbcTemplate 직행. 고정 seed(RNG)로 같은 seed = 같은 데이터(실행 간 비교 가능), 상위 5개 학교 50% 스큐(균등 데이터는 결과를 낙관 왜곡). 빈 DB 전제.
- 주의: `BulkSeedJdbcAdapter` 의 SQL 은 도메인 가드를 우회한 스키마 강결합(속도 트레이드오프) — **Flyway 마이그레이션 PR 에서 같이 검토.**

### seed.json 계약

- 비교: k6 setup() 에서 직접 시딩 vs 시더 산출물을 파일로
- 결정: **k6 는 시딩하지 않고 seed.json 을 읽기만 한다** (Gatling feeder 와 같은 표준 패턴)
- 이유: 시딩 실패/지연이 부하 실행에 섞이면 측정이 오염된다. api/bulk 어느 시더든 **같은 스키마의 seed.json 을 산출해야 한다는 것이 유일한 계약** — k6 는 누가 만들었는지 모른다.

### 관측

- 비교: (a) 이중축 차트 vs 세로 스택+공유 크로스헤어 (b) k6 기본 태그 그대로 vs 제한
- 결정: **대시보드 1장 5행**(헤더 스탯 / ①k6 판정 / ②SUT 앱 / ③DB 내부 / ④호스트 USE / ⑤RDS 호스트 USE), **이중축 금지**, k6 `systemTags` 에서 **url·name 제거**
- 이유: 포화점은 TPS·응답시간·VUs 를 같은 세로선(공유 크로스헤어)으로 읽는 게 이중축 착시보다 정확. url 태그는 쿼리스트링(memberId 등) 때문에 시리즈가 요청 수만큼 폭발(카디널리티) — 시나리오 구분은 커스텀 `scenario` 태그로 충분. RDS 호스트 지표는 postgres-exporter 가 못 보므로 CloudWatch(monitoring role 권한)가 원천.

## 확장 — 새 시나리오 추가

1. **k6 (항상)** — `scenarios/<profile>/<이름>.js` + `script.js` REGISTRY 한 줄. 상세: `loadtest/k6/README.md`
2. **시드 데이터 (필요할 때만, 규모로 분기)**
   - 소규모 → SeedController 에 이미 있는지 확인(SEED-001~007). 없으면 SEED-007 패턴 복제 후 `prepare-data.sh` 에 호출 스텝. **도메인 지식은 Java 에, bash 는 순서만.**
   - 대규모 → `BulkSeedService` 에 seedXxx 단계. 새 테이블당 3곳: Row record → 포트 메서드+어댑터 SQL → 서비스 생성 로직(기존 rng 재사용). `ANALYZE_TABLES`·Result 카운트 갱신.
   - 벌크는 시나리오별 픽스처가 아니라 **공유 월드 1개** — 필요 없는 모양은 `app.bulk-seed.*` 0 으로 끄고, 새 모양은 월드에 추가.
3. **k6 가 고를 ID (필요할 때만)** — seed.json `targets` + `lib/data.js` pick 함수 (`pickProjectId` 참조).

---

## notice 전/후 비교 하네스 (run.sh)

**notice 도메인 성능 리팩토링의 전/후 비교** 전용 미니 하네스. 리그 없이 로컬(bootRun + docker-compose PostgreSQL)에서 돌린다.

```text
loadtest/
├── run.sh          # 실행 래퍼: 사전조건 확인 + k6 버전/GitSHA/env 기록 + 결과 디렉토리 생성
├── compare.sh      # 두 실행(summary.json)의 p95/p99/에러율/RPS 비교 (jq 필요)
├── lib/            # config.js(env 주입) / auth.js(setup 토큰 발급) / checks.js / summary.js(handleSummary)
└── scenarios/notice/
    ├── read-status.smoke.js   # VU=1, 1분 — 정상 동작 확인
    └── read-status.load.js    # 평시 부하 — 전/후 비교 본체
```

결과는 `docs/loadtest/runs/<YYYY-MM-DD-HHmmss>-<label>/` 에 `summary.md`·`summary.json`·`run-meta.txt` 로 자동 저장된다(시각 포함 → 재실행해도 덮어쓰지 않음).

### 실행

인증 우선순위는 `K6_TOKEN > K6_MEMBER_ID > K6_EMAIL/K6_PASSWORD`. local 에서는 `K6_MEMBER_ID`(권장)로 `GET /test/token/access?memberId=` 를 통해 비밀번호 없이 토큰을 발급한다. 대상 공지·챌린저 데이터는 시딩 API(ADR-017)로 준비한다.

```bash
# smoke — 먼저 토큰/경로/응답 정상 확인
K6_MEMBER_ID=1 K6_NOTICE_ID=1 \
  loadtest/run.sh loadtest/scenarios/notice/read-status.smoke.js notice-read-status-smoke

# load — 전/후 비교 본체
K6_MEMBER_ID=1 K6_NOTICE_ID=1 K6_RATE=20 \
  loadtest/run.sh loadtest/scenarios/notice/read-status.load.js notice-read-status-before

# 리팩토링 후 동일 조건으로 재실행 → -after

# 전/후 비교 (run.sh 가 출력한 실제 디렉토리 경로 사용)
loadtest/compare.sh \
  docs/loadtest/runs/<...>-before \
  docs/loadtest/runs/<...>-after
```

주요 env:

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `K6_BASE_URL` | `http://localhost:8080` | 대상 서버 origin |
| `K6_NOTICE_ID` | `1` | 테스트 대상 공지 ID |
| `K6_MEMBER_ID` | (없음) | (권장, local) `/test/token/access` 로 토큰 발급할 memberId |
| `K6_EMAIL` / `K6_PASSWORD` | (없음) | 이메일 로그인 계정 (test 토큰 API 가 없는 staging 등) |
| `K6_TOKEN` | (없음) | 직접 주입 시 발급 과정 생략 |
| `K6_TOKEN_TTL_MIN` | (없음) | 발급 토큰 만료(분). soak 시 테스트 길이보다 크게 |
| `K6_RATE` | `20` | (load) 목표 RPS |
| `K6_DURATION` | `2m` | (load) 평시 부하 유지 구간 |
| `K6_PREALLOC_VUS` / `K6_MAX_VUS` | `50` / `200` | (load) arrival-rate 용 VU 풀 |
| `K6_TESTID` | `local-adhoc` | 실행 식별 태그(전/후 구분) |

### 부하 모델과 지표 범위

- executor 는 `ramping-arrival-rate` — 위 "부하 방식" 결정과 동일한 이유(coordinated omission 회피). 전/후를 **동일 부하**에서 비교하는 것이 목적이므로 `K6_RATE`·`K6_DURATION`·시드 규모를 두 실행에서 동일하게 고정한다.
- **k6 로 측정되지 않는 것**: 요청당 SQL 실행 수, DB connection 사용량 등 서버 내부 지표. **P6Spy 로그 / Actuator / Prometheus** 에서 별도 확인해 각 `summary.md` 의 "서버측 지표" 칸에 기입한다. (P0 리팩토링의 "쿼리 수 감소" 근거는 여기서 나온다.)
