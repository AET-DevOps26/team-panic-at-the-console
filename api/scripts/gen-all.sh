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
  --additional-properties=useSpringBoot3=true,interfaceOnly=true,useTags=true,hideGenerationTimestamp=true \
  --global-property=apis \
  --global-property=models \
  --global-property=supportingFiles=ApiUtil.java \
  -o "$REPO_ROOT/services/generated/java"

echo "==> Generating Python client (services/generated/python-client)"
TMP_CONFIG_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_CONFIG_DIR"' EXIT
# project_name_override sets both the output directory and the importable package name.
# We keep `client` so genai-service can `from client.api.incidents import ...`.
printf 'project_name_override: client\n' > "$TMP_CONFIG_DIR/config.yaml"
GEN_ROOT="$REPO_ROOT/services/generated"
rm -rf "$GEN_ROOT/python-client" "$GEN_ROOT/client"
mkdir -p "$GEN_ROOT"
(cd "$GEN_ROOT" && openapi-python-client generate --path "$SPEC" --config "$TMP_CONFIG_DIR/config.yaml")
# openapi-python-client writes a `client/` dir holding pyproject.toml + the
# `client/` package. Rename to `python-client/` so the directory matches the
# language while the importable package keeps its short name.
mv "$GEN_ROOT/client" "$GEN_ROOT/python-client"

echo "==> Generating TypeScript SDK (frontend)"
mkdir -p "$REPO_ROOT/services/frontend/src/api"
npx --yes openapi-typescript@7.4.4 "$SPEC" \
  -o "$REPO_ROOT/services/frontend/src/api/schema.d.ts"

echo "==> Formatting generated files via pre-commit hooks"
find \
  "$REPO_ROOT/services/generated" \
  "$REPO_ROOT/services/frontend/src/api" \
  \( -type d \( -name target -o -name .ruff_cache -o -name __pycache__ -o -name node_modules \) -prune \) \
  -o -type f ! -name '*.class' -print0 2>/dev/null \
  | pixi run lefthook run pre-commit \
      --files-from-stdin \
      --no-stage-fixed \
      --no-fail-on-changes

echo "==> Done. Never edit generated files by hand."
