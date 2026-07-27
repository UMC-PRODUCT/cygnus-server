# 부하 테스트 결과 인덱스

[ADR-013](../adr/013-k6-load-and-performance-testing-strategy.md) §2 에 따라, 부하 테스트 실행 결과를
`runs/<YYYY-MM-DD-label>/` 아래에 **실행 단위 디렉터리**로 영구 보존합니다.

각 실행 디렉터리에는 최소 다음을 둡니다.

- `summary.md` — 환경 / 가설 / 결과 요약 / 후속 액션 (raw 숫자만이 아니라 사고 과정을 남김)
- `summary.json` — `k6 run --summary-export` 로 생성한 raw 결과

스크립트와 실행법은 [loadtest/README.md](../../loadtest/README.md) 참고.

## 실행 목록

| 실행 | 도메인 | 시나리오 | 목적 |
|------|--------|----------|------|
| _(아직 없음)_ | notice | read-status load | P0 리팩토링 전/후 비교 예정 |
