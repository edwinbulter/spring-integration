#!/usr/bin/env bash
# Deletes the shared "single-node" kind cluster entirely (all demos and any
# other workloads running in it are lost). Use `kubectl delete ns <demo>` from
# a demo's cleanup script instead if you only want to remove that one demo.
set -euo pipefail

CLUSTER_NAME="single-node"

echo "==> Deleting kind cluster '${CLUSTER_NAME}'..."
kind delete cluster --name "${CLUSTER_NAME}"
