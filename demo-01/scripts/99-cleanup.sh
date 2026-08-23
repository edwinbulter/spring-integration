#!/usr/bin/env bash
# Removes everything demo-01 deployed to the cluster by deleting its
# namespace. The shared kind cluster and any other demos are untouched.
set -euo pipefail

echo "==> Deleting namespace 'demo-01'..."
kubectl delete namespace demo-01 --ignore-not-found
echo "==> Done."
