#!/usr/bin/env bash
# (Re)creates the shared "single-node" kind cluster used by every demo in this
# monorepo, with the wide /data/projects mount configured in kind-config.yaml.
#
# WARNING: this deletes any existing "single-node" kind cluster (and
# everything running in it) before recreating it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CLUSTER_NAME="single-node"

echo "==> Deleting existing kind cluster '${CLUSTER_NAME}' (if any)..."
kind delete cluster --name "${CLUSTER_NAME}" || true

echo "==> Creating kind cluster '${CLUSTER_NAME}'..."
# Run from the repo root so the relative extraMounts.hostPath (="..") in
# kind-config.yaml resolves to the parent workspace directory, not to
# wherever this script happened to be invoked from.
(cd "${REPO_ROOT}" && kind create cluster --name "${CLUSTER_NAME}" --config cluster/kind-config.yaml)

echo "==> Installing ingress-nginx..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
kubectl -n ingress-nginx rollout status deployment/ingress-nginx-controller --timeout=180s

echo "==> Cluster '${CLUSTER_NAME}' is ready."
