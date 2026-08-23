#!/usr/bin/env bash
# Applies all demo-01 Kubernetes manifests, in order, to the current
# kubectl context.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
K8S_DIR="${DEMO_DIR}/k8s"

echo "==> Ensuring host data directories exist..."
mkdir -p "${DEMO_DIR}/data/input-01" "${DEMO_DIR}/data/processed-01" "${DEMO_DIR}/data/output-01"

echo "==> Applying namespace..."
kubectl apply -f "${K8S_DIR}/namespace.yaml"

echo "==> Applying kafka..."
kubectl apply -f "${K8S_DIR}/kafka.yaml"

echo "==> Waiting for kafka to become ready..."
kubectl -n demo-01 rollout status deployment/kafka --timeout=180s

echo "==> Applying apps..."
kubectl apply -f "${K8S_DIR}/file-to-kafka-app.yaml"
kubectl apply -f "${K8S_DIR}/kafka-to-file-app.yaml"

kubectl -n demo-01 rollout status deployment/file-to-kafka-app --timeout=120s
kubectl -n demo-01 rollout status deployment/kafka-to-file-app --timeout=120s

echo "==> demo-01 deployed. Pods:"
kubectl -n demo-01 get pods
