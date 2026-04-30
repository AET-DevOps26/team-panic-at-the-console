#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="$REPO_ROOT/api/openapi.yaml"

if [ ! -f "$SPEC" ]; then
  echo "No api/openapi.yaml found, skipping codegen validation"
  exit 0
fi

echo "==> Generate TypeScript SDK (validation target)"
npx --yes openapi-typescript@7.4.4 "$SPEC" \
  -o /tmp/schema.d.ts

echo "==> Generate Python client (validation target)"
openapi-python-client generate \
  --path "$SPEC" \
  --output-path /tmp/genai-client \
  --overwrite

echo "==> Generate Java Spring Boot stubs (validation target)"
npx --yes @openapitools/openapi-generator-cli@2.13.0 generate \
  -i "$SPEC" \
  -g spring \
  --additional-properties=useSpringBoot3=true,interfaceOnly=true,useTags=true \
  -o /tmp/java-generated
