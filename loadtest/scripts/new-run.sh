#!/usr/bin/env bash
set -euo pipefail

# 부하 실행 기록 스캐폴드. docs/loadtest/runs/<날짜>-<이름>/summary.md 를 템플릿에서 만들고
# 날짜·커밋을 자동으로 채우고 나머지 환경 정보는 직접 기입한다.
#
# 사용법: loadtest/scripts/new-run.sh <이름>
#   예:   loadtest/scripts/new-run.sh home-breakpoint-500
# 실행 "전"에 만들어 가설부터 적는 것을 권장한다 (가설 없는 실행은 관광이지 실험이 아니다).

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TEMPLATE="$REPO_ROOT/docs/loadtest/runs/_template/summary.md"

SLUG="${1:?이름 필요 (예: home-breakpoint-500)}"
DIR="$REPO_ROOT/docs/loadtest/runs/$(date +%F)-$SLUG"

[ -e "$DIR/summary.md" ] && {
  echo "이미 있음: $DIR/summary.md" >&2
  exit 1
}
mkdir -p "$DIR"

sed \
  -e "s|{{SLUG}}|$SLUG|g" \
  -e "s|{{DATE}}|$(date +%F)|g" \
  -e "s|{{COMMIT}}|$(git -C "$REPO_ROOT" rev-parse --short HEAD)|g" \
  "$TEMPLATE" >"$DIR/summary.md"

echo "$DIR/summary.md"
echo "" >&2
echo "환경·실행 명령을 직접 기입하고 실행 로그·요약 JSON을 이 디렉터리에 보관하세요." >&2
