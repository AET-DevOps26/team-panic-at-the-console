#!/usr/bin/env bash
# Deploys the devops-platform Helm chart.
# Invoked locally and from CI via the `helm-deploy` pixi task.
#
# Required env (same names used in CI as GitHub secrets/vars):
#   KUBECONFIG_B64    base64-encoded kubeconfig
#   SOPS_AGE_KEY      age private key for SOPS-encrypted values
#   DEPLOY_NAMESPACE  target namespace
#   TAG               image tag to deploy (e.g. main, v0.1.0, sha-<sha>)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
CHART_DIR="$REPO_ROOT/infra/helm/devops-platform"
ENC_VALUES="$REPO_ROOT/infra/helm/secrets/values.prod.enc.yaml"
DEC_VALUES="$REPO_ROOT/infra/helm/secrets/values.prod.dec.yaml"

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

echo ">> configure kubeconfig"
mkdir -p "$HOME/.kube"
if [ -f "$HOME/.kube/config" ] && [ ! -f "$HOME/.kube/config.helm-deploy.bak" ]; then
  cp "$HOME/.kube/config" "$HOME/.kube/config.helm-deploy.bak"
  echo "   backed up existing kubeconfig to ~/.kube/config.helm-deploy.bak"
fi
echo "$KUBECONFIG_B64" | base64 --decode > "$HOME/.kube/config"
chmod 600 "$HOME/.kube/config"

echo ">> decrypt SOPS values"
mkdir -p "$HOME/.config/sops/age"
printf '%s' "$SOPS_AGE_KEY" > "$HOME/.config/sops/age/keys.txt"
chmod 600 "$HOME/.config/sops/age/keys.txt"
sops --decrypt "$ENC_VALUES" > "$DEC_VALUES"
trap 'rm -f "$DEC_VALUES"' EXIT

echo ">> helm upgrade --install (namespace=$DEPLOY_NAMESPACE tag=$TAG)"
helm upgrade --install devops-platform "$CHART_DIR" \
  --namespace "$DEPLOY_NAMESPACE" \
  --create-namespace \
  --set global.image.tag="$TAG" \
  --values "$DEC_VALUES"
