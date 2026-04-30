#!/usr/bin/env bash
set -euo pipefail

SPEC="api/openapi.yaml"

if [ ! -f "$SPEC" ]; then
  echo "No api/openapi.yaml found, skipping OpenAPI lint"
  exit 0
fi

npx --yes @redocly/cli@2.30.3 lint "$SPEC"
