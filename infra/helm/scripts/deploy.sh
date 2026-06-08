#!/usr/bin/env bash
# Deploys the devops-platform Helm chart.
# Invoked locally and from CI via the `helm-deploy` pixi task.
#
# Required env (same names used in CI as GitHub secrets/vars):
#   KUBECONFIG_B64    base64-encoded kubeconfig
#   SOPS_AGE_KEY      age private key for SOPS-encrypted values
#   DEPLOY_NAMESPACE  target namespace
#   TAG               image tag to deploy (e.g. main, v0.1.0, sha-<sha>)
# Optional env:
#   VALUES_FILE       path to SOPS-encrypted values file
#                     (default: infra/helm/secrets/values.prod.enc.yaml)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CHART_DIR="$REPO_ROOT/infra/helm/devops-platform"
ENC_VALUES="${VALUES_FILE:-$REPO_ROOT/infra/helm/secrets/values.prod.enc.yaml}"

require() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "::error:: env var $name is unset" >&2
    exit 1
  fi
}

require KUBECONFIG_B64
require SOPS_AGE_KEY
require DEPLOY_NAMESPACE
require TAG

for bin in helm kubectl sops; do
  command -v "$bin" >/dev/null || {
    echo "::error:: $bin not on PATH. Run via 'pixi run -e deploy helm-deploy'." >&2
    exit 1
  }
done

helm version --short
kubectl version --client
sops --version | head -n1

WORK_DIR="$(mktemp -d)"
DEC_VALUES="$WORK_DIR/values.dec.yaml"
trap 'rm -rf "$WORK_DIR"' EXIT

if [ ! -f "$ENC_VALUES" ]; then
  echo "::error:: encrypted values file not found: $ENC_VALUES" >&2
  exit 1
fi

echo ">> configure kubeconfig"
KUBECONFIG="$WORK_DIR/kubeconfig"
export KUBECONFIG
printf '%s' "$KUBECONFIG_B64" | base64 -d > "$KUBECONFIG"
chmod 600 "$KUBECONFIG"
unset KUBECONFIG_B64

echo ">> decrypt SOPS values"
SOPS_AGE_KEY_FILE="$WORK_DIR/age.key"
export SOPS_AGE_KEY_FILE
printf '%s' "$SOPS_AGE_KEY" > "$SOPS_AGE_KEY_FILE"
chmod 600 "$SOPS_AGE_KEY_FILE"
unset SOPS_AGE_KEY
sops --decrypt "$ENC_VALUES" > "$DEC_VALUES"

CHART_STAGE="$WORK_DIR/chart"
mkdir -p "$CHART_STAGE/files"
cp -r "$CHART_DIR"/. "$CHART_STAGE/"
cp "$REPO_ROOT/api/openapi.yaml" "$CHART_STAGE/files/openapi.yaml"

echo ">> helm upgrade --install (namespace=$DEPLOY_NAMESPACE tag=$TAG)"
HELM_VALUES=(--values "$DEC_VALUES")
MONITORING_ENC="${MONITORING_VALUES_FILE:-$REPO_ROOT/infra/helm/secrets/values.monitoring.enc.yaml}"
if [ -f "$MONITORING_ENC" ]; then
  echo ">> merge monitoring values from $MONITORING_ENC"
  sops --decrypt "$MONITORING_ENC" > "$WORK_DIR/values.monitoring.dec.yaml"
  HELM_VALUES+=(--values "$WORK_DIR/values.monitoring.dec.yaml")
fi

helm upgrade --install devops-platform "$CHART_STAGE" \
  --namespace "$DEPLOY_NAMESPACE" \
  --create-namespace \
  --wait \
  --timeout 10m \
  --rollback-on-failure \
  --set global.image.tag="$TAG" \
  "${HELM_VALUES[@]}"
