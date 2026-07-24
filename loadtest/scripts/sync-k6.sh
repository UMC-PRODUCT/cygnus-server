#!/usr/bin/env bash
set -euo pipefail

# loadtest/k6 를 generator EC2 로 rsync 한다.
# generator user-data 가 부팅 시 repo 를 clone 하지 않으므로, 지금 checkout 의 k6 스크립트를 그대로 올린다.
#
# 사용법: loadtest/scripts/sync-k6.sh
#   SSH_KEY  : (선택) SSH private key 경로. 미설정이면 ssh 기본 키를 쓴다.
#   SSH_USER : (선택) 기본 ec2-user

REPO_ROOT="$(git -C "$(dirname "$0")" rev-parse --show-toplevel)"
TF_DIR="$REPO_ROOT/loadtest/terraform"
K6_DIR="$REPO_ROOT/loadtest/k6"
SSH_USER="${SSH_USER:-ec2-user}"

GENERATOR_IP="$(terraform -chdir="$TF_DIR" output -raw generator_public_ip)"
if [ -z "$GENERATOR_IP" ]; then
  echo "generator_public_ip 를 terraform output 에서 읽지 못했습니다. apply 가 끝났는지 확인하세요." >&2
  exit 1
fi

SSH_OPTS="-o StrictHostKeyChecking=accept-new"
if [ -n "${SSH_KEY:-}" ]; then
  SSH_OPTS="$SSH_OPTS -i $SSH_KEY"
fi

echo "sync: $K6_DIR/ -> $SSH_USER@$GENERATOR_IP:/home/ec2-user/k6/"
# RUN.md 는 generator user-data 가 만든 실행 힌트라, out/ 은 k6 요약 산출물이라 삭제하지 않는다.
rsync -az --delete \
  --exclude '.gitignore' \
  --exclude 'README.md' \
  --exclude 'RUN.md' \
  --exclude 'out/' \
  -e "ssh $SSH_OPTS" \
  "$K6_DIR/" "$SSH_USER@$GENERATOR_IP:/home/ec2-user/k6/"

echo "done."
