#!/usr/bin/env bash
# k6 시나리오를 실행하고 결과를 날짜/라벨 디렉토리에 보존한다.
# k6 버전·Git SHA·환경변수 요약을 run-meta.txt 로 함께 남긴다(재현성).
# summary.md / summary.json 은 시나리오의 handleSummary 가 K6_OUT_DIR 로 자동 생성한다.
#
# usage: loadtest/run.sh <scenario.js> <label> [추가 k6 인자...]
# 예:    K6_MEMBER_ID=1 K6_RATE=20 loadtest/run.sh \
#          loadtest/scenarios/notice/read-status.load.js notice-read-status-before
set -euo pipefail

[ $# -ge 2 ] || { echo "usage: loadtest/run.sh <scenario.js> <label> [k6 args...]"; exit 1; }
SCENARIO="$1"; LABEL="$2"; shift 2

command -v k6 >/dev/null 2>&1 || { echo "[에러] k6 가 설치되어 있지 않습니다."; exit 1; }
[ -f "$SCENARIO" ] || { echo "[에러] 시나리오 파일 없음: $SCENARIO"; exit 1; }

BASE_URL="${K6_BASE_URL:-http://localhost:8080}"
# 사전조건: 대상 서버 응답 확인(있으면). curl 없으면 건너뜀.
if command -v curl >/dev/null 2>&1; then
  curl -fsS -o /dev/null "${BASE_URL}/actuator/health" 2>/dev/null \
    || echo "[경고] ${BASE_URL} 헬스체크 실패 — 앱이 떠 있는지 확인하세요(계속 진행)."
fi

# 시각(HHmmss)까지 포함해 같은 날 같은 label 재실행 시 이전 결과가 덮어써지지 않게 한다.
TS=$(date +%Y-%m-%d-%H%M%S)
OUT="docs/loadtest/runs/${TS}-${LABEL}"
mkdir -p "$OUT"
GITREF=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)

{
  echo "date: $(date -Iseconds 2>/dev/null || date)"
  echo "gitref: ${GITREF}"
  echo "k6: $(k6 version | head -1)"
  echo "scenario: ${SCENARIO}"
  echo "env: BASE_URL=${BASE_URL} NOTICE_ID=${K6_NOTICE_ID:-1} RATE=${K6_RATE:-} DURATION=${K6_DURATION:-} TESTID=${K6_TESTID:-$LABEL}"
} | tee "$OUT/run-meta.txt"

K6_OUT_DIR="$OUT" K6_GITREF="$GITREF" K6_TESTID="${K6_TESTID:-$LABEL}" \
  k6 run "$@" "$SCENARIO"

echo "결과: ${OUT}/ (summary.md, summary.json, run-meta.txt)"
