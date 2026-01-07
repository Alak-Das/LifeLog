# LifeLog EHR Backend

## Overview & Domain

**Project Name**: LifeLog EHR Backend

**Description**: A high-performance, compliant Clinical Data Repository (CDR) built on the HL7 FHIR standard. It provides a robust backend for storing, retrieving, and managing clinical health data with sub-second latency and strict regulatory compliance features.

**Problem Statement**: Modern healthcare applications require interoperable, scalable, and secure storage for complex clinical data. Building a compliant backend from scratch is resource-intensive. LifeLog solves this by offering a pre-configured, standard-compliant FHIR server facade backed by NoSQL storage for flexibility and performance.

**Target Users**:
*   **Clinician Apps**: Mobile/Web apps for doctors to view patient history.
*   **Patient Portals**: Interfaces for patients to access their own records.
*   **IoMT Devices**: Wearables and sensors pushing vital signs (Observations).
*   **External Systems**: Insurance/Payer systems via standard FHIR APIs.

**Core Business Capabilities**:
*   **Clinical Data Management**: CRUD operations for Patients, Encounters, Conditions, Observations, Immunizations, etc.
*   **Semantic Interoperability**: Enforces HL7 FHIR R4 standard structures and terminologies (LOINC, SNOMED).
*   **Real-time Notifications**: Subscription-based webhooks for event-driven workflows.
*   **Audit & Compliance**: Immutable audit trails for every access and modification.

**Domain Model Overview**:
*   **Administrative**: `Patient`, `Practitioner`, `Organization`, `Appointment`.
*   **Clinical**: `Observation`, `Condition`, `Encounter`, `Immunization`, `MedicationRequest`, `DiagnosticReport`.
*   **Infrastructure**: `Subscription` (webhooks), `AuditEvent`.

---

## Architecture & Design

**Architecture Style**: **Modular Monolith**.
The application is built as a single deployable Spring Boot artifact but structured with distinct service boundaries (Providers -> Services -> Repositories) to allow for future splitting if necessary.

**High-Level Components**:
1.  **FHIR Facade (Providers)**: The entry point (Controller layer); handles HTTP requests, parses FHIR JSON, and maps to internal services.
2.  **Service Layer**: Implements business logic, validation, ID generation, and orchestration.
3.  **Data Access Layer (Repositories)**: Interfaces with MongoDB using Spring Data.
4.  **Caching Layer**: Redis-backed cache for high-frequency reads.
5.  **Event System**: Asynchronous event handling for Audit Logging and Subscriptions.

**Communication Patterns**:
*   **Synchronous REST**: Standard FHIR interactions (GET /Patient/{id}).
*   **Asynchronous**:
    *   **Audit Logging**: `@Async` execution to prevent blocking main threads.
    *   **Webhooks**: `@Async` subscription notifications via `RestTemplate`.

**Design Patterns**:
*   **Layered Architecture**: Controller -> Service -> Repository.
*   **Facade Pattern**: HAPI FHIR `IResourceProvider` acts as a facade over the domain logic.
*   **Optimistic Locking**: Uses versioning (`@Version`) to handle concurrent updates.
*   **Decorator/Interceptor**: Uses Interceptors for Authentication and Audit Logging.

---

## Tech Stack & Dependencies

**Core**:
*   **Language**: Java 21 (LTS)
*   **Framework**: Spring Boot 3.x / 4.x
*   **Standard**: HAPI FHIR 6.x/8.x (R4)

**Data & Storage**:
*   **Database**: MongoDB 7.0 (Document Store)
*   **Cache**: Redis 7.2 (Key-Value Store)

**Infrastructure**:
*   **Containerization**: Docker, Docker Compose
*   **Build Tool**: Maven

**Libraries**:
*   **Lombok**: Boilerplate reduction.
*   **Micrometer**: Metrics instrumentation (Prometheus).

---

## Project Structure & Conventions

```
src/main/java/com/al/lifelog/
├── config/           # Spring & Lib configurations (Mongo, Redis, Security)
├── interceptor/      # Request interceptors (Audit, Auth)
├── model/            # MongoDB Entities (MongoPatient, MongoObservation)
├── provider/         # FHIR Resource Providers (Controllers)
├── repository/       # Spring Data MongoDB Repositories
├── service/          # Business Logic & Transaction Management
└── security/         # Security configurations
```

**Layering Rules**:
*   `Provider` can call `Service`.
*   `Service` can call `Repository` and other `Services`.
*   `Repository` accesses DB only.
*   **Dependency Direction**: Outer layers depend on inner layers.

**Error Handling**:
*   **Framework**: HAPI FHIR Global Exception Handlers.
*   **Mapping**:
    *   `ResourceNotFoundException` -> 404
    *   `UnprocessableEntityException` -> 422
    *   `AuthenticationException` -> 401
    *   Internal Errors -> 500 (OperationOutcome returned)

**Validation**:
*   **Input**: `RequestValidatingInterceptor` checks FHIR structural validity on POST/PUT.
*   **Business**: Service layer checks (e.g., date ranges, reference integrity).

---

## Configuration & Environments

**Environment Variables**:

| Variable | Description | Default |
| :--- | :--- | :--- |
| `SPRING_DATA_MONGODB_URI` | Connection string for MongoDB | `mongodb://mongo:27017/lifelog` |
| `SERVER_PORT` | Application Port | `8080` |
| `LOGGING_LEVEL_ROOT` | Log verbosity | `INFO` |

