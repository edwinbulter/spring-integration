# Spring Integration Demo Repository

This repository is created to build experience with Spring Integration in a Kubernetes environment. It demonstrates practical integration patterns using Spring Integration DSL, Kafka, file processing, and database operations running in a local kind cluster.

## What is Spring Integration?

Spring Integration is an implementation of the Enterprise Integration Patterns (EIP) within the Spring ecosystem. It provides a lightweight messaging framework that enables building message-driven applications with support for various integration patterns such as:

- Message routing and transformation
- Channel adapters for external systems (files, databases, messaging systems, HTTP, etc.)
- Message endpoints and filters
- Splitters, aggregators, and enrichers
- Error handling and retry mechanisms

## When to Use Spring Integration

Spring Integration is particularly well-suited for:

**File-based Integration**: Processing files from directories, transforming content, and routing to different destinations based on content or metadata.

**Message-driven Architectures**: Building event-driven systems with Kafka, RabbitMQ, JMS, or other messaging systems where declarative message routing and transformation is needed.

**System Integration**: Connecting heterogeneous systems (databases, REST APIs, SOAP services, FTP servers) with minimal boilerplate code through pre-built adapters.

**Complex Message Flow Orchestration**: Scenarios requiring message splitting, aggregation, content-based routing, or multi-step transformations where the Spring Integration DSL provides clarity over imperative code.

**Legacy System Integration**: Integrating with existing systems that use protocols like FTP, SFTP, email (IMAP/POP3), or file shares.

### When to Consider Alternatives

For simpler use cases, consider these alternatives:

- **Simple REST APIs**: Use Spring MVC or Spring WebFlux directly
- **Basic Kafka consumers/producers**: Use Spring Kafka's `@KafkaListener` and `KafkaTemplate` without the Integration DSL overhead
- **Serverless/Function-based**: Use Spring Cloud Function for stateless, function-based integration
- **Batch processing**: Use Spring Batch for scheduled, large-volume data processing
- **Workflow orchestration**: Consider workflow engines (Camunda, Temporal) for complex business processes with human tasks

## Official Documentation

- [Spring Integration Reference Documentation](https://docs.spring.io/spring-integration/reference/)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/)
- [Spring Integration DSL](https://docs.spring.io/spring-integration/reference/dsl.html)

## Current Demo: File → Kafka → File/Database Pipeline

The demo demonstrates a complete message processing pipeline using Spring Integration:

- **quote-to-file-app**: Fetches quotes from an external API and writes them to files (custom message source with dynamic configuration)
- **file-to-kafka-app**: Polls a directory, transforms files into JSON messages, and publishes to Kafka
- **kafka-to-file-app**: Consumes Kafka messages and writes them as JSON files
- **kafka-to-db-app**: Consumes the same Kafka messages independently and stores them in PostgreSQL

Key learning areas:
- File polling with Spring Integration File adapter
- Kafka integration with independent consumer groups
- Message transformation and enrichment (adding metadata and logging entries)
- Error handling with dedicated error channels
- Running a complete integration pipeline in Kubernetes (kind cluster)

For detailed information including data flow, deployment instructions, and testing procedures, see [demo-01/README.md](demo-01/README.md).

## Repository Structure

The repository is structured as a monorepo to support adding future demos. For detailed information about the structure, how to set up the kind cluster, and how to add new demos, see [doc/monorepo-structure.md](doc/monorepo-structure.md).

Quick overview:
- Currently contains one demo (demo-01) with its own `pom.xml` and Kubernetes namespace
- Root `pom.xml` provides dependency management and the `cluster/` configuration
- Build with `mvn -f demo-01/pom.xml package`

## Getting Started

```bash
# 1. Create the shared kind cluster
./cluster/create-cluster.sh

# 2. Build and deploy demo-01
./demo-01/scripts/01-build-and-load-images.sh
./demo-01/scripts/02-deploy.sh

# 3. Test by adding a file
echo "hello world" > demo-01/data/input-01/test.txt

# 4. View logs
kubectl -n demo-01 logs -l app=file-to-kafka-app -f
```

See individual demo READMEs for specific instructions.
