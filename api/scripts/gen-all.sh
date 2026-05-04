#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="$REPO_ROOT/api/openapi.yaml"

if [ ! -f "$SPEC" ]; then
  echo "api/openapi.yaml not found — nothing to generate" >&2
  exit 1
fi

echo "==> Generating Java Spring Boot stubs"
rm -rf "$REPO_ROOT/services/generated/java"
npx --yes @openapitools/openapi-generator-cli@2.13.0 generate \
  -i "$SPEC" \
  -g spring \
  --additional-properties=useSpringBoot3=true,interfaceOnly=true,useTags=true \
  -o "$REPO_ROOT/services/generated/java"

echo "==> Generating Python client (genai-service)"
TMP_CONFIG=$(mktemp)
printf 'project_name_override = "client"\n' > "$TMP_CONFIG"
rm -rf "$REPO_ROOT/services/genai-service/client"
(cd "$REPO_ROOT/services/genai-service" && openapi-python-client generate --path "$SPEC" --config "$TMP_CONFIG")
rm -f "$TMP_CONFIG"

echo "==> Generating TypeScript SDK (frontend)"
mkdir -p "$REPO_ROOT/services/frontend/src/api"
npx --yes openapi-typescript@7.4.4 "$SPEC" \
  -o "$REPO_ROOT/services/frontend/src/api/schema.d.ts"

echo "==> Done. Never edit generated files by hand."
