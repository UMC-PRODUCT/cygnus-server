#!/usr/bin/env bash

set -euo pipefail

boot_jars=()
while IFS= read -r jar_path; do
  boot_jars+=("$jar_path")
done < <(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)

if [[ "${#boot_jars[@]}" -ne 1 ]]; then
  echo "::error::Expected exactly one boot JAR, found ${#boot_jars[@]}."
  exit 1
fi

deny_pattern='(^|/)(\.env($|\.)|google-services.*\.json$|GoogleService-Info.*\.plist$|client_secret_.*\.json$|service[-_]account.*\.json$|.*firebase-adminsdk.*\.json$|.*\.(pem|key|p8|p12|pfx|jks|keystore)$|kubeconfig($|\.)|.*\.kubeconfig$|k3s\.ya?ml$|.*\.tfstate($|\.)|.*\.tfvars($|\.)|application-(secret|local).*\.ya?ml$)'
unsafe_entries="$(jar tf "${boot_jars[0]}" | grep -E "$deny_pattern" || true)"

if [[ -n "$unsafe_entries" ]]; then
  echo "::error::The boot JAR contains forbidden local or credential files."
  printf '%s\n' "$unsafe_entries"
  exit 1
fi

echo "Boot JAR credential-file check passed."
