#!/usr/bin/env bash
# Builds both demo-01 apps and their Docker images, then loads the images
# into the shared "single-node" kind cluster so no registry is needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEMO_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
CLUSTER_NAME="single-node"
APPS=("file-to-kafka-app" "kafka-to-file-app")

echo "==> Building demo-01 (mvn package)..."
mvn -f "${DEMO_DIR}/pom.xml" -q package

for app in "${APPS[@]}"; do
  echo "==> Building Docker image ${app}:demo-01..."
  docker build -t "${app}:demo-01" "${DEMO_DIR}/${app}"

  echo "==> Loading ${app}:demo-01 into kind cluster '${CLUSTER_NAME}'..."
  kind load docker-image "${app}:demo-01" --name "${CLUSTER_NAME}"
done

echo "==> Images built and loaded."
