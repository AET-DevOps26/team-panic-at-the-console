#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SPEC="$REPO_ROOT/api/openapi.yaml"

if [ ! -f "$SPEC" ]; then
  echo "No api/openapi.yaml found, skipping OpenAPI lint"
  exit 0
fi

npx --yes @redocly/cli@2.30.3 lint "$SPEC"
