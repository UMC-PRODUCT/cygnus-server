#!/usr/bin/env bash
# SSM Parameter Store 의 값을 디스크에 쓰지 않고,
# 실행할 명령의 프로세스 환경변수로만 주입한다.
#
# 사용법:
#   ./scripts/with-ssm-env.sh <ENVIRONMENT> [PROJECT_NAME] -- <COMMAND...>
#   예) ./scripts/with-ssm-env.sh alpha -- ./gradlew bootRun
#       AWS_PROFILE=umc ./scripts/with-ssm-env.sh alpha -- ./gradlew bootRun
#       ./scripts/with-ssm-env.sh prod cygnus-server -- java -jar build/libs/app.jar
#
# 환경변수:
#   SSM_PREFIX   (default: /umc-product)
#   AWS_REGION   (default: ap-northeast-2)
#   AWS_PROFILE  (aws cli 프로필. 비우면 기본 자격증명 체인 사용)
#
# 동작:
#   /umc-product/<PROJECT_NAME>/<ENVIRONMENT>/ 하위 전체를 복호화 조회한 뒤
#   exec env 로 명령을 실행한다. 값은 파일로 남지 않고 프로세스 메모리에만 존재한다.

set -euo pipefail

SSM_PREFIX="${SSM_PREFIX:-/umc-product}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"

usage() {
  echo "사용법: $0 <ENVIRONMENT> [PROJECT_NAME] -- <COMMAND...>" >&2
  echo "  예) $0 alpha -- ./gradlew bootRun" >&2
  exit 1
}

ENVIRONMENT="${1:-}"
[[ -z "${ENVIRONMENT}" ]] && usage
shift

PROJECT_NAME="cygnus-server"
if [[ "${1:-}" != "--" && -n "${1:-}" ]]; then
  PROJECT_NAME="$1"
  shift
fi
[[ "${1:-}" == "--" ]] || usage
shift
(( $# > 0 )) || usage

PATH_PREFIX="${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/"

env_pairs=()
while IFS=$'\t' read -r name value; do
  [[ -z "${name}" ]] && continue
  env_pairs+=("${name##*/}=${value}")
done < <(aws ssm get-parameters-by-path \
  --path "${PATH_PREFIX}" --recursive --with-decryption \
  --region "${AWS_REGION}" \
  --query "Parameters[*].[Name,Value]" --output text)

if (( ${#env_pairs[@]} == 0 )); then
  echo "ERROR: ${PATH_PREFIX} 에서 파라미터를 찾지 못했습니다 (권한 또는 경로 확인)." >&2
  exit 1
fi

echo "▶ ${PATH_PREFIX} 파라미터 ${#env_pairs[@]}개를 주입해 실행: $*" >&2
exec env "${env_pairs[@]}" "$@"
