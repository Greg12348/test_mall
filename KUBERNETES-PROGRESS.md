# Kubernetes migration progress

## Current goal

Move the Mall application from Docker Compose to Kubernetes gradually, one step at a time.

The application currently contains Docker images and Compose services for:

- API Gateway
- Order Service
- Product Service
- Kafka
- Order MySQL
- Product MySQL

Consul has been removed. Services use configurable URLs and Docker/Kubernetes DNS names.

## Completed

- Created a three-node Docker Desktop Kubernetes cluster using the Kind provisioner:
  - 1 control-plane node
  - 2 worker nodes
- Created the `mall` namespace.
- Stopped the Docker Compose application without deleting its named volumes.
- Deployed and verified `product-mysql` as a StatefulSet with persistent storage.
- Deployed and verified `order-mysql` as a StatefulSet with persistent storage.
- Deployed and verified Kafka as a single-node KRaft StatefulSet with persistent storage.
- Enabled `publishNotReadyAddresses` on the Kafka headless Service so the broker can resolve
  `kafka-0.kafka` while starting.
- Published `mall-product-service` to Docker Hub as
  `greg12348/mall-product-service:latest`.
- Deployed `product-service` and verified its in-cluster health endpoint returns `UP`.
- Verified scaling and Service load balancing by running three healthy `product-service`
  replicas across both workers, confirming three Service endpoints, and completing 20/20
  requests through `product-service:8082`.
- Verified a zero-unavailable rolling update using simulated pod-template markers from
  `v1` to `v2`, then rolled back to `v1`; all three replicas and the Gateway route remained
  healthy after the rollback.
- Published `mall-order-service` to Docker Hub as
  `greg12348/mall-order-service:latest`.
- Deployed `order-service` and verified its in-cluster health endpoint returns `UP`.
- Published `mall-api-gateway` to Docker Hub as
  `greg12348/mall-api-gateway:latest`.
- Deployed `api-gateway`, verified its health endpoint returns `UP`, and verified
  `/api/products` routes successfully to Product Service.

## Current state

The following pods are expected to be `1/1 Running` in the `mall` namespace:

- `product-mysql-0`
- `order-mysql-0`
- `kafka-0`
- Three `product-service` Deployment pods
- One `order-service` Deployment pod
- One `api-gateway` Deployment pod

## Next action

Expose the API Gateway locally for interactive testing:

```powershell
kubectl port-forward service/api-gateway 8080:8080 -n mall
```

Then test `http://localhost:8080/api/products` from the host.
