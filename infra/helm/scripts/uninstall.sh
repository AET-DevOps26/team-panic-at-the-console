#!/usr/bin/env bash
# Uninstalls the devops-platform Helm release. Leaves the namespace in place.
# Invoked locally and from CI via the `helm-uninstall` pixi task.
#
# Required env:
#   KUBECONFIG_B64    base64-encoded kubeconfig
#   DEPLOY_NAMESPACE  target namespace
set -euo pipefail

RELEASE="devops-platform"

require() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "::error:: env var $name is unset" >&2
    exit 1
  fi
}

require KUBECONFIG_B64
require DEPLOY_NAMESPACE

for bin in helm kubectl; do
  command -v "$bin" >/dev/null || {
    echo "::error:: $bin not on PATH. Run via 'pixi run -e deploy helm-uninstall'." >&2
    exit 1
  }
done

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT

KUBECONFIG="$WORK_DIR/kubeconfig"
export KUBECONFIG
printf '%s' "$KUBECONFIG_B64" | base64 -d > "$KUBECONFIG"
chmod 600 "$KUBECONFIG"
unset KUBECONFIG_B64

echo ">> helm uninstall $RELEASE (namespace=$DEPLOY_NAMESPACE)"
if helm status "$RELEASE" --namespace "$DEPLOY_NAMESPACE" >/dev/null 2>&1; then
  helm uninstall "$RELEASE" --namespace "$DEPLOY_NAMESPACE" --wait
else
  echo "   release '$RELEASE' not found in namespace '$DEPLOY_NAMESPACE'; skipping helm uninstall"
fi

echo ">> delete leftover resources in namespace=$DEPLOY_NAMESPACE"
kubectl delete all,configmap,secret,pvc,ingress \
  --namespace "$DEPLOY_NAMESPACE" \
  --selector "app.kubernetes.io/instance=$RELEASE" \
  --ignore-not-found
