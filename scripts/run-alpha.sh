#!/usr/bin/env bash
# SSM 의 alpha 환경변수를 주입해 로컬에서 서버를 실행한다.
# .env 파일을 만들지 않으며, 값은 실행된 프로세스에만 존재한다.
#
# 사용법:
#   ./scripts/run-alpha.sh [GRADLE_ARGS...]   # ./gradlew bootRun [GRADLE_ARGS...]
#   ./scripts/run-alpha.sh -- <COMMAND...>    # bootRun 대신 임의 명령 실행
#
# 예)
#   AWS_PROFILE=umc ./scripts/run-alpha.sh
#   AWS_PROFILE=umc ./scripts/run-alpha.sh --debug-jvm
#   ./scripts/run-alpha.sh -- java -jar build/libs/app.jar
#
#   # 특정 값만 로컬에서 덮어쓰고 싶을 때 (env 가 SSM 주입값보다 나중에 적용됨)
#   ./scripts/run-alpha.sh -- env APP_SEED_ENABLED=false ./gradlew bootRun
#
# 사전 준비: aws cli 로그인 (docs/infra/ssm-parameter-store-guide.md 참고)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${SCRIPT_DIR}/.."

echo "⚠️  alpha 환경(공유 DB·리소스)에 연결합니다. 파괴적인 작업은 주의하세요." >&2

if [[ "${1:-}" == "--" ]]; then
  shift
  exec "${SCRIPT_DIR}/with-ssm-env.sh" alpha -- "$@"
fi

exec "${SCRIPT_DIR}/with-ssm-env.sh" alpha -- ./gradlew bootRun "$@"
