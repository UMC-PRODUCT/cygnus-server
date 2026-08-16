#!/usr/bin/env bash
set -euo pipefail

: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_IMAGE_NAME:?ECR_IMAGE_NAME is required}"
: "${ECR_PASSWORD:?ECR_PASSWORD is required}"
: "${IMAGE_TAG:?IMAGE_TAG is required}"
: "${SERVER_APP_DIRECTORY:?SERVER_APP_DIRECTORY is required}"
: "${APP_ENV_B64:?APP_ENV_B64 is required}"
: "${APP_REPLICAS:?APP_REPLICAS is required}"
: "${HTTP_PORT:?HTTP_PORT is required}"
: "${APP_PORT:?APP_PORT is required}"
: "${MANAGEMENT_PORT:?MANAGEMENT_PORT is required}"
: "${HEALTHCHECK_TIMEOUT:?HEALTHCHECK_TIMEOUT is required}"
: "${POSTGRES_USER:?POSTGRES_USER is required}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required}"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "develop 홈서버 배포 시작"
echo "Image: ${ECR_IMAGE_NAME}:${IMAGE_TAG}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

export PATH="${PATH}:/usr/local/bin:/opt/homebrew/bin:/Applications/Docker.app/Contents/Resources/bin"

DOCKER_BIN="$(command -v docker || true)"
if [[ -z "${DOCKER_BIN}" ]]; then
  echo "docker 명령어를 찾을 수 없습니다."
  exit 1
fi

ORIGINAL_DOCKER_CONFIG="${DOCKER_CONFIG:-${HOME:-}/.docker}"
CURRENT_DOCKER_CONTEXT="$("${DOCKER_BIN}" context show 2>/dev/null || true)"
USING_DOCKER_HOST_FALLBACK=false

echo "[1] Docker 환경 확인"
echo "Docker Path=${DOCKER_BIN}"
echo "Docker Context=${CURRENT_DOCKER_CONTEXT:-unknown}"
echo "Docker Host=${DOCKER_HOST:-context-default}"

if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
  echo "Docker daemon에 연결할 수 없습니다."
  "${DOCKER_BIN}" context ls || true

  for docker_socket in "${HOME:-}/.docker/run/docker.sock" "${HOME:-}/.colima/default/docker.sock"; do
    if [[ -S "${docker_socket}" ]]; then
      export DOCKER_HOST="unix://${docker_socket}"
      USING_DOCKER_HOST_FALLBACK=true
      echo "Docker socket fallback 적용: ${DOCKER_HOST}"
      break
    fi
  done

  if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
    echo "Docker daemon에 연결할 수 없습니다."
    exit 1
  fi
fi

DOCKER_CONFIG="$(mktemp -d)"
export DOCKER_CONFIG

if [[ -d "${ORIGINAL_DOCKER_CONFIG}/contexts" ]]; then
  cp -R "${ORIGINAL_DOCKER_CONFIG}/contexts" "${DOCKER_CONFIG}/contexts"
fi

