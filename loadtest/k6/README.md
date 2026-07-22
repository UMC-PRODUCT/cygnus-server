# loadtest/k6

부하 테스트 실행 코드. 문서가 아니라 러너가 실제로 쓰는 k6 스크립트다.
(실행 결과·분석 문서는 `docs/loadtest/` 에 둔다. 전략은 ADR-013 참조.)

## 구조

- `script.js` — 단일 entrypoint. `PROFILE`(부하 유형) × `SCENARIO`(업무 시나리오)로 실행 대상을 고른다.
- `config/profiles.js` — smoke/load/stress/soak 별 options·thresholds·기본 RATE/DURATION.
- `lib/` — `http`(공통 요청/체크), `auth`(부하 전용 토큰 발급+캐시), `data`(seed.json 로드), `metrics`(시나리오 태그).
- `scenarios/{smoke,load,stress,soak}/*.js` — 업무 시나리오. 각 파일은 `requiresSeed` 와 default 함수를 export 한다.
- `data/seed.json` — `prepare-data.sh` 산출물(gitignore). 형태는 `seed.example.json` 참조.

## 실행

generator EC2 에서 `run-umc-k6` 로 실행한다. 로컬에서는 `loadtest/scripts/run-k6.sh` 가 sync 후 원격 실행한다.

```bash
run-umc-k6 <profile> <scenario> <rate> <duration>

run-umc-k6 smoke  health-check 1    1m     # 시딩 불필요
run-umc-k6 smoke  project-read 1    1m     # seed.json 필요
run-umc-k6 load   project-read 300  10m
run-umc-k6 stress project-read 1000 20m
run-umc-k6 soak   project-read 100  2h
```

## 시딩

k6 는 시딩하지 않는다. 시딩은 `loadtest/scripts/prepare-data.sh` 가 소유하고 `data/seed.json` 을 만든다.
`requiresSeed = true` 시나리오는 seed.json 이 없으면 init 단계에서 실패한다 — 먼저 `prepare-data.sh` 를 돌리거나 `seed.example.json` 을 복사한다.

## 시나리오 추가

`scenarios/<profile>/<name>.js` 에 `export const requiresSeed` 와 default 함수를 만들고,
`script.js` 의 `REGISTRY` 에 한 줄 등록한다. 인증이 필요하면 `lib/auth.js` 를 쓴다.

새 시드 데이터가 필요하면(시드 API 추가·bulk 시더 확장·seed.json targets)
`loadtest/README.md` 의 "새 시나리오 추가 (확장 가이드)" 를 따른다.
