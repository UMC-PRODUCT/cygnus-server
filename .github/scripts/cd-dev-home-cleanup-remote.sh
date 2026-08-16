#!/usr/bin/env bash
set -euo pipefail

: "${SERVER_APP_DIRECTORY:?SERVER_APP_DIRECTORY is required}"

mkdir -p "${SERVER_APP_DIRECTORY}"
rm -f "${SERVER_APP_DIRECTORY}/docker-compose.yml"
rm -f "${SERVER_APP_DIRECTORY}/nginx.conf"
