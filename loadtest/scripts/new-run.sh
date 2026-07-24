#!/usr/bin/env bash
set -euo pipefail

# 부하 실행 기록 스캐폴드. docs/loadtest/runs/<날짜>-<이름>/summary.md 를 템플릿에서 만들고
# 재현에 필요한 환경(커밋·이미지·인스턴스 스펙)을 자동으로 채운다.
#
# 사용법: loadtest/scripts/new-run.sh <이름>
#   예:   loadtest/scripts/new-run.sh home-breakpoint-500
# 실행 "전"에 만들어 가설부터 적는 것을 권장한다 (가설 없는 실행은 관광이지 실험이 아니다).

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TFVARS="$REPO_ROOT/loadtest/terraform/terraform.tfvars"
TEMPLATE="$REPO_ROOT/docs/loadtest/runs/_template/summary.md"

SLUG="${1:?이름 필요 (예: home-breakpoint-500)}"
DIR="$REPO_ROOT/docs/loadtest/runs/$(date +%F)-$SLUG"

[ -e "$DIR/summary.md" ] && {
  echo "이미 있음: $DIR/summary.md" >&2
  exit 1
}
mkdir -p "$DIR"

tfvar() { # tfvars 문자열 값 (없으면 기본값 $2)
  local v=""
  [ -f "$TFVARS" ] && v="$(sed -n "s/^$1[[:space:]]*=[[:space:]]*\"\(.*\)\"/\1/p" "$TFVARS" | head -1)"
  echo "${v:-$2}"
}

sed \
  -e "s|{{SLUG}}|$SLUG|g" \
  -e "s|{{DATE}}|$(date +%F)|g" \
  -e "s|{{COMMIT}}|$(git -C "$REPO_ROOT" rev-parse --short HEAD)|g" \
  -e "s|{{APP_IMAGE}}|$(tfvar app_image '(tfvars 미설정)')|g" \
  -e "s|{{SUT_TYPE}}|$(tfvar sut_instance_type t4g.small)|g" \
  -e "s|{{DB_CLASS}}|$(tfvar db_instance_class db.t4g.small)|g" \
  "$TEMPLATE" >"$DIR/summary.md"

echo "$DIR/summary.md"
