# Codex project progress

Last updated: 2026-08-03 (America/Chicago)

This document records completed work, decisions, commands, and current state. Debugging and
error-resolution history is intentionally omitted.

## 2026-07-31: Local Kubernetes and automated tests

The Mall project was migrated from Docker Compose to Docker Desktop Kubernetes. The `mall`
namespace contains Product MySQL, Order MySQL, Kafka, Product Service, Order Service, and API
Gateway. MySQL and Kafka use persistent volumes.

Automated test coverage was completed for:

- 52 unit and web-slice tests
- 11 component integration tests using Testcontainers and MySQL 8.4
- Product and Order repository behavior
- Stock reservation and event handling
- Transactional outbox behavior
- API Gateway route forwarding
- Complete successful-order system flow

Run the fast tests:

```powershell
mvn -pl product-service,order-service,api-gateway test
```

Run unit and component tests:

```powershell
mvn -pl product-service,order-service,api-gateway verify
```

Run the Gateway system test when a Gateway endpoint is available:

```powershell
$env:GATEWAY_URL = "http://localhost:8080"
.\system-tests\successful-order-gateway.ps1
```

## 2026-08-01: Jenkins CI/CD

Jenkins was installed locally and configured for the GitHub repository:

```text
https://github.com/Greg12348/test_mall.git
```

The pipeline tests the services, builds versioned images, pushes images, deploys Kubernetes
workloads, waits for rollouts, and runs the successful-order Gateway test.

Jenkins credential identifiers:

- `dockerhub-credential`: Docker Hub username/token
- `mall-kubeconfig`: original local Kubernetes kubeconfig
- `aws-mall-credentials`: AWS access-key ID and secret access key

No credential values are stored in Git.

Docker Desktop must be running while local Jenkins builds container images. Docker Desktop is
not required for workloads that are already running in AWS.

GitHub polling schedule:

```text
H/5 * * * *
```

This asks Jenkins to check for repository changes approximately every five minutes, with a
Jenkins-selected offset.

## 2026-08-02: AWS foundation

AWS deployment configuration:

- AWS account: `753974169033`
- Region: `us-east-1`
- AWS CLI profile: `mall-aws`
- EKS cluster: `mall-test`
- Kubernetes version: 1.34
- Managed node group: `mall-workers`
- Current learning node size: one `t3.large`
- Default EBS StorageClass: encrypted `gp3`
- EBS CSI managed add-on enabled

ECR repositories:

```text
753974169033.dkr.ecr.us-east-1.amazonaws.com/mall-product-service
753974169033.dkr.ecr.us-east-1.amazonaws.com/mall-order-service
753974169033.dkr.ecr.us-east-1.amazonaws.com/mall-api-gateway
```

The initial AWS application image tag was `aws-test-1`. Jenkins now publishes immutable tags
in the form:

```text
BUILD_NUMBER-GIT_COMMIT
```

AWS infrastructure and deployment files are stored under `aws/`. The Kubernetes base is under
`kubernetes/`, and the AWS-specific Kustomize overlay is under `aws/kubernetes/`.

## 2026-08-03: AWS application and data migration

The following workloads are running in the EKS `mall` namespace:

- Product Service
- Order Service
- API Gateway
- Product MySQL
- Order MySQL
- Single-node Kafka for the learning environment

Persistent storage:

- Product MySQL: encrypted 2 GiB `gp3` EBS volume
- Order MySQL: encrypted 2 GiB `gp3` EBS volume
- Kafka: encrypted 2 GiB `gp3` EBS volume

The AWS overlay uses resource requests and limits sized for the current single learning node.
Product Service is limited to one replica in this environment.

### Database migration tooling

Database tools are stored under `database-migration/`:

- `migrate-databases.ps1`: configuration-driven MySQL export/import utility
- `migration-config.example.json`: Product and Order mapping example
- `sample-product-dump.sql`: synthetic, repeatable Product database sample

Real database dumps, backups, passwords, and local migration configuration are excluded from
Git.

The synthetic database migration inserted and verified:

```text
ID:          10001
Name:        Migrated AWS Demo Product
Price:       149.99
Stock:       25
```

### AWS validation

Run the consolidated validation script:

```powershell
.\aws\validate-migration.ps1
```

It verifies:

- Correct EKS context
- Deployment and StatefulSet rollouts
- Bound `gp3` persistent volume claims
- ECR application images
- Migrated Product record in AWS MySQL
- Gateway access to the migrated Product
- Complete successful-order workflow through Kafka

Latest validation result:

```text
AWS MIGRATION VALIDATION: SUCCESS
```

The verified order test created Product `10002`, created Order `1`, reached
`STOCK_RESERVED`, and reduced stock from 10 to 8.

## 2026-08-03: AWS Jenkins deployment

The Jenkins pipeline now performs:

```text
GitHub checkout
-> Maven verification
-> Docker image builds
-> ECR pushes
-> EKS manifest deployment
-> Kubernetes rollout verification
-> Gateway successful-order test
```

Relevant commits:

- `8d50216`: AWS EKS migration configuration and validation
- `78b0b83`: Jenkins deployment to AWS ECR and EKS
- `6093e02`: API Gateway exposure through AWS ALB

## 2026-08-03: Public AWS Application Load Balancer

The official AWS Load Balancer Controller is installed in `kube-system` with two ready
replicas. It uses an IRSA service account and a dedicated IAM policy.

One internet-facing ALB routes public HTTP traffic to API Gateway. Product Service, Order
Service, MySQL, and Kafka remain internal.

Public endpoint:

```text
http://k8s-mall-apigatew-524487532f-169527717.us-east-1.elb.amazonaws.com
```

Verified public request:

```text
GET /api/products/10001
```

The endpoint returned the migrated Product successfully and `/actuator/health` returned `UP`.

The current public endpoint uses HTTP and does not have application authentication. It is for
learning use only and must not contain sensitive data. The ALB, EKS control plane, EC2 node,
and EBS volumes continue generating AWS charges while they exist.

## Current architecture

```text
Internet
   -> AWS Application Load Balancer
   -> Spring API Gateway on EKS
      -> Product Service -> Product MySQL on EKS/EBS
      -> Order Service   -> Order MySQL on EKS/EBS
      -> Kafka on EKS/EBS
```

## Production roadmap

The learning deployment is functionally complete. A production-oriented upgrade should be
implemented in this order:

1. Authentication and authorization
2. HTTPS using AWS Certificate Manager
3. RDS MySQL with Multi-AZ and automated backups
4. At least two EKS worker nodes across Availability Zones
5. Multiple application replicas, disruption budgets, and topology spreading
6. AWS Secrets Manager integration
7. CloudWatch logs, metrics, dashboards, and alarms
8. S3 for product images and object storage
9. MSK only when Kafka compatibility, replay, ordering, or multiple consumers are required
10. Infrastructure as code and separate development, staging, and production environments

For a small production workload that does not require Kafka semantics, SQS, SNS, or EventBridge
may be simpler and less expensive than MSK.

## Cost and cleanup reminder

The current test environment is billable. When learning work is finished, remove resources in
dependency order: public Ingress/ALB, application workloads and PVCs, node group, EKS cluster,
and controller IAM resources. Retain only ECR images or backups that are intentionally needed.
