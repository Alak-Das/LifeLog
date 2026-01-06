# LifeLog EHR Backend

A Clinical Data Backend built with **Spring Boot 4**, **HAPI FHIR**, **MongoDB**, and **Redis**.

This project implements a **HAPI FHIR Facade** architecture, enabling standard FHIR resources to be stored as JSON documents in MongoDB, with high-performance caching via Redis.

## 🚀 Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 4.0.1
- **FHIR Standard**: HAPI FHIR 8.6.1 (R4 Model)
- **Database**: MongoDB 7.0 (Stores FHIR JSON + Search Indexes)
- **Cache**: Redis 7.2 (Caffeine-backed high-performance caching)
- **Containerization**: Docker & Docker Compose with Auto-Healing Health Checks

## 🔐 Security & Interceptors

The application follows FHIR best practices for security and logging:

### 1. Authentication
*   **Basic Auth**: Simple credentials for internal API usage.
    *   **User**: `admin` / **Pass**: `password`
*   **Public Access**: `/.well-known/smart-configuration`, `/fhir/metadata`, and `/actuator/health` are accessible without auth.

### 2. Audit Logging (Asynchronous)
*   **Interceptor**: `AuditLoggingInterceptor` captures every FHIR interaction (Patient search, Observation creation, etc.).
*   **Async Persistence**: Handled by `AuditService` with `@Async` to ensure audit logging never blocks clinical response times.
*   **Storage**: Logs are stored in the `audit_logs` collection in MongoDB, including:
    *   Timestamp, Username, Client IP.
    *   REST Operation Type, Resource Type, Resource ID.
    *   HTTP Status Code & Request URL.

### 3. Resource Validation
*   **Validation**: Every incoming `POST/PUT` request is validated against HL7 FHIR R4 structure using the `RequestValidatingInterceptor` and Caffeine caching for validation profiles.

## ⚙️ Performance Optimizations

*   **Write-Through Optimization**: Resources are saved to MongoDB, then their logical ID is derived and used to update the FHIR JSON in memory before caching in Redis. This reduces database I/O by avoiding redundant "double-save" operations.
*   **Logical ID Persistence**: The system ensures 100% fidelity between the MongoDB `_id` and the FHIR Resource `id` field during both search and read operations.

## 🔗 API Endpoints

Base URL: `http://localhost:8080/fhir`

### Core Resources

| Resource | POST | GET (by ID) | Search Parameters |
| :--- | :--- | :--- | :--- |
| **Patient** | `/Patient` | `/Patient/{id}` | `name` (family/given), `gender` |
| **Observation** | `/Observation` | `/Observation/{id}` | `subject` (Patient ID), `code` (LOINC) |
| **Condition** | `/Condition` | `/Condition/{id}` | `subject`, `code` (SNOMED) |
| **Encounter** | `/Encounter` | `/Encounter/{id}` | `subject` |
| **AllergyIntolerance** | `/AllergyIntolerance` | `/AllergyIntolerance/{id}` | `patient` |
| **MedicationRequest** | `/MedicationRequest` | `/MedicationRequest/{id}` | `subject` |
| **Appointment** | `/Appointment` | `/Appointment/{id}` | `actor` (Patient ID) |

### System & Health

| Endpoint | Description |
| :--- | :--- |
| `GET /actuator/health` | Comprehensive health check (App, DB, Redis) |
| `GET /fhir/metadata` | Capability Statement (Public) |

## 🗺️ Roadmap

- [x] **Auth**: Simple Basic Authentication.
- [x] **Validation**: HAPI FHIR Validation for strict standard compliance.
- [x] **Pagination**: Implemented HAPI FHIR Bundle Pagination for Patient Resource.
- [x] **Observability**: Spring Boot Actuator health (public) and metrics.
- [x] **Operations**: Docker Health Check for auto-healing.
- [x] **Compliance**: **Asynchronous** Audit Logging of all FHIR interactions.
- [x] **Optimization**: Single-pass write-through caching logic.
- [ ] **UI**: Build a Clinician Dashboard using Next.js/React.
- [ ] **IoMT**: Integrate with Wearables (Apple Health, Google Fit).
- [ ] **Analytics**: Population health dashboard using MongoDB Charts.

## 🛠️ Troubleshooting

**Issue**: `Port 8080 already in use`
- **Fix**: Stop other services or change `SERVER_PORT` in `docker-compose.yml`.

**Issue**: Redis Connection Failure
- **Fix**: Ensure the Redis container is healthy. If running locally without Docker, ensure Redis is installed and running on port 6379.

## 🧪 Testing

Run unit tests (Services + Controllers) with Maven:

```bash
mvn test
```
