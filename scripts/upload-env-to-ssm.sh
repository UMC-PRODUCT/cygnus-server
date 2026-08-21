#!/usr/bin/env bash
# .env 형식 파일을 AWS SSM Parameter Store 에 SecureString 으로 업로드한다.
#
# 사용법:
#   ./scripts/upload-env-to-ssm.sh <ENV_FILE> <ENVIRONMENT> [PROJECT_NAME]
#   예) ./scripts/upload-env-to-ssm.sh .env.prod prod
#       DRY_RUN=1 ./scripts/upload-env-to-ssm.sh .env.example alpha
#
# 파라미터 이름 규칙: ${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/${KEY}
#   예) /umc-product/cygnus-server/prod/DATABASE_URL
#
# 환경변수:
#   SSM_PREFIX   (default: /umc-product)
#   PROJECT_NAME (default: cygnus-server — 세 번째 인자로도 지정 가능)
#   AWS_REGION   (default: ap-northeast-2)
#   AWS_PROFILE  (aws cli 프로필. 비우면 기본 자격증명 체인 사용)
#   TIER         (default: Intelligent-Tiering — 4KB 이하는 무료 Standard,
#                 초과 값만 Advanced 로 자동 승격되어 과금을 최소화한다)
#   KMS_KEY_ID   (비우면 계정 기본 aws/ssm KMS 키 사용)
#   DRY_RUN      (1이면 실제 업로드 없이 대상 목록만 출력)
#   ASSUME_YES   (1이면 업로드 전 확인 프롬프트 생략 — CI 용)
#
# 규칙:
#   - 주석(#)과 빈 줄은 무시한다.
#   - 값이 빈 항목은 업로드하지 않는다 (SSM 은 빈 값을 허용하지 않음).
#   - 값을 감싼 짝이 맞는 따옴표("..." / '...')는 벗겨서 저장한다
#     (docker compose v2 의 env_file dotenv 파싱과 동일한 동작).
#   - 4096 바이트를 넘는 값은 Advanced tier 로 저장되어 비용이 발생하므로 경고를 출력한다.

set -euo pipefail

usage() {
  sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
  exit 1
}

ENV_FILE="${1:-}"
ENVIRONMENT="${2:-}"
PROJECT_NAME="${3:-${PROJECT_NAME:-cygnus-server}}"
[[ -z "${ENV_FILE}" || -z "${ENVIRONMENT}" ]] && usage
[[ -f "${ENV_FILE}" ]] || { echo "ERROR: 파일을 찾을 수 없습니다: ${ENV_FILE}" >&2; exit 1; }

SSM_PREFIX="${SSM_PREFIX:-/umc-product}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
TIER="${TIER:-Intelligent-Tiering}"
KMS_KEY_ID="${KMS_KEY_ID:-}"
DRY_RUN="${DRY_RUN:-0}"
ASSUME_YES="${ASSUME_YES:-0}"

if [[ "${DRY_RUN}" != "1" ]]; then
  command -v aws >/dev/null 2>&1 || { echo "ERROR: aws cli 가 설치되어 있지 않습니다." >&2; exit 1; }
fi

# ── 1단계: 파싱 ─────────────────────────────────────────────
keys=()
values=()
skipped_empty=()
skipped_invalid=()

lineno=0
while IFS= read -r raw || [[ -n "${raw}" ]]; do
  lineno=$((lineno + 1))
  line="${raw%$'\r'}"
  line="${line#"${line%%[![:space:]]*}"}"           # 앞 공백 제거
  [[ -z "${line}" || "${line}" == \#* ]] && continue
  line="${line#export }"

  if [[ "${line}" != *"="* ]]; then
    skipped_invalid+=("L${lineno}: ${line}")
    continue
  fi

  key="${line%%=*}"
  value="${line#*=}"
  key="${key%"${key##*[![:space:]]}"}"              # 키 뒤 공백 제거

  if [[ ! "${key}" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
    skipped_invalid+=("L${lineno}: ${key}")
    continue
  fi

  # 짝이 맞는 감싼 따옴표 제거
  if [[ ${#value} -ge 2 ]]; then
    if [[ "${value:0:1}" == '"' && "${value: -1}" == '"' ]] \
      || [[ "${value:0:1}" == "'" && "${value: -1}" == "'" ]]; then
      value="${value:1:${#value}-2}"
    fi
  fi

  if [[ -z "${value}" ]]; then
    skipped_empty+=("${key}")
    continue
  fi

  keys+=("${key}")
  values+=("${value}")
done < "${ENV_FILE}"

# ── 2단계: 계획 출력 ────────────────────────────────────────
echo "════════════════════════════════════════════════════"
echo " SSM 업로드 계획"
echo "  파일     : ${ENV_FILE}"
echo "  대상     : ${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/<KEY>"
echo "  리전     : ${AWS_REGION}"
echo "  Tier     : ${TIER}"
echo "  업로드   : ${#keys[@]}개"
echo "  빈 값    : ${#skipped_empty[@]}개 (skip)"
echo "  형식오류 : ${#skipped_invalid[@]}개 (skip)"
echo "════════════════════════════════════════════════════"

for i in "${!keys[@]}"; do
  size=${#values[$i]}
  note=""
  (( size > 4096 )) && note="  ⚠ ${size}B > 4KB — Advanced tier 과금 발생"
  printf '  %-55s (%d B)%s\n' "${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/${keys[$i]}" "${size}" "${note}"
done

if (( ${#skipped_empty[@]} > 0 )); then
  echo ""
  echo "값이 비어 있어 업로드하지 않는 키:"
  printf '  - %s\n' "${skipped_empty[@]}"
fi

if (( ${#skipped_invalid[@]} > 0 )); then
  echo ""
  echo "형식이 올바르지 않아 무시한 줄:"
  printf '  - %s\n' "${skipped_invalid[@]}"
fi

if (( ${#keys[@]} == 0 )); then
  echo ""
  echo "업로드할 항목이 없습니다."
  exit 0
fi

if [[ "${DRY_RUN}" == "1" ]]; then
  echo ""
  echo "DRY_RUN=1 — 실제 업로드는 수행하지 않았습니다."
  exit 0
fi

# ── 3단계: 확인 후 업로드 ───────────────────────────────────
if [[ "${ASSUME_YES}" != "1" ]]; then
  echo ""
  read -r -p "${#keys[@]}개 파라미터를 업로드(덮어쓰기)합니다. 계속할까요? [y/N] " answer
  [[ "${answer}" == "y" || "${answer}" == "Y" ]] || { echo "중단했습니다."; exit 1; }
fi

uploaded=0
for i in "${!keys[@]}"; do
  param_name="${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/${keys[$i]}"

  args=(ssm put-parameter
    --name "${param_name}"
    --type SecureString
    --value="${values[$i]}"
    --tier "${TIER}"
    --overwrite
    --region "${AWS_REGION}")
  [[ -n "${KMS_KEY_ID}" ]] && args+=(--key-id "${KMS_KEY_ID}")

  version="$(aws "${args[@]}" --query 'Version' --output text)"
  uploaded=$((uploaded + 1))
  printf '  [%d/%d] %s (version %s)\n' "${uploaded}" "${#keys[@]}" "${param_name}" "${version}"
done

echo ""
echo "완료: ${uploaded}개 파라미터를 업로드했습니다."
echo "확인: aws ssm get-parameters-by-path --path \"${SSM_PREFIX}/${PROJECT_NAME}/${ENVIRONMENT}/\" --recursive --region ${AWS_REGION} --query 'Parameters[*].Name'"
