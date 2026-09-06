# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a Spring Integration monorepo containing independent demos (demo-01, demo-02, etc.), each showcasing different Spring Integration patterns. The demos share only:
- Root `pom.xml` for dependency/plugin management (Spring Boot BOM, Java version)
- Shared kind cluster configuration in `cluster/`

**Important**: The root `pom.xml` intentionally has NO `<modules>` section. Each demo is fully independent and must be built separately.

## Build Commands

### Building a Specific Demo
```bash
# Build all apps within demo-01
mvn -f demo-01/pom.xml package

# Build a single app within demo-01
mvn -f demo-01/file-to-kafka-app/pom.xml package
```

### Running Tests
```bash
# Run all tests in demo-01
mvn -f demo-01/pom.xml test

# Run tests for a single app
mvn -f demo-01/file-to-kafka-app/pom.xml test

# Run a single test class
mvn -f demo-01/file-to-kafka-app/pom.xml test -Dtest=FileToKafkaHandlerTest

# Run a single test method
mvn -f demo-01/file-to-kafka-app/pom.xml test -Dtest=FileToKafkaHandlerTest#testSpecificMethod
```

### Kubernetes/Kind Operations

#### Cluster Management
```bash
# Create the shared kind cluster (WARNING: destroys existing "single-node" cluster)
./cluster/create-cluster.sh

# Delete the entire cluster
./cluster/delete-cluster.sh
```

#### Demo Deployment (demo-01 example)
```bash
# Build Maven artifacts, Docker images, and load into kind cluster
./demo-01/scripts/01-build-and-load-images.sh

# Deploy all services to Kubernetes
./demo-01/scripts/02-deploy.sh

# Clean up only this demo (removes namespace, preserves cluster)
./demo-01/scripts/99-cleanup.sh
```

#### Viewing Logs
```bash
kubectl -n demo-01 logs -l app=quote-to-file-app -f
kubectl -n demo-01 logs -l app=file-to-kafka-app -f
kubectl -n demo-01 logs -l app=kafka-to-file-app -f
kubectl -n demo-01 logs -l app=kafka-to-db-app -f
```

#### Database Access
```bash
# Query PostgreSQL database
kubectl -n demo-01 exec -it deploy/postgres -- psql -U demo01 -d demo01 \
  -c "SELECT id, filename, creation_date FROM messages ORDER BY creation_date DESC LIMIT 5;"
```

## Architecture

### Spring Integration DSL Flow Pattern

All apps follow a consistent Spring Integration DSL pattern with three key components:

1. **FlowConfig Class**: Defines the main `IntegrationFlow` bean using Spring Integration DSL
   - Inbound adapters (file polling, Kafka message-driven, custom message sources)
   - Transformations/handlers
   - Outbound adapters (file writing, Kafka publishing, database operations)
   - Error channel configuration

2. **Handler Class** (if needed): Contains business logic for message processing
   - Typically injected into the flow's `.handle()` method
   - Handles transformations, external API calls, or complex processing

3. **Error Handling**: Each flow has a dedicated error channel
   - Named error channel (e.g., `fileToKafkaErrorChannel`)
   - Separate error logging flow that consumes from the error channel
   - Extracts failed message details and logs with context

### Flow Types in demo-01

**File Polling** (`file-to-kafka-app`):
```java
IntegrationFlow.from(Files.inboundAdapter(inputDir),
    e -> e.poller(Pollers.fixedDelay(ms).errorChannel(errorChannel)))
```

**Kafka Message-Driven** (`kafka-to-file-app`, `kafka-to-db-app`):
```java
IntegrationFlow.from(Kafka.messageDrivenChannelAdapter(consumerFactory, topic)
    .errorChannel(errorChannel))
```

**Custom Message Source** (`quote-to-file-app`):
```java
IntegrationFlow.from(customMessageSource,
    e -> e.poller(Pollers.fixedDelay(ms).errorChannel(errorChannel)))
```

### Key Patterns

**Model Objects**: Use Java records for immutability (e.g., `FileMessage`, `LogEntry`)

**Message Transformation**: Messages flow through pipelines with transformations at each step:
- Files → JSON messages with metadata (filename, logging entries)
- Each consumer adds its own logging entry to track message flow
- Timestamp prefixes prevent filename collisions (format: `yyyyMMdd-HHmmss`)

**Independent Consumers**: Multiple apps can consume the same Kafka topic independently using different consumer groups (e.g., `kafka-to-file-app` and `kafka-to-db-app` both consume `topic-01`)

**Directory Structure**: Each demo uses a `data/` subdirectory with folders like `input-01/`, `processed-01/`, `output-01/` that are mounted into Kubernetes pods via the kind cluster's broad mount

### Kind Cluster Configuration

The `cluster/kind-config.yaml` uses a RELATIVE path (`..`) for `extraMounts.hostPath`, resolved relative to the working directory when `kind create cluster` runs. This keeps the config portable across machines. The `create-cluster.sh` script always runs from the repo root, ensuring the parent workspace directory (e.g., `.../IdeaProjects`) is mounted to `/data/projects` on the kind node.

Individual demo pods mount their specific subdirectories (e.g., `/data/projects/spring-integration/demo-01/data`).

## Adding a New Demo

1. Create new directory `demo-XX/` with structure:
   ```
   demo-XX/
   ├── pom.xml          # packaging=pom, parent=root pom, own <modules>
   ├── k8s/             # Kubernetes manifests
   ├── scripts/         # Build, deploy, cleanup scripts
   ├── data/            # Mounted directories for file I/O
   └── app-name/        # Individual Spring Boot apps
   ```

2. Create Kubernetes namespace matching directory name (`demo-XX`)
3. Reuse shared `cluster/` - no new kind cluster needed
4. Follow cleanup pattern: `kubectl delete ns demo-XX`

## Configuration

**Java Version**: 17 (defined in root `pom.xml`)

**Spring Boot Version**: 3.3.13 (defined in root `pom.xml`)

**Dependencies**: Each app uses Spring Boot dependency management from parent. Common deps:
- `spring-boot-starter-integration`
- `spring-integration-file`, `spring-integration-kafka`
- `spring-boot-starter-json` (for Jackson ObjectMapper)
- `spring-boot-starter-test` (test scope)
