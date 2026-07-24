#!/usr/bin/env bash
set -euo pipefail

# 로컬에서 k6 스크립트를 sync 한 뒤 generator EC2 에서 원격 실행한다.
# 실제 k6 실행은 generator 에서 일어나고, 결과는 run-umc-k6 의 Prometheus remote-write 로 monitoring 에 적재된다.
#
# 사용법: loadtest/scripts/run-k6.sh <profile> <scenario> <rate> [duration]
#   예: loadtest/scripts/run-k6.sh smoke health-check 1 1m
#       loadtest/scripts/run-k6.sh load  project-read 300 10m
#   SSH_KEY / SSH_USER : sync-k6.sh 와 동일.
#   RUN_DIR : (선택) 결과(콘솔 로그·요약 JSON) 저장 디렉터리. new-run.sh 가 만든
#             runs/ 디렉터리를 주면 실행 기록 옆에 자동 보관된다. 미설정 시 loadtest/k6/out.

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TF_DIR="$REPO_ROOT/loadtest/terraform"
SSH_USER="${SSH_USER:-ec2-user}"

PROFILE="${1:?profile 필요 (smoke|load|stress|soak|breakpoint)}"
SCENARIO="${2:?scenario 필요 (예: health-check, project-read)}"
RATE="${3:?rate 필요 (예: 1, 300)}"
DURATION="${4:-}"

# 최신 k6 스크립트를 먼저 올린다.
"$(dirname "$0")/sync-k6.sh"

GENERATOR_IP="$(terraform -chdir="$TF_DIR" output -raw generator_public_ip)"

SSH_OPTS="-o StrictHostKeyChecking=accept-new"
if [ -n "${SSH_KEY:-}" ]; then
  SSH_OPTS="$SSH_OPTS -i $SSH_KEY"
fi

# 결과 저장 위치: 콘솔 출력(.log)과 k6 요약(.summary.json)을 실행마다 남긴다.
DEST_DIR="${RUN_DIR:-$REPO_ROOT/loadtest/k6/out}"
mkdir -p "$DEST_DIR"
BASE="k6-$PROFILE-$SCENARIO-$(date +%F-%H%M%S)"

# handleSummary 가 쓸 원격 out/ 디렉터리 보장 (sync 는 out/ 을 건드리지 않는다)
ssh $SSH_OPTS "$SSH_USER@$GENERATOR_IP" "mkdir -p /home/ec2-user/k6/out"

echo "run-umc-k6 $PROFILE $SCENARIO $RATE $DURATION  @ $SSH_USER@$GENERATOR_IP"
# thresholds 초과 시 k6 가 비정상 종료(99)해도 요약은 남으므로, 회수까지 마친 뒤 그 코드로 종료한다.
rc=0
# shellcheck disable=SC2029  # 인자를 원격에서 전개하는 것이 의도다.
ssh $SSH_OPTS "$SSH_USER@$GENERATOR_IP" \
  "run-umc-k6 '$PROFILE' '$SCENARIO' '$RATE' '$DURATION'" | tee "$DEST_DIR/$BASE.log" || rc=$?

scp $SSH_OPTS "$SSH_USER@$GENERATOR_IP:/home/ec2-user/k6/out/last-summary.json" \
  "$DEST_DIR/$BASE.summary.json" 2>/dev/null \
  || echo "(요약 JSON 없음 — k6 가 init 단계에서 실패한 경우)" >&2

echo "결과 저장: $DEST_DIR/$BASE.log / $BASE.summary.json"
exit $rc
