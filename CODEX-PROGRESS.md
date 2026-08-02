# Codex project progress

Last updated: 2026-07-31 (America/Chicago)

## Current objective

Build a CI/CD pipeline for the Mall microservices after completing the Docker Compose to
Kubernetes migration and adding automated tests.

## Kubernetes migration status

The local Docker Desktop Kubernetes cluster uses the Kind provisioner with:

- 1 control-plane node: `desktop-control-plane`
- 2 worker nodes: `desktop-worker`, `desktop-worker2`
- Namespace: `mall`

Deployed workloads:

- `product-mysql` StatefulSet with a 2 GiB persistent volume
- `order-mysql` StatefulSet with a 2 GiB persistent volume
- `kafka` single-node KRaft StatefulSet with a 2 GiB persistent volume
- `product-service` Deployment with 3 replicas
- `order-service` Deployment with 1 replica
- `api-gateway` Deployment with 1 replica

Application images are published on Docker Hub:

- `greg12348/mall-product-service:latest`
- `greg12348/mall-order-service:latest`
- `greg12348/mall-api-gateway:latest`

Kubernetes manifests are stored in `kubernetes/`.

### Kafka startup fix

Kafka originally failed because `kafka-0.kafka` was not resolvable before the pod became
ready. The Kafka headless Service now has:

```yaml
publishNotReadyAddresses: true
```

This allows KRaft controller/broker communication during startup.

### Verified Kubernetes features

- Self-healing: deleted a Product Service pod and Kubernetes created a healthy replacement.
- Scaling: Product Service scaled from 1 to 3 replicas across both workers.
- Service load balancing: Product Service has three ready endpoints; 20/20 Service requests
  succeeded.
- Rolling update: simulated `v1` to `v2` using a pod-template annotation with 3/3 available.
- Rollback: returned to `v1` with 3/3 available and a healthy Gateway route.
- Persistence: inserted marker `docker-restart-20260731-01`, restarted Docker Desktop, and
  confirmed the row remained in Product MySQL.

The Product Service rolling strategy is:

```yaml
maxUnavailable: 0
maxSurge: 1
```

### Local Gateway access

Run and keep open:

```powershell
kubectl port-forward service/api-gateway 8080:8080 -n mall
```

Then use `http://localhost:8080`.

## Automated test status

### Unit and web-slice tests

There are 52 passing unit/web-slice tests across Product and Order Services.

Coverage includes:

- Product controller and business operations
- Stock availability, increase, decrease, and insufficient stock
- Order controller, creation, price calculation, and lookup
- Product-client failure behavior
- Order-created and stock-result consumer validation
- Duplicate event handling and conflicting transitions
- Product and Order outbox creation
- Outbox claim, successful publish, failed publish, and retry marking

Explicitly unfinished methods in `OrderServiceImpl` that return placeholder `null` or empty
values are not treated as correct behavior and are not tested.

### Component integration tests

There are 11 passing component integration tests in 7 `*IT` classes:

- Product repository with real MySQL 8.4
- Atomic Product stock reservation
- Product outbox repository state transitions
- Order-created consumer transaction and duplicate handling
- Order repository with real MySQL 8.4
- Order outbox repository state transitions
- Stock-result consumer transaction and duplicate handling
- API Gateway Product and Order routing with prefix stripping

Test locations:

```text
product-service/src/test/java/com/libo/mall/product/integration/
order-service/src/test/java/com/libo/mall/order/integration/
api-gateway/src/test/java/com/libo/mall/gateway/integration/
```

Maven Failsafe is configured in the root `pom.xml` to run `*IT` tests during `verify`.
Testcontainers 2.0.5 starts disposable MySQL 8.4 containers. Docker Desktop must be running,
but the Mall application does not need to be started.

Verified totals:

```text
Unit and web-slice tests:     52
Component integration tests: 11
Total passing tests:         63
Failures:                     0
Errors:                       0
```

Run fast tests:

```powershell
mvn -pl product-service,order-service,api-gateway test
```

Run unit plus component integration tests:

```powershell
mvn -pl product-service,order-service,api-gateway verify
```

### Existing deployed-system tests

- `system-tests/successful-order.ps1`: calls Product and Order Services directly.
- `system-tests/successful-order-gateway.ps1`: runs the complete order flow through Gateway.
- `system-tests/successful-order-docker.ps1`: validates Compose and wraps the Gateway test.

With the Kubernetes port-forward active, run:

```powershell
.\system-tests\successful-order-gateway.ps1
```

## Important decisions

- Kubernetes runs OCI/container images; it does not build Java source directly.
- Application images are delivered through Docker Hub so every worker can pull them.
- MySQL Services can both use port 3306 because they have different Kubernetes DNS names.
- Kafka uses a headless Service (`clusterIP: None`) for stable broker identity.
- Internal service addresses are:
  - `product-mysql:3306`
  - `order-mysql:3306`
  - `kafka:9092`
  - `product-service:8082`
  - `order-service:8084`
  - `api-gateway:8080`
- Database PVCs survive pod and Docker Desktop restarts, but local cluster deletion/reset can
  still destroy data. External database backups are required for cluster-loss protection.

## Next step

Add CI/CD and Kubernetes smoke-test automation. A recommended pipeline order is:

1. Compile and run unit/web-slice tests.
2. Run Testcontainers component integration tests.
3. Build versioned application images.
4. Push images to a registry.
5. Validate Kubernetes manifests.
6. Deploy to a test namespace.
7. Wait for Kubernetes rollouts.
8. Run the successful-order Gateway system test.
9. Promote or roll back based on the result.

Before choosing the CI implementation, decide which platform to use, such as GitHub Actions,
Jenkins, or GitLab CI.
