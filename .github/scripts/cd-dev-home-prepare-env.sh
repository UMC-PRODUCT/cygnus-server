#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:?AWS_REGION is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"
: "${SECRET_S3_URI:?SECRET_S3_URI is required}"
: "${GITHUB_ENV:?GITHUB_ENV is required}"

if [[ -z "${AWS_ACCOUNT_ID:-}" ]]; then
  AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
fi

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
ECR_PASSWORD="$(aws ecr get-login-password --region "${AWS_REGION}")"
APP_ENV_B64="$(aws s3 cp "${SECRET_S3_URI}" - | base64 -w 0)"

echo "::add-mask::${ECR_PASSWORD}"
echo "::add-mask::${APP_ENV_B64}"

{
  echo "ECR_REGISTRY=${ECR_REGISTRY}"
  echo "ECR_IMAGE_NAME=${ECR_REGISTRY}/${ECR_REPOSITORY}"
  echo "ECR_PASSWORD=${ECR_PASSWORD}"
  echo "APP_ENV_B64=${APP_ENV_B64}"
} >> "${GITHUB_ENV}"
