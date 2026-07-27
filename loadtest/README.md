# loadtest — k6 부하 테스트 하네스

[ADR-013](../docs/adr/013-k6-load-and-performance-testing-strategy.md)을 기반으로 한 최소 하네스입니다.
현재는 **notice 도메인 성능 리팩토링의 전/후 비교**를 위해 `notice/read-status` 시나리오만 구현돼 있습니다.

> **디렉토리 구조 참고:** ADR-013 은 `scenarios/{smoke,load}/` + `domains/` 분리를 제안하지만,
> 도메인이 하나뿐인 현재 규모에서는 **도메인별 디렉토리 + `<대상>.<type>.js` 네이밍**
> (`scenarios/notice/read-status.load.js`)이 더 단순해 이 방식을 택했습니다. 시나리오/도메인이
> 늘어나면 ADR 의 `smoke|load|...` 분리로 재정렬하고 ADR 을 실제 구조에 맞게 갱신합니다.

## 구조

```text
loadtest/
├── run.sh          # 실행 래퍼: 사전조건 확인 + k6 버전/GitSHA/env 기록 + 결과 디렉토리 생성
├── compare.sh      # 두 실행(summary.json)의 p95/p99/에러율/RPS 비교 (jq 필요)
├── lib/
│   ├── config.js   # BASE_URL / NOTICE_ID / 인증정보를 env 로 주입
│   ├── auth.js     # setup()에서 토큰 발급 (하드코딩 제거)
│   ├── checks.js   # 공통 check + 커스텀 메트릭(endpoint_duration, endpoint_success)
│   └── summary.js  # handleSummary: 종료 시 summary.md/json 자동 생성
└── scenarios/
    └── notice/
        ├── read-status.smoke.js   # VU=1, 1분 — 정상 동작 확인
        └── read-status.load.js    # 평시 부하 — 전/후 비교 본체

../docs/loadtest/runs/   # 실행 결과 보존 (실행당 디렉토리: summary.md/json + run-meta.txt)
```

## 사전 준비 (local 기준)

1. **DB 기동** — docker-compose 의 PostgreSQL (호스트 포트 54321)

   ```bash
   docker compose up -d umc-product-postgres
   ```

2. **앱 기동** — bootRun (포트 8080)

   ```bash
   ./gradlew bootRun
   ```

3. **인증 토큰** — `notice/{id}/read-status`는 해당 공지에 CHECK 권한이 있는 계정의 토큰이
   필요합니다. local 에서는 `K6_MEMBER_ID`(권장)로 `GET /test/token/access?memberId=` 를 통해
   비밀번호 없이 발급합니다. 대상 공지·챌린저 데이터는 시딩 API(ADR-017)로 준비합니다.

## 실행

환경변수로 대상·인증을 주입합니다. 인증 우선순위는 `K6_TOKEN > K6_MEMBER_ID > K6_EMAIL/K6_PASSWORD` 입니다.
(Git Bash 등 bash 환경 기준. `run.sh` 가 결과 디렉토리·메타 기록을 처리합니다.)

```bash
# smoke — 먼저 이걸로 토큰/경로/응답이 정상인지 확인 (local: memberId 로 토큰 발급)
K6_MEMBER_ID=1 K6_NOTICE_ID=1 \
  k6 run loadtest/scenarios/notice/read-status.smoke.js

# load (권장: run.sh) — 전/후 비교 본체. 결과가 docs/loadtest/runs/<날짜>-<label>/ 에 자동 저장
K6_MEMBER_ID=1 K6_NOTICE_ID=1 K6_RATE=20 \
  loadtest/run.sh loadtest/scenarios/notice/read-status.load.js notice-read-status-before

# 리팩토링 후 동일 조건으로 재실행
K6_MEMBER_ID=1 K6_NOTICE_ID=1 K6_RATE=20 \
  loadtest/run.sh loadtest/scenarios/notice/read-status.load.js notice-read-status-after

# 전/후 비교
loadtest/compare.sh \
  docs/loadtest/runs/2026-07-27-notice-read-status-before \
  docs/loadtest/runs/2026-07-27-notice-read-status-after
```

