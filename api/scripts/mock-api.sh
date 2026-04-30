#!/usr/bin/env bash
set -euo pipefail

SPEC="api/openapi.yaml"

if [ ! -f "$SPEC" ]; then
  echo "No api/openapi.yaml found, cannot start mock API" >&2
  exit 1
fi

npx --yes @stoplight/prism-cli@4.10.5 mock "$SPEC" -d
