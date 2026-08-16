# {{SLUG}}

> 한 줄 요약: (실행 후 채움 — 예: "home 300rps p95 180ms 안정. 520rps 부터 Hikari pending 발생 → 한계는 DB 커넥션")

## 가설 — 실행 전에 쓴다

- 확인하려는 것:
- 예상:

## 환경 — 재현에 필요한 전부 (스크립트가 자동 기입)

| 항목 | 값 |
|---|---|
| 날짜 | {{DATE}} |
| 커밋 | {{COMMIT}} |
| 앱 이미지 | {{APP_IMAGE}} |
| SUT / DB 스펙 | {{SUT_TYPE}} / {{DB_CLASS}} |
| 시딩 | SEED_STRATEGY= BULK_MEMBER_COUNT= BULK_RANDOM_SEED= |
| 실행 명령 | `loadtest/scripts/run-k6.sh <profile> <scenario> <rate> <duration>` |

## 결과 — k6 요약 + 핵심 수치

(RUN_DIR 로 실행했다면 같은 디렉터리의 `k6-*.log` / `k6-*.summary.json` 이 원본 — 표는 거기서 발췌)

| 지표 | 값 | 판정 (threshold) |
|---|---|---|
| TPS (실효) | | |
| p95 / p99 | | p95<800ms |
| 에러율 | | <1% |

(Grafana 스크린샷: 포화점 뷰 필수, 특이했던 행 추가)

## 관찰 — 대시보드 행별로 본 것

- ② 앱 (Hikari·Tomcat·JVM):
- ③ DB 내부 (커넥션·락·캐시):
- ④ 호스트 USE (SUT vs generator):
- ⑤ RDS 호스트:

## 결론 / 다음 액션

-
