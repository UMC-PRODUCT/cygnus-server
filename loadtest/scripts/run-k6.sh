#!/usr/bin/env bash
set -euo pipefail

# 로컬에서 k6 스크립트를 sync 한 뒤 generator EC2 에서 원격 실행한다.
# 실제 k6 실행은 generator 에서 일어나고, 결과는 run-umc-k6 의 Prometheus remote-write 로 monitoring 에 적재된다.
#
# 사용법: loadtest/scripts/run-k6.sh <profile> <scenario> <rate> [duration]
#   예: loadtest/scripts/run-k6.sh smoke health-check 1 1m
#       loadtest/scripts/run-k6.sh load  project-read 300 10m
#   SSH_KEY / SSH_USER : sync-k6.sh 와 동일.

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

echo "run-umc-k6 $PROFILE $SCENARIO $RATE $DURATION  @ $SSH_USER@$GENERATOR_IP"
# shellcheck disable=SC2029  # 인자를 원격에서 전개하는 것이 의도다.
ssh $SSH_OPTS "$SSH_USER@$GENERATOR_IP" \
  "run-umc-k6 '$PROFILE' '$SCENARIO' '$RATE' '$DURATION'"
