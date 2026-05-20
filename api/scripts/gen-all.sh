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
  --global-property=apis \
  --global-property=models \
  --global-property=supportingFiles=ApiUtil.java \
  -o "$REPO_ROOT/services/generated/java"

echo "==> Generating Python client (genai-service)"
TMP_CONFIG_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_CONFIG_DIR"' EXIT
printf 'project_name_override: client\n' > "$TMP_CONFIG_DIR/config.yaml"
rm -rf "$REPO_ROOT/services/genai-service/client"
(cd "$REPO_ROOT/services/genai-service" && openapi-python-client generate --path "$SPEC" --config "$TMP_CONFIG_DIR/config.yaml")

echo "==> Generating TypeScript SDK (frontend)"
mkdir -p "$REPO_ROOT/services/frontend/src/api"
npx --yes openapi-typescript@7.4.4 "$SPEC" \
  -o "$REPO_ROOT/services/frontend/src/api/schema.d.ts"

echo "==> Formatting generated files via pre-commit hooks"
GEN_PATHS=(
  "$REPO_ROOT/services/generated"
  "$REPO_ROOT/services/genai-service/client"
  "$REPO_ROOT/services/frontend/src/api"
)
mapfile -t GEN_FILES < <(
  find "${GEN_PATHS[@]}" \
    \( -type d \( -name target -o -name .ruff_cache -o -name __pycache__ -o -name node_modules \) -prune \) \
    -o -type f ! -name '*.class' -print 2>/dev/null
)
if [ ${#GEN_FILES[@]} -gt 0 ]; then
  printf '%s\0' "${GEN_FILES[@]}" | \
    pixi run lefthook run pre-commit \
      --files-from-stdin \
      --no-stage-fixed \
      --no-fail-on-changes || true
fi

echo "==> Done. Never edit generated files by hand."