**Config Files**:
*   `application.yml`: Main configuration source.

**Secrets Management**:
*   Currently uses Environment Variables.
*   **Recommendation**: Use Docker Secrets or Vault for production.

---

## Setup, Installation & Local Development

**Prerequisites**:
*   Docker & Docker Compose
*   Java 21 JDK (optional if using Docker)
*   Maven 3.9+ (optional if using Docker)

**Quick Start (Docker)**:

1.  **Clone the repository**:
    ```bash
    git clone <repo-url>
    cd LifeLog
    ```

2.  **Start Services**:
    ```bash
    docker-compose up -d --build
    ```
    *   Starts Backend (8080)
    *   Starts MongoDB (27017)
    *   Starts Redis (6379)

3.  **Verify Status**:
    ```bash
    docker-compose ps
    ```

**Database**:
*   MongoDB initializes automatically.
*   Data is persisted in the `mongo-data` volume.

---

## Running & Operations

**Ports**:
*   **Local/Dev**: `http://localhost:8080`

**Health & Probes**:
*   **Liveness/Readiness**: `GET /actuator/health`
    *   Returns `200 OK` with status `UP` if DB and Redis are connected.

**Metrics**:
*   **Prometheus**: `GET /actuator/prometheus`
    *   Exposes `fhir_patient_created`, JVM metrics, HTTP latency.

**Shutdown**:
*   Supports graceful shutdown via Spring Boot (SIGTERM).

---

## API & Usage

**Base URL**: `/fhir`

**Authentication**:
*   **Type**: Basic Auth
*   **Default Credentials**: `admin` / `password`

**Global Conventions**:
*   **Response Format**: `application/fhir+json`
*   **Dates**: ISO 8601 (YYYY-MM-DDThh:mm:ss+zz:zz)

**Main Endpoints**:

| Method | Path | Description | Status Codes |
| :--- | :--- | :--- | :--- |
| `POST` | `/fhir/Patient` | Create a Patient | `201`, `400` |
| `GET` | `/fhir/Patient/{id}` | Read a Patient | `200`, `404` |
| `GET` | `/fhir/Observation` | Search Observations | `200` |

**Example Request (Create Patient)**:
```bash
curl -X POST http://localhost:8080/fhir/Patient \
  -u admin:password \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{"family": "Doe", "given": ["John"]}]
  }'
```

**Webhooks (Subscriptions)**:
*   Supports `rest-hook` channel type.
*   Triggers on resource creation/update matching criteria.

---

## Data & Persistence

**Data Model**:
*   **Storage Strategy**: Resources are stored as documents containing metadata fields + a full `fhirJson` string.
*   **Collections**:
    *   `patients`
    *   `observations`
    *   `encounters`
    *   `audit_events` (Audit Logs)

**Indexing**:
*   **Compound Indexes**: Example: `Patient` (family, given).
*   **Text Indexes**: For fuzzy search support.

**Retention**:
*   **Audit Logs**: Hard delete not implemented; assume indefinite retention for compliance.

---

## Security & Compliance

**Security Model**:
*   **Transport**: HTTPS required in production (TLS termination at ingress/LB).
*   **Authentication**: Interceptor-based checks. Public endpoints are explicitly allow-listed.

**Audit Logging**:
*   Every modify/access action is logged to `audit_events`.
*   **Fields**: Who (User/IP), What (Resource), When, Outcome.

**Input Sanitization**:
*   Strict FHIR parsing rejects malformed JSON or unknown fields.

---

## Testing & Quality

**Test Types**:
*   **Unit Tests**: JUnit 5 + Mockito. Focus on Service logic.
*   **Integration Tests**: Postman + Newman. Focus on HTTP/Controller layer and end-to-end flows.

**Run Tests Locally**:
```bash
# Unit Tests
mvn test

# Integration Tests (Requires server running)
newman run tests/postman/LifeLog_Integration_Tests.postman_collection.json -e tests/postman/LifeLog_Local.postman_environment.json
```

---

## Observability: Logging, Metrics, Tracing

**Logging**:
*   **Format**: Plain text / Console (Local).
*   **Levels**:
    *   `INFO`: Standard operations, startup.
    *   `ERROR`: Exceptions, validation failures.

**Metrics**:
*   Exposed via Micrometer/Actuator.
*   **Custom Metrics**:
    *   `fhir.patient.created` (Counter)
    *   `fhir.observation.created` (Counter)

---

## CI/CD, Deployment & Environments

**Environments**:
*   **Local**: Docker Compose (`localhost:8080`)
*   **Dev/Staging**: (Placeholder)
*   **Production**: (Placeholder)

**Deployment Strategy**:
*   **Container**: Build Docker image -> Push to Registry -> Deploy to Orchestrator (K8s/ECS).

---

## Integrations & External Systems

*   **Subscribers**: Any external system registering a `rest-hook` Subscription.
    *   **Retry Policy**: Basic try/catch (Currently). Recommended to implement exponential backoff via Queue.

---

## Governance, Ownership & Future Work

**Owner**: Engineering Team (LifeLog)

**Roadmap**:
*   [ ] OAuth2 / SMART on FHIR full compliance.
*   [ ] Advanced Search Parameters (Chained params).
*   [ ] Terminology Server Integration.
*   [ ] Bulk Data Export ($export).