if [[ -d "${ORIGINAL_DOCKER_CONFIG}/cli-plugins" ]]; then
  mkdir -p "${DOCKER_CONFIG}/cli-plugins"
  for plugin in "${ORIGINAL_DOCKER_CONFIG}"/cli-plugins/*; do
    [[ -e "${plugin}" ]] || continue
    ln -s "${plugin}" "${DOCKER_CONFIG}/cli-plugins/$(basename "${plugin}")"
  done
fi

if [[ "${USING_DOCKER_HOST_FALLBACK}" == "false" && -z "${DOCKER_HOST:-}" && -n "${CURRENT_DOCKER_CONTEXT}" ]]; then
  export DOCKER_CONTEXT="${CURRENT_DOCKER_CONTEXT}"
fi

cleanup() {
  "${DOCKER_BIN}" logout "${ECR_REGISTRY}" >/dev/null 2>&1 || true
  rm -rf "${DOCKER_CONFIG}"
}
trap cleanup EXIT

echo "Docker Isolated Config=${DOCKER_CONFIG}"

if ! "${DOCKER_BIN}" info >/dev/null 2>&1; then
  echo "격리된 Docker config에서 Docker daemon에 연결할 수 없습니다."
  "${DOCKER_BIN}" context ls || true
  exit 1
fi

if "${DOCKER_BIN}" compose version >/dev/null 2>&1; then
  COMPOSE_MODE="docker-plugin"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_MODE="standalone"
  DOCKER_COMPOSE_BIN="$(command -v docker-compose)"
else
  echo "docker compose 플러그인을 찾을 수 없습니다."
  find "${ORIGINAL_DOCKER_CONFIG}" -maxdepth 3 -type f -name 'docker-compose' 2>/dev/null || true
  exit 1
fi

docker_compose() {
  if [[ "${COMPOSE_MODE}" == "docker-plugin" ]]; then
    "${DOCKER_BIN}" compose "$@"
  else
    "${DOCKER_COMPOSE_BIN}" "$@"
  fi
}

echo "Docker Compose Mode=${COMPOSE_MODE}"

echo "[2] ECR 인증 설정"
ECR_AUTH="$(printf 'AWS:%s' "${ECR_PASSWORD}" | base64 | tr -d '\n')"
cat > "${DOCKER_CONFIG}/config.json" << EOF
{
  "auths": {
    "${ECR_REGISTRY}": {
      "auth": "${ECR_AUTH}"
    }
  }
}
EOF

echo "[3] 애플리케이션 디렉토리 확인"
if [[ ! -d "${SERVER_APP_DIRECTORY}" ]]; then
  echo "애플리케이션 디렉토리가 존재하지 않습니다: ${SERVER_APP_DIRECTORY}"
  exit 1
fi

cd "${SERVER_APP_DIRECTORY}"

echo "[4] 애플리케이션 런타임 환경 파일 갱신"
if printf '%s' "${APP_ENV_B64}" | base64 -d > app.env 2>/dev/null; then
  echo "app.env 디코딩 완료: base64 -d"
elif printf '%s' "${APP_ENV_B64}" | base64 -D > app.env 2>/dev/null; then
  echo "app.env 디코딩 완료: base64 -D"
else
  echo "app.env 디코딩에 실패했습니다."
  exit 1
fi

echo "[5] Nginx 설정 렌더링"
TEMP_NGINX_CONFIG="$(mktemp)"
sed "s|__APP_PORT__|${APP_PORT}|g" nginx.conf > "${TEMP_NGINX_CONFIG}"
mv "${TEMP_NGINX_CONFIG}" nginx.conf

echo "[6] Docker Compose 치환 전용 환경 파일 갱신"
{
  echo "# GitHub Actions develop deployment compose variables"
  echo "NGINX_CONTAINER_NAME=umc-product-nginx-dev"
  echo "IMAGE_NAME=${ECR_IMAGE_NAME}"
  echo "IMAGE_TAG=${IMAGE_TAG}"
  echo "HTTP_PORT=${HTTP_PORT}"
  echo "APP_PORT=${APP_PORT}"
  echo "MANAGEMENT_PORT=${MANAGEMENT_PORT}"
  echo "SPRING_PROFILES_ACTIVE=dev"
  echo "APP_REPLICAS=${APP_REPLICAS}"
  echo "POSTGRES_CONTAINER_NAME=umc-product-postgres-dev"
  echo "POSTGRES_DB=development"
  echo "POSTGRES_USER=${POSTGRES_USER}"
  echo "POSTGRES_PASSWORD=${POSTGRES_PASSWORD}"
  echo "VALKEY_CONTAINER_NAME=umc-product-valkey-dev"
} > .env

wait_for_services_health() {
  deadline=$(($(date +%s) + HEALTHCHECK_TIMEOUT))

  while [[ "$(date +%s)" -le "${deadline}" ]]; do
    APP_CONTAINER_ID="$(docker_compose ps -q app 2>/dev/null | head -n 1 || true)"
    NGINX_CONTAINER_ID="$(docker_compose ps -q nginx 2>/dev/null | head -n 1 || true)"
    APP_HEALTHY=false
    NGINX_HEALTHY=false

    if [[ -n "${APP_CONTAINER_ID}" ]] \
      && "${DOCKER_BIN}" exec "${APP_CONTAINER_ID}" curl -sf "http://localhost:${MANAGEMENT_PORT}/actuator/health" >/dev/null 2>&1; then
      APP_HEALTHY=true
    fi

    if [[ -n "${NGINX_CONTAINER_ID}" ]] \
      && "${DOCKER_BIN}" exec "${NGINX_CONTAINER_ID}" wget -q --spider "http://localhost/nginx-health" >/dev/null 2>&1; then
      NGINX_HEALTHY=true
    fi

    if [[ "${APP_HEALTHY}" == "true" && "${NGINX_HEALTHY}" == "true" ]]; then
      echo "App/Nginx healthcheck 성공"
      return 0
    fi

    sleep 5
  done

  echo "App/Nginx healthcheck 타임아웃: ${HEALTHCHECK_TIMEOUT}s"
  return 1
}

compose_up_with_wait() {
  if docker_compose up --help 2>/dev/null | grep -q -- '--wait'; then
    docker_compose up -d \
      --scale "app=${APP_REPLICAS}" \
      --remove-orphans \
      --wait \
      --wait-timeout "${HEALTHCHECK_TIMEOUT}" \
      app nginx
  else
    echo "docker compose --wait 미지원: app healthcheck를 직접 확인합니다."
    docker_compose up -d \
      --scale "app=${APP_REPLICAS}" \
      --remove-orphans \
      app nginx
    wait_for_services_health
  fi
}

cleanup_old_images() {
  CURRENT_IMAGE="${ECR_IMAGE_NAME}:${IMAGE_TAG}"
  LATEST_IMAGE="${ECR_IMAGE_NAME}:development-latest"
  OLD_IMAGES="$("${DOCKER_BIN}" images "${ECR_IMAGE_NAME}" --format '{{.Repository}}:{{.Tag}}' 2>/dev/null || true)"

  printf '%s\n' "${OLD_IMAGES}" | while IFS= read -r image_ref; do
    [[ -n "${image_ref}" ]] || continue
    case "${image_ref}" in
      "${CURRENT_IMAGE}"|"${LATEST_IMAGE}")
        continue
        ;;
    esac

    "${DOCKER_BIN}" image rm "${image_ref}" >/dev/null 2>&1 || true
  done

  "${DOCKER_BIN}" image prune -f >/dev/null 2>&1 || true
}

echo "[7] App 이미지 Pull"
docker_compose pull app

echo "[8] 서비스 갱신 및 healthcheck 대기"
if ! compose_up_with_wait; then
  echo "서비스 갱신 또는 healthcheck 대기 중 실패했습니다."
  echo ""
  echo "Docker Compose 상태:"
  docker_compose ps || true
  echo ""
  echo "PostgreSQL 로그:"
  docker_compose logs --tail=100 postgres || true
  echo ""
  echo "Valkey 로그:"
  docker_compose logs --tail=100 valkey || true
  echo ""
  echo "Nginx 로그:"
  docker_compose logs --tail=100 nginx || true
  echo ""
  echo "App 로그:"
  docker_compose logs --tail=100 app || true
  exit 1
fi

echo "[9] 컨테이너 상태"
docker_compose ps

echo "[10] 오래된 app 이미지 정리"
cleanup_old_images

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "develop 홈서버 배포 완료"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
