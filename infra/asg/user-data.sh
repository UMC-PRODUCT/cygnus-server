#!/usr/bin/env bash
set -euo pipefail

exec > >(tee /var/log/umc-product-user-data.log | logger -t user-data -s 2>/dev/console) 2>&1

AWS_REGION="__AWS_REGION__"
AWS_ACCOUNT_ID="__AWS_ACCOUNT_ID__"
ECR_REPOSITORY="__ECR_REPOSITORY__"
IMAGE_TAG="__IMAGE_TAG__"
APP_PORT="__APP_PORT__"
MANAGEMENT_PORT="__MANAGEMENT_PORT__"
SSM_PARAMETER_PATH="__SSM_PARAMETER_PATH__"
SPRING_PROFILE="__SPRING_PROFILE__"

ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

APP_DIR="/opt/umc-product"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Starting UMC Product server"
echo "Image: ${IMAGE_URI}"
echo "Spring profile: ${SPRING_PROFILE}"
echo "Secret source configured"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

ensure_bootstrap_dependencies() {
  local missing=()

  command -v docker >/dev/null 2>&1 || missing+=("docker")
  docker compose version >/dev/null 2>&1 || missing+=("docker compose plugin")
  command -v aws >/dev/null 2>&1 || missing+=("awscli")
  command -v jq >/dev/null 2>&1 || missing+=("jq")
  command -v curl >/dev/null 2>&1 || missing+=("curl")

  if (( ${#missing[@]} > 0 )); then
    echo "Missing baked AMI dependencies: ${missing[*]}"
    echo "Bake Docker, Docker Compose plugin, AWS CLI, jq, and curl into the Launch Template AMI."
    exit 1
  fi
}

ensure_bootstrap_dependencies

systemctl enable docker
systemctl start docker

mkdir -p "${APP_DIR}"
cd "${APP_DIR}"

echo "Rendering env file from SSM Parameter Store..."
PARAMETERS_JSON="$(aws ssm get-parameters-by-path \
  --region "${AWS_REGION}" \
  --path "${SSM_PARAMETER_PATH}" \
  --with-decryption \
  --recursive \
  --output json)"

PARAMETER_COUNT="$(jq '.Parameters | length' <<< "${PARAMETERS_JSON}")"
if [[ "${PARAMETER_COUNT}" == "0" ]]; then
  echo "No SSM parameters found under ${SSM_PARAMETER_PATH}"
  exit 1
fi

{
  echo "# Generated from SSM Parameter Store path: ${SSM_PARAMETER_PATH}"
  echo "# Do not edit on the instance."
} > "${APP_DIR}/.env"

jq -r '
  def invalid_key_items:
    map(select((.env_key | test("^[A-Z_][A-Z0-9_]*$")) | not));
  def multiline_value_items:
    map(select((.Value | contains("\n")) or (.Value | contains("\r"))));
  def duplicate_key_groups:
    group_by(.env_key) | map(select(length > 1));

  .Parameters
  | map(. + { env_key: (.Name | split("/")[-1]) })
  | invalid_key_items as $invalid_keys
  | if ($invalid_keys | length) > 0 then
      error("Invalid env key from SSM parameter name: " + $invalid_keys[0].Name)
    else . end
  | multiline_value_items as $multiline_values
  | if ($multiline_values | length) > 0 then
      error("Parameter value must be single-line for dotenv rendering: " + $multiline_values[0].Name)
    else . end
  | duplicate_key_groups as $duplicate_groups
  | if ($duplicate_groups | length) > 0 then
      error("Duplicate env key resolved from SSM parameters: " + $duplicate_groups[0][0].env_key)
    else . end
  | sort_by(.env_key)[]
  | "\(.env_key)=\(.Value)"
' <<< "${PARAMETERS_JSON}" >> "${APP_DIR}/.env"

chmod 600 "${APP_DIR}/.env"

echo "Logging in to ECR..."
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

cat > docker-compose.yml <<EOF
services:
  app:
    image: ${IMAGE_URI}
    container_name: umc-product-app
    restart: unless-stopped
    env_file:
      - .env
    environment:
      SERVER_PORT: "${APP_PORT}"
      MANAGEMENT_PORT: "${MANAGEMENT_PORT}"
      SPRING_PROFILES_ACTIVE: "${SPRING_PROFILE}"
    ports:
      - "${APP_PORT}:${APP_PORT}"
EOF

if [[ "${MANAGEMENT_PORT}" != "${APP_PORT}" ]]; then
  cat >> docker-compose.yml <<EOF
      - "${MANAGEMENT_PORT}:${MANAGEMENT_PORT}"
EOF
fi

cat >> docker-compose.yml <<EOF
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:${MANAGEMENT_PORT}/actuator/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
EOF

echo "Pulling and starting container..."
docker compose pull
docker compose up -d

echo "Waiting for local health check..."
CONTAINER_ID="$(docker compose ps -q app)"

if [[ -z "${CONTAINER_ID}" ]]; then
  echo "Application container was not created"
  docker compose ps
  docker compose logs --tail=300
  exit 1
fi

for i in {1..60}; do
  HEALTH_STATUS="$(docker inspect --format='{{.State.Health.Status}}' "${CONTAINER_ID}" 2>/dev/null || echo starting)"

  if [[ "${HEALTH_STATUS}" == "healthy" ]]; then
    echo "Application is healthy"
    exit 0
  fi

  if [[ "${HEALTH_STATUS}" == "unhealthy" ]]; then
    echo "Application became unhealthy"
    docker compose logs --tail=300
    exit 1
  fi

  echo "Waiting for application... ${i}/60 (health=${HEALTH_STATUS})"
  sleep 5
done

echo "Application failed to become healthy"
docker compose logs --tail=300
exit 1