주요 env:

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `K6_BASE_URL` | `http://localhost:8080` | 대상 서버 origin |
| `K6_NOTICE_ID` | `1` | 테스트 대상 공지 ID |
| `K6_MEMBER_ID` | (없음) | (권장, local) `/test/token/access` 로 토큰 발급할 memberId |
| `K6_EMAIL` / `K6_PASSWORD` | (없음) | 이메일 로그인 계정 (test 토큰 API 가 없는 staging 등) |
| `K6_TOKEN` | (없음) | 직접 주입 시 발급 과정 생략 |
| `K6_TOKEN_TTL_MIN` | (없음) | 발급 토큰 만료(분). soak 시 테스트 길이보다 크게(예: `120`) |
| `K6_RATE` | `20` | (load) 목표 RPS |
| `K6_DURATION` | `2m` | (load) 평시 부하 유지 구간 |
| `K6_PREALLOC_VUS` / `K6_MAX_VUS` | `50` / `200` | (load) arrival-rate 용 VU 풀 |
| `K6_TESTID` | `local-adhoc` | 실행 식별 태그(전/후 구분) |

## 부하 모델 (load)

- **executor: `ramping-arrival-rate`** — VU 수가 아니라 **목표 RPS(도착률)** 로 부하를 정의한다.
  응답 시간이 변해도 제공 부하가 일정하므로, 리팩토링 전/후를 **동일 부하**에서 비교할 수 있다.
  (`ramping-vus + sleep` 은 응답이 빨라질수록 실제 RPS 가 올라가 비교가 오염된다.)
- **기본 20 RPS** 는 read-status 가 운영진 조회 엔드포인트(ADR-013 Tier 3, 평시 트래픽 작음)라는
  가정에 근거한 임시값이다. 실제 capacity 측정 시 `K6_RATE` 로 조정하고 근거를 summary.md 에 남긴다.
- `dropped_iterations` 임계로 **부하 생성기가 목표 RPS 를 못 따라가는지**(maxVUs 부족/서버 과포화) 감시한다.
- 장기 실행(soak): 아직 soak 시나리오는 없다. 추가 시 `K6_TOKEN_TTL_MIN` 을 테스트 길이보다 크게 주면
  local(test 토큰 API) 에서는 만료 없이 돈다. staging 로그인 기반 장기 soak 은 토큰 갱신 로직이 별도로
  필요하다(향후 확장).

## 전/후 비교 워크플로우

1. 리팩토링 **전** 상태에서 `run.sh` 로 load 실행 → `docs/loadtest/runs/<날짜>-...-before/` 에
   `summary.json` + `summary.md` + `run-meta.txt` 자동 생성. 생성된 `summary.md` 의 가설/시드 버전 칸을 채운다.
2. 리팩토링 적용
3. **후** 상태에서 **같은 시드·같은 옵션(K6_RATE·K6_DURATION 동일)** 으로 재실행 → `...-after/`
4. `compare.sh` 로 **p95 / p99 / 에러율 / 실측 RPS** 를 나란히 비교

### 지표 범위 (중요)

- **k6 로 측정되는 것**: 응답 시간(p95/p99/avg), 성공률, HTTP 실패율, 실측 RPS, dropped_iterations, checks.
- **k6 로 측정되지 않는 것**: **요청당 SQL 실행 수, DB connection 사용량 등 서버 내부 지표**.
  이 값들은 k6 summary 에 포함되지 않으므로 **P6Spy 로그 / Actuator / Prometheus** 에서 별도로 확인해
  각 `summary.md` 의 "서버측 지표" 칸에 기입한다. (P0 리팩토링의 "쿼리 수 감소" 근거는 여기서 나온다.)

> 비교가 오염되지 않으려면 시드 데이터 규모(대상 챌린저 수)와 목표 RPS 를 두 실행에서 동일하게 고정해야 합니다.
> 특히 read-status 의 성능 개선 효과는 회원 수가 클수록 크게 드러납니다.
