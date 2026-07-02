#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/download-ssm-env.sh --path <ssm-path> [--output <file>] [--region <region>] [--profile <profile>] [--overwrite]

Downloads SecureString/String parameters under an SSM Parameter Store path and renders a dotenv file.

Example:
  scripts/download-ssm-env.sh \
    --path /umc-product/dev/runtime \
    --output .env.local \
    --profile umcproduct-readonly \
    --overwrite

Parameter naming rule:
  /umc-product/dev/runtime/JWT_ACCESS_TOKEN_SECRET -> JWT_ACCESS_TOKEN_SECRET=...

Values must be single-line dotenv-ready strings. Store multiline values as escaped text or base64.
USAGE
}

base64_decode() {
  if base64 --decode >/dev/null 2>&1 <<< "" ; then
    base64 --decode
  else
    base64 -D
  fi
}

SSM_PARAMETER_PATH="${SSM_PARAMETER_PATH:-}"
OUTPUT_FILE="${ENV_OUTPUT_FILE:-.env.local}"
AWS_REGION_VALUE="${AWS_REGION:-ap-northeast-2}"
AWS_PROFILE_VALUE="${AWS_PROFILE:-}"
OVERWRITE=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --path)
      SSM_PARAMETER_PATH="${2:-}"
      shift 2
      ;;
    --output)
      OUTPUT_FILE="${2:-}"
      shift 2
      ;;
    --region)
      AWS_REGION_VALUE="${2:-}"
      shift 2
      ;;
    --profile)
      AWS_PROFILE_VALUE="${2:-}"
      shift 2
      ;;
    --overwrite)
      OVERWRITE=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -z "${SSM_PARAMETER_PATH}" ]]; then
  echo "--path or SSM_PARAMETER_PATH is required." >&2
  usage >&2
  exit 2
fi

if [[ -z "${OUTPUT_FILE}" ]]; then
  echo "--output or ENV_OUTPUT_FILE is required." >&2
  exit 2
fi

if [[ -e "${OUTPUT_FILE}" && "${OVERWRITE}" != "true" ]]; then
  echo "Output file already exists: ${OUTPUT_FILE}" >&2
  echo "Pass --overwrite to replace it." >&2
  exit 2
fi

command -v aws >/dev/null 2>&1 || {
  echo "aws CLI is required." >&2
  exit 1
}

command -v jq >/dev/null 2>&1 || {
  echo "jq is required." >&2
  exit 1
}

if [[ "${SSM_PARAMETER_PATH}" != /* ]]; then
  SSM_PARAMETER_PATH="/${SSM_PARAMETER_PATH}"
fi
SSM_PARAMETER_PATH="${SSM_PARAMETER_PATH%/}"

AWS_ARGS=(--region "${AWS_REGION_VALUE}")
if [[ -n "${AWS_PROFILE_VALUE}" ]]; then
  AWS_ARGS+=(--profile "${AWS_PROFILE_VALUE}")
fi

PARAMETERS_JSON="$(aws ssm get-parameters-by-path \
  "${AWS_ARGS[@]}" \
  --path "${SSM_PARAMETER_PATH}" \
  --with-decryption \
  --recursive \
  --output json)"

PARAMETER_COUNT="$(jq '.Parameters | length' <<< "${PARAMETERS_JSON}")"
if [[ "${PARAMETER_COUNT}" == "0" ]]; then
  echo "No parameters found under ${SSM_PARAMETER_PATH}." >&2
  exit 1
fi

TMP_OUTPUT="$(mktemp)"
TMP_KEYS="$(mktemp)"
cleanup() {
  rm -f "${TMP_OUTPUT}" "${TMP_KEYS}"
}
trap cleanup EXIT

{
  echo "# Generated from SSM Parameter Store path: ${SSM_PARAMETER_PATH}"
  echo "# Do not commit this file."
} > "${TMP_OUTPUT}"

while IFS= read -r encoded_parameter; do
  parameter_json="$(printf '%s' "${encoded_parameter}" | base64_decode)"
  parameter_name="$(jq -r '.Name' <<< "${parameter_json}")"
  parameter_value="$(jq -r '.Value' <<< "${parameter_json}")"
  env_key="${parameter_name##*/}"

  if ! [[ "${env_key}" =~ ^[A-Z_][A-Z0-9_]*$ ]]; then
    echo "Invalid env key from SSM parameter name: ${parameter_name}" >&2
    exit 1
  fi

  if grep -Fxq "${env_key}" "${TMP_KEYS}"; then
    echo "Duplicate env key resolved from SSM parameters: ${env_key}" >&2
    exit 1
  fi
  printf '%s\n' "${env_key}" >> "${TMP_KEYS}"

  if [[ "${parameter_value}" == *$'\n'* || "${parameter_value}" == *$'\r'* ]]; then
    echo "Parameter value must be single-line for dotenv rendering: ${parameter_name}" >&2
    echo "Store multiline secrets as escaped text or base64." >&2
    exit 1
  fi

  printf '%s=%s\n' "${env_key}" "${parameter_value}" >> "${TMP_OUTPUT}"
done < <(jq -r '.Parameters | sort_by(.Name)[] | @base64' <<< "${PARAMETERS_JSON}")

install -m 600 "${TMP_OUTPUT}" "${OUTPUT_FILE}"
echo "Wrote ${PARAMETER_COUNT} parameters to ${OUTPUT_FILE}."
