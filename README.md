# AccessZero — Identity Breach Containment Platform

[![CI/CD Pipeline](https://github.com/Codeguruu03/AccessZero/actions/workflows/ci.yml/badge.svg)](https://github.com/Codeguruu03/AccessZero/actions/workflows/ci.yml)
[![Java 17](https://img.shields.io/badge/Java-17-blue?logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Keycloak](https://img.shields.io/badge/Keycloak-25-orange?logo=keycloak)](https://www.keycloak.org/)
[![Terraform](https://img.shields.io/badge/Terraform-1.9.5-blueviolet?logo=terraform)](https://www.terraform.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **AccessZero** is an enterprise-grade **Identity Breach Containment and Zero-Access Verification Platform**. It enables Security Operations teams to instantly detect, contain, and cryptographically verify the revocation of all access paths for a compromised identity — across Keycloak, OpenLDAP, OAuth 2.0 tokens, and SAML 2.0 federated applications — in a matter of seconds.

---

## Table of Contents

- [Problem Statement](#problem-statement)
- [Architecture Overview](#architecture-overview)
- [Key Features](#key-features)
- [Technology Stack](#technology-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Local Development](#local-development)
  - [Docker Compose](#docker-compose)
  - [Kubernetes Deployment](#kubernetes-deployment)
- [API Reference](#api-reference)
- [Security Design](#security-design)
  - [Two-Person Approval](#two-person-approval)
  - [Containment State Machine](#containment-state-machine)
  - [Immutable Audit Trail](#immutable-audit-trail)
- [Terraform Infrastructure](#terraform-infrastructure)
- [CI/CD Pipeline](#cicd-pipeline)
- [Project Structure](#project-structure)
- [Contributing](#contributing)

---

## Problem Statement

When an enterprise identity is compromised (credential theft, insider threat, supply-chain attack), a Security Engineer faces a critical question:

> **"How many systems does this user have access to, and how do I revoke ALL of it — right now?"**

In a modern enterprise, a single user may have:

- 7+ active SSO sessions (Keycloak, Okta)
- 14+ OAuth 2.0 refresh tokens across SaaS applications
- LDAP group memberships granting privileged access to internal systems
- SAML 2.0 federated sessions in HR, CRM, and ERP systems
- Active VPN tunnels and API gateway tokens

Manually revoking these across fragmented systems is **slow, error-prone, and incomplete**.

**AccessZero solves this** by computing the full identity blast radius, executing a coordinated Kill Switch containment across all integrated systems, and providing cryptographic proof that access is fully revoked.

---

## Architecture Overview

```
                         IT ADMIN / SecOps Engineer
                                    │
                                    ▼
                           AccessZero Console
                                    │
                                    ▼
                          Spring Boot Control Plane
                                    │
               ┌────────────────────┼────────────────────┐
               │                    │                    │
               ▼                    ▼                    ▼
        Identity Graph          Containment          Verification
           Engine                  Engine               Engine
               │                    │                    │
               └────────────────────┼────────────────────┘
                                    │
           ┌────────────────────────┼────────────────────────┐
           │                        │                        │
           ▼                        ▼                        ▼
       Keycloak                 OpenLDAP               Applications
     (OAuth / OIDC)         (Groups / Directory)    (OIDC / SAML / API)
           │                        │
           └────────────────────────┘
                                    │
                                    ▼
                               PostgreSQL
                            (Identity Graph DB)
                                    │
                                    ▼
                    Immutable Audit Log (SHA-256 Chain)
```

---

## Key Features

| Feature | Description |
|---|---|
| **Blast Radius Engine** | Computes all access paths (User → Group → Role → Application) using a directed identity graph. Calculates risk score, disruption level, and affected applications before any destructive action. |
| **Containment Engine** | Executes a coordinated Kill Switch across Keycloak, OpenLDAP, OAuth token store, and SAML assignments in a single transactional operation. |
| **Two-Person Approval** | Enforces four-eyes approval for containment of high-risk identities, preventing self-approval. Full approval lifecycle tracked in the database. |
| **Zero-Access Verification** | After containment, independently verifies revocation across every provider (Keycloak, LDAP, OAuth, SAML) and produces a per-provider verification matrix with residual risk analysis. |
| **Containment Simulation** | Non-destructive dry-run that predicts exactly what will be revoked, sessions terminated, and groups removed — before any real action is taken. |
| **Identity Graph** | Directed graph (User → Group → Role → Application, User → Token → Application, User → Session → Application) enabling complete lateral access path discovery. |
| **Immutable Audit Trail** | Every containment action is recorded with a SHA-256 hash chain, providing tamper-evident forensic-grade audit logs. |
| **Rollback / Restore** | Controlled rollback of containment operations, restoring user state, re-activating access paths, and recording a full restoration audit event. |
| **CLI Interface** | Bash/PowerShell admin CLI for incident response automation (`accesszero analyze`, `accesszero contain`, `accesszero verify`). |

---

## Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **API Server** | Java 17 + Spring Boot 3.2 | Identity control-plane REST API |
| **Identity Provider** | Keycloak 25 | OAuth 2.0 / OIDC token lifecycle management |
| **Enterprise Directory** | OpenLDAP | Group memberships, privileged group management |
| **Auth Protocols** | OAuth 2.0, OIDC, SAML 2.0 | Multi-protocol access revocation |
| **Database** | PostgreSQL 16 / H2 (test) | Identity graph and access path persistence |
| **Container Runtime** | Docker + Docker Compose | Local development environment |
| **Orchestration** | Kubernetes (Kustomize) | Production multi-service deployment |
| **Infrastructure as Code** | Terraform 1.9.5 | Reproducible cloud infrastructure (AWS / Azure) |
| **CI/CD** | GitHub Actions | Automated build, test, and validation pipeline |
| **Automation** | Bash / PowerShell | Incident response CLI tooling |

---

## Getting Started

### Prerequisites

- Java 17+ (Temurin / OpenJDK)
- Maven 3.9+
- Docker Desktop 4.x+
- kubectl 1.28+
- Terraform 1.9.5+
- Python 3.11+ (for K8s manifest validation)

### Local Development

```bash
# 1. Clone the repository
git clone https://github.com/Codeguruu03/AccessZero.git
cd AccessZero

# 2. Run the full test suite (against embedded H2)
mvn test

# 3. Start the Spring Boot application (H2 in-memory database)
mvn spring-boot:run

# 4. API will be available at:
#    http://localhost:8080
#    H2 Console: http://localhost:8080/h2-console
```

### Docker Compose

Starts the full local IAM stack: AccessZero API, PostgreSQL 16, Keycloak 25, and OpenLDAP.

```bash
docker-compose up -d

# Services:
#   AccessZero API  → http://localhost:8080
#   Keycloak Admin  → http://localhost:8081
#   OpenLDAP        → ldap://localhost:389
#   PostgreSQL      → localhost:5432
```

### Kubernetes Deployment

```bash
# Apply all manifests with Kustomize
kubectl apply -k k8s/

# Verify pods are running
kubectl get pods -n accesszero

# Port-forward the API
kubectl port-forward svc/accesszero-api 8080:8080 -n accesszero
```

---

## API Reference

### Identity Management

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/identities` | List all identities with blast radius summary |
| `GET` | `/api/v1/identities/{userId}` | Get identity by ID |
| `GET` | `/api/v1/identities/username/{username}` | Get identity by username |
| `POST` | `/api/v1/identities/sync/{username}` | Sync identity from Keycloak + LDAP |

### Blast Radius Analysis

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/blast-radius/{userId}` | Calculate full access blast radius |
| `GET` | `/api/v1/blast-radius/username/{username}` | Calculate by username |

### Containment Operations

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/containment/request` | Initiate containment (standard or emergency override) |
| `POST` | `/api/v1/containment/{operationId}/approve` | Two-person approval for pending containment |
| `GET` | `/api/v1/containment/operations` | List all containment operations |
| `POST` | `/api/v1/containment/{operationId}/rollback` | Roll back a completed containment |

### Simulation (Non-Destructive Dry Run)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/simulation/{userId}` | Simulate containment without executing it |
| `GET` | `/api/v1/simulation/username/{username}` | Simulate by username |

### Verification

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/verification/username/{username}` | Verify zero-access across all providers |
| `GET` | `/api/v1/verification/user/{userId}` | Verify by user ID |

### Audit

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/audit/events` | List all immutable audit events |
| `GET` | `/api/v1/audit/events/{target}` | List events for a specific target user |

---

## Security Design

### Two-Person Approval

For identities with a risk score ≥ 75 (CRITICAL), containment **requires approval from a second administrator**:

```
Admin A ──► Request Containment ──► CONTAINMENT_PENDING
                                          │
Admin B ──────────────────────────────── Approve
                                          │
                                    CONTAINING ──► CONTAINED
```

- Admin A **cannot approve their own request** (enforced at service layer).
- Approval is tracked with actor identity and timestamp in the immutable audit log.
- Emergency CISO override bypasses the approval gate for active breach scenarios.

### Containment State Machine

```
NORMAL ──► SUSPECTED ──► ANALYZING ──► CONTAINMENT_PENDING
                                                │
                                          CONTAINING
                                                │
                                          VERIFYING
                                          /        \
                                    CONTAINED    PARTIAL
                                        │            │
                                    RECOVERY   MANUAL_ACTION
```

### Immutable Audit Trail

Every action produces a tamper-evident audit record:

```
[AUDIT #47] 2026-08-12 10:31:22 UTC
  Actor:   anil.admin
  Action:  CONTAIN_IDENTITY
  Target:  rahul.sharma
  Result:  PARTIAL
  Hash:    SHA-256(prev_hash + actor + action + target + timestamp)
  
  Actions Executed:
    [1] Keycloak: Account disabled (ENABLED=FALSE)
    [2] OAuth:    14 refresh tokens revoked, 7 sessions terminated
    [3] LDAP:     Groups [finance, payroll-admin, vpn-users] removed
    [4] SAML:     45 / 47 access paths revoked
    [5] Verify:   2 residual risks (manual action required)
```

---

## Terraform Infrastructure

Infrastructure-as-Code for cloud deployment (AWS/Azure):

```bash
cd terraform

# Initialize providers
terraform init

# Preview infrastructure changes
terraform plan

# Apply infrastructure
terraform apply
```

Provisions:
- **Kubernetes cluster** (EKS / AKS)
- **Keycloak** deployment with realm configuration
- **PostgreSQL** managed database
- **Network policies** and **IAM roles**

---

## CI/CD Pipeline

The GitHub Actions pipeline (`/.github/workflows/ci.yml`) runs on every push to `main`:

| Stage | Tool | What it validates |
|---|---|---|
| **Unit & Integration Tests** | Maven + JUnit 5 | 23 tests across 9 test classes |
| **Package Build** | Maven | Spring Boot fat JAR production build |
| **K8s Manifest Validation** | kubectl kustomize + PyYAML | Validates all 8 Kubernetes manifests |
| **Terraform Format** | Terraform 1.9.5 | `terraform fmt -check` on all HCL files |
| **Terraform Validate** | Terraform 1.9.5 | Schema and provider validation |

---

## Project Structure

```
AccessZero/
├── .github/
│   └── workflows/ci.yml          # GitHub Actions CI/CD pipeline
├── src/
│   ├── main/java/com/accesszero/
│   │   ├── AccessZeroApplication.java
│   │   ├── adapter/
│   │   │   ├── keycloak/          # Keycloak Admin REST adapter
│   │   │   └── ldap/              # OpenLDAP directory adapter
│   │   ├── config/
│   │   │   └── DataSeeder.java    # Test data seed (Rahul Sharma scenario)
│   │   ├── controller/            # REST API controllers
│   │   ├── domain/
│   │   │   ├── entity/            # JPA entities (User, Group, Role, Session ...)
│   │   │   ├── enums/             # ContainmentStatus, UserStatus, PathType ...
│   │   │   └── graph/             # Identity graph model (IdentityGraph, GraphNode)
│   │   ├── dto/                   # Request/Response DTOs (Java Records)
│   │   ├── repository/            # Spring Data JPA repositories
│   │   └── service/
│   │       ├── AccessPathResolverService.java
│   │       ├── AuditService.java
│   │       ├── BlastRadiusEngine.java
│   │       ├── ContainmentEngine.java
│   │       ├── ContainmentSimulationEngine.java
│   │       ├── IdentityGraphEngine.java
│   │       ├── IdentitySyncService.java
│   │       └── VerificationEngine.java
│   └── test/
│       ├── java/com/accesszero/   # 23 integration and unit tests
│       └── resources/
│           └── application.yml    # Test configuration (H2 in-memory)
├── k8s/                           # Kubernetes manifests (Kustomize)
│   ├── 00-namespace.yaml
│   ├── 01-configmap-secret.yaml
│   ├── 02-postgres.yaml
│   ├── 03-keycloak.yaml
│   ├── 04-openldap.yaml
│   ├── 05-accesszero-api.yaml
│   ├── 06-test-apps.yaml
│   └── kustomization.yaml
├── terraform/                     # Infrastructure as Code
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   ├── k8s.tf
│   └── keycloak.tf
├── docker-compose.yml             # Local IAM stack
├── .gitattributes                 # LF line ending enforcement
├── pom.xml                        # Maven build descriptor
└── README.md
```

---

## Contributing

1. Fork the repository and create a feature branch (`git checkout -b feature/my-feature`)
2. Ensure all tests pass locally: `mvn test`
3. Commit with a conventional commit message: `git commit -m "feat: add feature X"`
4. Push your branch and open a Pull Request against `main`
5. The CI/CD pipeline must be green before merging

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

<div align="center">
  <sub>Built for enterprise identity security. Designed for Security Operations.</sub>
</div>
