#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:?AWS_REGION is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${SSM_PARAMETER_PATH:?SSM_PARAMETER_PATH is required}"
: "${GITHUB_ENV:?GITHUB_ENV is required}"

base64_encode_file() {
  local file_path="$1"

  if base64 -w 0 "${file_path}" >/dev/null 2>&1; then
    base64 -w 0 "${file_path}"
  else
    base64 < "${file_path}" | tr -d '\n'
  fi
}

if [[ -z "${AWS_ACCOUNT_ID:-}" ]]; then
  AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
fi

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_PASSWORD="$(aws ecr get-login-password --region "${AWS_REGION}")"
APP_ENV_FILE="$(mktemp)"
cleanup() {
  rm -f "${APP_ENV_FILE}"
}
trap cleanup EXIT

scripts/download-ssm-env.sh \
  --path "${SSM_PARAMETER_PATH}" \
  --output "${APP_ENV_FILE}" \
  --region "${AWS_REGION}" \
  --overwrite

while IFS='=' read -r env_key env_value; do
  [[ -n "${env_key}" && "${env_key}" != \#* ]] || continue
  echo "::add-mask::${env_value}"
done < "${APP_ENV_FILE}"

APP_ENV_B64="$(base64_encode_file "${APP_ENV_FILE}")"

echo "::add-mask::${ECR_PASSWORD}"
echo "::add-mask::${APP_ENV_B64}"

{
  echo "ECR_REGISTRY=${ECR_REGISTRY}"
  echo "ECR_IMAGE_NAME=${ECR_REGISTRY}/${ECR_REPOSITORY}"
  echo "ECR_PASSWORD=${ECR_PASSWORD}"
  echo "APP_ENV_B64=${APP_ENV_B64}"
} >> "${GITHUB_ENV}"
