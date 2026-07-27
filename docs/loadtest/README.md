# 부하 테스트 결과 인덱스

[ADR-013](../adr/013-k6-load-and-performance-testing-strategy.md) §2 에 따라, 부하 테스트 실행 결과를
`runs/<YYYY-MM-DD-HHmmss>-<label>/` 아래에 **실행 단위 디렉터리**로 영구 보존합니다.
(디렉터리명에 시각을 포함해 같은 날 같은 label 로 재실행해도 이전 결과가 덮어써지지 않습니다.)

각 실행 디렉터리에는 다음이 생성됩니다. `loadtest/run.sh` 로 실행하면 모두 자동 생성됩니다.

- `summary.md` — 환경 / 가설 / 결과 요약 / 후속 액션 (시나리오의 `handleSummary` 가 자동 생성, 가설·시드 칸은 수기 보완)
- `summary.json` — k6 raw 결과 (`handleSummary` 가 생성)
- `run-meta.txt` — k6 버전 / Git SHA / 실행 env 요약 (`run.sh` 가 생성)

스크립트와 실행법은 [loadtest/README.md](../../loadtest/README.md) 참고.

## 실행 목록

| 실행 | 도메인 | 시나리오 | 목적 |
|------|--------|----------|------|
| _(아직 없음)_ | notice | read-status load | P0 리팩토링 전/후 비교 예정 |
