#!/usr/bin/env bash
# 두 실행 결과(summary.json)의 핵심 지표를 나란히 비교한다.
# jq 가 필요하다. 없으면 각 디렉토리의 summary.md 를 직접 비교하면 된다.
#
# usage: loadtest/compare.sh <before_dir> <after_dir>
# 예:    loadtest/compare.sh \
#          docs/loadtest/runs/2026-07-27-notice-read-status-before \
#          docs/loadtest/runs/2026-07-27-notice-read-status-after
set -euo pipefail

[ $# -eq 2 ] || { echo "usage: loadtest/compare.sh <before_dir> <after_dir>"; exit 1; }
BEFORE="$1"; AFTER="$2"

command -v jq >/dev/null 2>&1 || {
  echo "[안내] jq 가 없어 자동 비교를 건너뜁니다. 각 summary.md 를 직접 비교하세요:";
  echo "  ${BEFORE}/summary.md"; echo "  ${AFTER}/summary.md"; exit 1;
}
for d in "$BEFORE" "$AFTER"; do
  [ -f "$d/summary.json" ] || { echo "[에러] $d/summary.json 없음"; exit 1; }
done

metric() { # dir metric key
  jq -r --arg m "$2" --arg k "$3" '.metrics[$m].values[$k] // "n/a"' "$1/summary.json"
}

printf "%-26s %-16s %-16s\n" "지표" "before" "after"
printf "%-26s %-16s %-16s\n" "--------------------------" "----------------" "----------------"
# label metric key
while IFS='|' read -r label m k; do
  b=$(metric "$BEFORE" "$m" "$k")
  a=$(metric "$AFTER" "$m" "$k")
  printf "%-26s %-16s %-16s\n" "$label" "$b" "$a"
done <<'ROWS'
p95 (ms)|endpoint_duration|p(95)
p99 (ms)|endpoint_duration|p(99)
avg (ms)|endpoint_duration|avg
success rate|endpoint_success|rate
http_req_failed|http_req_failed|rate
rps (http_reqs/s)|http_reqs|rate
dropped_iterations|dropped_iterations|count
ROWS
