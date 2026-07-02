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
cleanup() {
  rm -f "${TMP_OUTPUT}"
}
trap cleanup EXIT

{
  echo "# Generated from SSM Parameter Store path: ${SSM_PARAMETER_PATH}"
  echo "# Do not commit this file."
} > "${TMP_OUTPUT}"

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
' <<< "${PARAMETERS_JSON}" >> "${TMP_OUTPUT}"

install -m 600 "${TMP_OUTPUT}" "${OUTPUT_FILE}"
echo "Wrote ${PARAMETER_COUNT} parameters to ${OUTPUT_FILE}."
