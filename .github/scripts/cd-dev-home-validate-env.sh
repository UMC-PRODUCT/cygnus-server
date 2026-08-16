#!/usr/bin/env bash
set -euo pipefail

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "필수 환경 변수 검증 시작"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

required_vars=(
  AWS_ACCESS_KEY_ID
  AWS_SECRET_ACCESS_KEY
  SERVER_APP_DIRECTORY
  SERVER_SSH_HOST
  SERVER_SSH_USERNAME
  SERVER_SSH_PRIVATE_KEY
  SERVER_SSH_PORT
  POSTGRES_USER
  POSTGRES_PASSWORD
  AWS_REGION
  ECR_REPOSITORY
  HTTP_PORT
  APP_PORT
  MANAGEMENT_PORT
  IMAGE_TAG
  SECRET_S3_URI
  APP_REPLICAS
  HEALTHCHECK_TIMEOUT
)

missing_vars=()
for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    missing_vars+=("${var_name}")
  fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
  echo "다음 환경 변수들이 설정되지 않았습니다:"
  printf ' - %s\n' "${missing_vars[@]}"
  exit 1
fi

numeric_vars=(HTTP_PORT APP_PORT MANAGEMENT_PORT APP_REPLICAS HEALTHCHECK_TIMEOUT)
for var_name in "${numeric_vars[@]}"; do
  value="${!var_name}"
  if ! [[ "${value}" =~ ^[0-9]+$ ]]; then
    echo "::error::${var_name} must be numeric. current=${value}"
    exit 1
  fi
done

echo "모든 필수 환경 변수가 설정되었습니다."
