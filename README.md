<div align="center">

# LifeLog EHR Backend
### High-Performance Clinical Data Repository (CDR)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.1-brightgreen)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-Cache-red)](https://redis.io/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Database-green)](https://www.mongodb.com/)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

*Built on the [HL7 FHIR R4](http://hl7.org/fhir/R4/) Standard*

</div>

---

## 📋 Table of Contents
- [Executive Summary](#-executive-summary)
- [Key Features](#-key-features)
- [Architecture & Design](#-architecture--design)
  - [System Architecture](#system-architecture-diagram)
  - [Request Lifecycle](#-request-lifecycle-sequence)
  - [Service Responsibilities](#-service-responsibilities)
- [Technology Stack](#-technology-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Quick Start](#quick-start-with-docker)
- [API Reference](#-api-reference)
  - [Clinical Resources](#clinical-resources)
  - [Administrative Resources](#administrative-resources)
  - [System](#system-endpoints)
- [Operations Manual](#-operations-manual)
  - [Configuration](#configuration)
  - [Observability](#observability)
  - [Troubleshooting](#troubleshooting)
- [Developer Guide](#-developer-guide)
- [Roadmap](#-roadmap)

---

## 📝 Executive Summary

**LifeLog EHR Backend** is a purpose-built, compliant backend component for modern Electronic Health Record (EHR) systems. It solves the complexity of building a healthcare data platform from scratch by providing a pre-configured **FHIR Facade** that is secure, scalable, and interoperable out of the box.

Unlike traditional SQL-based EHRs, LifeLog leverages **MongoDB** to natively store FHIR JSON documents, offering superior flexibility for diverse clinical data types, while maintaining strict ACID compliance and validation rules.

**Target Use Cases**:
*   **Clinician Dashboards**: Real-time access to patient vitals and history.
*   **IoMT Data Ingestion**: High-throughput writes from wearables.
*   **Patient Portals**: Secure, granular access for patients.

---

## ✨ Key Features

### 🩺 Clinical
*   **Comprehensive Resource Support**: CRUD operations for `Patient`, `Observation`, `Encounter`, `Condition`, `Immunization`, `MedicationRequest`, and more.
*   **Semantic Validation**: Strictly enforces FHIR R4 structure and cardinality.
*   **Advanced Search**: Supports chained parameters (e.g., `subject.name`), dates, and token searches.

### 🛡️ Security & Compliance
*   **Audit Trails**: Asynchronous, immutable logging of every data access (`AuditEvent`).
*   **Optimistic Locking**: Prevents "lost updates" using standard FHIR versioning (`Weak ETag`).
*   **Authentication**: Basic Auth (**RBAC Enabled**)

| Role | Username | Password | Access |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `password` | Administrative Resources (`Patient`, `Practitioner`, `Organization`, `Appointment`) |
| **Clinical** | `clinician` | `password` | Clinical Resources (`Observation`, `Condition`, `Encounter`, etc.) |
*   **Input Sanitization**: Rejects malformed or unrecognized data structures.

### 🚀 Technical
*   **Event-Driven Architecture**: Non-blocking **Webhooks** (`Subscription`) for real-time integrations.
*   **Write-Through Caching**: **Redis** layer for sub-millisecond read latency on hot data.
*   **Scalable**: Stateless architecture ready for container orchestration (Kubernetes).

---

## 🏗 Architecture & Design

**Style**: Modular Monolith (FHIR Facade)

The system behaves as a protocol translator:
1.  **Ingest**: Receives standard FHIR REST requests.
2.  **Process**: Validates logic, enforces rules, and generates events.
3.  **Persist**: Stores data as optimized Documents (MongoDB) and KV pairs (Redis).

### System Architecture Diagram

```mermaid
graph TD
    Client[Client Apps / IoMT] -->|HTTPS/REST| App[LifeLog EHR Backend]
    
    subgraph App [Spring Boot Application]
        direction TB
        Security[Security Filter Chain] -->|Auth Check| Interceptors
        
        subgraph Interceptors [HAPI FHIR Interceptors]
            Valid[Request Validation]
            AuditInt[Audit Logging Hook]
        end
        
        Interceptors -->|Valid Request| Providers[FHIR Resource Providers]
        
        subgraph Domain [Business Logic Layer]
            Providers -->|Map DTO| Services[Service Layer]
            Services -->|Logic & Rules| Repos[Repositories]
            Services -.->|Async| EventBus[Async Task Executor]
        end
        
        EventBus -->|Fire & Forget| AuditAsync[Audit Service]
        EventBus -->|Notify| SubAsync[Subscription Service]
    end
    
    subgraph Data [Persistence & Caching]
        Repos <-->|Read/Write| Mongo[(MongoDB)]
        Repos <-->|Cache Hit/Miss| Redis[(Redis Cache)]
        AuditAsync -->|Write Log| Mongo
    end
    
    SubAsync -.->|HTTP Webhook| External[External Systems]
```

### 🔄 Request Lifecycle (Sequence)

How a **POST /Observation** moves through the system:

```mermaid
sequenceDiagram
    participant Client
    participant Provider as ObservationProvider
    participant Service as ObservationService
    participant DB as MongoDB
    participant Cache as Redis
    participant Async as AsyncExecutor

    Client->>Provider: POST /Observation (JSON)
    Provider->>Provider: Validate HTTP/Auth
    Provider->>Service: createObservation(DTO)
    Service->>Service: Generate ID & Version
    Service->>DB: Save(MongoObservation)
    Service->>Cache: Set("observation:id", JSON)
    Service->>Async: Fire Events (Audit, Subscription)
    
    par Async Tasks
        Async->>DB: Save(AuditLog)
        Async->>Client: Webhook Notification (if Subscribed)
    end
    
    Service-->>Provider: Return Created Object
    Provider-->>Client: 201 Created (Location Header)
```

### 🧠 Service Responsibilities

| Service | Responsibility | Key Interactions |
| :--- | :--- | :--- |
| **PatientService** | Identity Management | Indexed search by Name/Gender. |
| **ObservationService** | Clinical Data Management | High-volume writes, Redis Caching. |
| **SubscriptionService** | Webhooks | Matches `Criteria` (e.g., `code=123`) -> POST to URL. |
| **AuditService** | Compliance | Asynchronous reliable logging. |

---

## 💻 Technology Stack

| Component | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Core** | Java | 21 (LTS) | Language |
| **Framework** | Spring Boot | 3.x / 4.x | App Framework |
| **Standard** | HAPI FHIR | R4 | FHIR Parsing & Logic |
| **Database** | MongoDB | 7.0 | Document Store |
| **Cache** | Redis | 7.2 | Performance Layer |
| **Build** | Maven | 3.9+ | Dependency Management |
| **Ops** | Docker | Latest | Containerization |

---

## 🏁 Getting Started

### Prerequisites
*   **Docker Desktop** (Engine 20.10+)
*   **Git**

### Quick Start (with Docker)
Run the entire stack (App + Mongo + Redis) in one command:

```bash
# 1. Clone Repo
git clone https://github.com/al-das/lifelog-server.git
cd lifelog-server

# 2. Start Stack
docker-compose up -d --build

# 3. Verify (Wait ~30s for startup)
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### Manual Build
If you prefer running without Docker:
```bash
# Ensure local Mongo (27017) and Redis (6379) are running
mvn clean install
java -jar target/lifelog-ehr-0.0.1-SNAPSHOT.jar
```

---

## 📚 API Reference

**Base URL**: `http://localhost:8080/fhir`

> [!TIP]
> All resources support `_format=json` and `_pretty=true` parameters.

### 1. Patient Scenarios (Role: **Admin**)
| Method | Endpoint | Params | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/Patient` | N/A | Create a new patient. |
| `GET` | `/Patient/{id}` | N/A | Read patient details. |
| `GET` | `/Patient` | `name`, `gender`, `_id` | Search patients. |
| `GET` | `/Patient` | `_revinclude=Observation:patient` | Search with reverse include (include Observations). |
| `PUT` | `/Patient/{id}` | N/A | Update patient record. |
| `DELETE` | `/Patient/{id}` | N/A | Delete patient record. |
| `GET` | `/Patient/{id}/_history` | N/A | Retrieve version history of a patient. |

### 2. Clinical Resources (Role: **Clinical**)
| Resource | Method | Endpoint | Supported Params |
| :--- | :--- | :--- | :--- |
| **Observation** | `POST` | `/Observation` | N/A |
| | `GET` | `/Observation` | `subject`, `subject.name`, `date` (supports prefixes `gt`, `lt`), `category` |
| **Condition** | `POST` | `/Condition` | N/A |
| | `GET` | `/Condition` | `subject`, `clinical-status` |
| **Encounter** | `POST` | `/Encounter` | N/A |
| | `GET` | `/Encounter` | `subject`, `date` |
| **AllergyIntolerance** | `POST` | `/AllergyIntolerance` | N/A |
| | `GET` | `/AllergyIntolerance` | `patient` |
| **MedicationRequest** | `POST` | `/MedicationRequest` | N/A |
| | `GET` | `/MedicationRequest` | `subject`, `status` |
| **Immunization** | `POST` | `/Immunization` | N/A |
| | `GET` | `/Immunization` | `patient`, `date` |
| **DiagnosticReport** | `POST` | `/DiagnosticReport` | N/A |
| | `GET` | `/DiagnosticReport` | `subject`, `status` |

### 3. Administrative Resources (Role: **Admin**)
| Resource | Method | Endpoint | Description |
| :--- | :--- | :--- | :--- |
| **Practitioner** | `POST` | `/Practitioner` | Register a new healthcare professional. |
| | `GET` | `/Practitioner` | Search by `name`. |
| **Organization** | `POST` | `/Organization` | Register a healthcare organization. |
| | `GET` | `/Organization` | Search by `name`. |
| **Appointment** | `POST` | `/Appointment` | Schedule an appointment. |
| | `GET` | `/Appointment` | Search by `actor` (patient/practitioner). |
| **Subscription** | `POST` | `/Subscription` | Register a webhook subscription. |

### 4. System & Metadata (Public)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/metadata` | **CapabilityStatement**: Server capabilities & conformance. |
| `GET` | `/.well-known/smart-configuration` | **SMART Configuration**: OAuth2 endpoints (future use). |
| `GET` | `/actuator/health` | **Health Check**: Kubernetes Liveness Probe. |
| `GET` | `/actuator/prometheus` | **Metrics**: Prometheus scraper endpoint. |

---

## ⚙️ Operations Manual

### Configuration
Key environment variables in `docker-compose.yml`:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SPRING_DATA_MONGODB_URI` | Mongo Connection | `mongodb://mongo:27017/lifelog` |
| `SPRING_REDIS_HOST` | Redis Host | `redis` |
| `SERVER_PORT` | App Port | `8080` |
| `LOGGING_LEVEL_ROOT` | Global Log Level | `INFO` |
| `SPRING_SECURITY_ADMIN_USERNAME` | Admin Username | `admin` |
| `SPRING_SECURITY_ADMIN_PASSWORD` | Admin Password | `password` |
| `SPRING_SECURITY_CLINICAL_USERNAME` | Clinical Username | `clinician` |
| `SPRING_SECURITY_CLINICAL_PASSWORD` | Clinical Password | `password` |

### Observability
*   **Metrics**: Prometheus scraper available at `/actuator/prometheus`.
*   **Logging**: STDOUT (Docker Logs).

### Troubleshooting

<details>
<summary><strong>❌ Port 8080 is already in use</strong></summary>

Stop the conflicting service or change `SERVER_PORT` in `docker-compose.yml` and recreate containers.
</details>

<details>
<summary><strong>❌ MongoSocketOpenException</strong></summary>

Ensure the `mongo` container is healthy. If running locally (not Docker), change connection string to `localhost:27017`.
</details>

---

## 💻 Developer Guide

### Project Structure
```
src/main/java/com/al/lifelog/
├── config/           # App Configuration (Beans)
├── interceptor/      # AOP (Audit, Auth)
├── model/            # Mongo Documents
├── provider/         # FHIR Controllers
├── repository/       # DB Access
├── service/          # Business Logic
```

### Running Tests
```bash
# Unit Tests (Fast)
mvn test

# Integration Tests (Requires running server)
# Uses Newman (Postman CLI)
# Integration Tests (Requires running server)
# Uses Newman (Postman CLI)
newman run tests/postman/LifeLog_Integration_Tests.postman_collection.json \
  -e tests/postman/LifeLog_Local.postman_environment.json

### Security Verification
Verify Role-Based Access Control (RBAC) rules:
```powershell
./verify_rbac.ps1
```
```

---

## 🗺 Roadmap

- [x] **Core FHIR Resources** (Patient, Obs, Condition, etc.)
- [x] **Advanced Search** (Chained Parameters)
- [x] **Role-Based Access Control (RBAC)**
- [x] **Docker Support**
- [ ] **OAuth2 / SMART on FHIR Compliance**
- [ ] **Terminology Services** (Validation against LOINC/SNOMED)
- [ ] **Bulk Export ($export)**

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<div align="center">
    <sub>Maintained by the LifeLog Engineering Team.</sub>
</div>
