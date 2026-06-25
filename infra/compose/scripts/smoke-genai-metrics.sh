#!/usr/bin/env bash
# Seed a test incident (if missing) and trigger three genai regen tasks so
# Prometheus/Grafana panels get data. Requires compose stack up with Ollama ready.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
COMPOSE=(docker compose --project-directory "$REPO_ROOT" -f "$REPO_ROOT/infra/compose/docker-compose.yml")

INCIDENT_ID="${INCIDENT_ID:-018e2c5f-1234-7abc-8def-000000000099}"
INCIDENT_SERVICE="${INCIDENT_SERVICE:-http://localhost:8081}"
GENAI_METRICS="${GENAI_METRICS:-http://localhost:8087/metrics}"
WAIT_SECONDS="${WAIT_SECONDS:-75}"

"${COMPOSE[@]}" exec -T postgres psql -U devops -d incidents -c \
  "INSERT INTO incidents (id, status, severity, created_at, updated_at, title)
   VALUES ('${INCIDENT_ID}', 'OPEN', 'SEV2', NOW(), NOW(), 'Metrics smoke test')
   ON CONFLICT DO NOTHING;"

for task in summary severity solutions; do
  code=$(curl -sS -o /dev/null -w '%{http_code}' -X POST \
    "${INCIDENT_SERVICE}/incidents/${INCIDENT_ID}/genai/${task}")
  echo "POST /genai/${task} -> ${code}"
  test "${code}" = "202"
done

echo "Waiting ${WAIT_SECONDS}s for Ollama to finish..."
sleep "${WAIT_SECONDS}"

curl -sS "${GENAI_METRICS}" | grep -E '^ai_generations_total|^nats_messages_total' || true
echo "Open Grafana: http://localhost:3030 (dashboard: genai-service, range: Last 15 minutes)"
