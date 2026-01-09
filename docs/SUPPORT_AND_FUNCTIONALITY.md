# LifeLog EHR - Support & Functionality Reference

## 1. System Overview
**LifeLog EHR** is a modular, high-performance Clinical Data Repository (CDR) built on the HL7 FHIR R4 standard. It serves as a backend "FHIR Facade" that abstracts complex clinical logic and storage optimizations behind a standard RESTful API.

### Core Architecture
- **Style**: Modular Monolith (Spring Boot).
- **Standards**: HL7 FHIR R4 (Strict JSON validation).
- **Persistence**: Hybrid model using MongoDB (Documents) for flexibility and Redis (Key-Value) for speed.
- **Security**: Granular Role-Based Access Control (RBAC) with Authority-driven enforcement.

### Technology Stack
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.2+
- **FHIR Engine**: HAPI FHIR 6.x
- **Database**: MongoDB 6.0+
- **Caching**: Redis 7.x
- **Containerization**: Docker & Docker Compose

---

## 2. Business Logic & Capabilities

### 2.1 FHIR Resource Management
The system supports the following Clinical and Administrative modules. Each module implements strict semantic validation against the FHIR R4 specification.

| Module | Resource Type | Key Business Rules |
| :--- | :--- | :--- |
| **Patient Administration** | `Patient` | **Write**: Restricted to Registrars.<br>**Read**: Clinical staff.<br>**Search**: Token/String search on Name, Gender.<br>**Versioning**: Full history maintained. |
| **Clinical Observations** | `Observation` | **High Volume**: Optimized for IoMT ingestion.<br>**Caching**: Aggressive caching (10 min TTL).<br>**Notifications**: Triggers `Subscription` webhooks on create. |
| **Encounters & Visits** | `Encounter` | Links interactions between Patient and Practitioner.<br>**Integrity**: Must reference valid Patient/Practitioner IDs. |
| **Conditions (Diagnosis)** | `Condition` | Tracks current and historical problems.<br>**Security**: Sensitive access (Physician/Nurse/Biller only). |
| **Medications** | `MedicationRequest` | **Prescribing**: Restricted to Physicians.<br>**Dispensing**: Visible to Pharmacists. |
| **Diagnostics** | `DiagnosticReport` | Lab results integration.<br>**Write**: Restricted to Lab Techs. |
| **Scheduling** | `Appointment` | **Self-Service**: Patients can book (create) own appointments.<br>**Management**: Schedulers manage slots. |

### 2.2 Data Persistence & Integrity
*   **Optimistic Locking**: The system implements standard FHIR Versioning.
    *   Every update requires a version check. If the client sends `If-Match: W/"2"` and the current server version is `3`, the request fails with `412 Precondition Failed`.
    *   **Goal**: prevents "lost updates" when multiple clinicians edit a record simultaneously.
*   **Soft Deletes vs. Hard Deletes**:
    *   **Current State**: Deletes are destructive (Hard Delete) from the main table but transaction logs/audit trails remain.
    *    **Recommendation**: Future support for `active: false` (Soft Delete) is planned.
*   **Versioning**:
    *   Every update creates a historical copy in the `_history` collection.
    *   Accessible via `GET /{Resource}/{id}/_history`.

### 2.3 Caching Strategy (Redis)
*   **Pattern**: Cache-Aside / Write-Through.
*   **TTL**: 10 Minutes default.
*   **Key Namespaces**: `patient:{id}`, `observation:{id}`.
*   **Invalidation**:
    *   **Update/Delete**: Immediately invalidates the cache key to ensure consistency.
    *   **Search**: Searches always hit the primary database (MongoDB) to ensure accuracy, they are *not* cached.

### 2.4 Event Driven Architecture
*   **Subscriptions**: The system supports FHIR `Subscription` resources (REST Hook).
*   **Trigger**: When a resource is Created/Updated, the `SubscriptionService` evaluates active subscriptions.
*   **Delivery**: Asynchronous POST request to the subscriber's endpoint.

---

## 3. Security & Access Control (RBAC)

The system uses a **Maximum Granularity** authority model. Users are not checked for "Roles" (e.g. `PHYSICIAN`) in the code, but for "Authorities" (e.g. `OBSERVATION_WRITE`). This allows purely configuration-based role changes without code refactoring.

### 3.1 Role-Authority Matrix (Active Definition)

#### Clinical Roles
| Role | Username | Authorities (Capabilities) |
| :--- | :--- | :--- |
| **Physician** | `physician` | `PATIENT_READ`, `OBSERVATION_WRITE`, `CONDITION_WRITE`, `ENCOUNTER_WRITE`, `MEDICATION_WRITE`, `ALLERGY_WRITE`, `IMMUNIZATION_WRITE`, `DIAGNOSTIC_READ` |
| **Nurse** | `nurse` | `PATIENT_READ`, `OBSERVATION_WRITE`, `CONDITION_READ`, `ENCOUNTER_READ`, `IMMUNIZATION_WRITE`, `MEDICATION_READ` |
| **Pharmacist** | `pharmacist` | `PATIENT_READ`, `MEDICATION_READ`, `ALLERGY_READ` |
| **Lab Tech** | `lab_tech` | `SERVICE_ORDER_READ`, `DIAGNOSTIC_WRITE`, `OBSERVATION_WRITE` |

#### Administrative Roles
| Role | Username | Authorities (Capabilities) |
| :--- | :--- | :--- |
| **Registrar** | `registrar` | `PATIENT_WRITE` (Create/Update), `APPOINTMENT_WRITE`, `COVERAGE_WRITE` |
| **Scheduler** | `scheduler` | `APPOINTMENT_WRITE`, `SCHEDULE_WRITE`, `PRACTITIONER_READ` |
| **Biller** | `biller` | `ENCOUNTER_READ`, `CONDITION_READ`, `ACCOUNT_WRITE` |
| **Practice Mgr** | `practice_mgr` | `PRACTITIONER_WRITE`, `ORGANIZATION_WRITE`, `LOCATION_WRITE` |

#### System Roles
| Role | Username | Authorities (Capabilities) |
| :--- | :--- | :--- |
| **SysAdmin** | `sys_admin` | `SUBSCRIPTION_WRITE`, `SYSTEM_CONFIG_WRITE` |
| **Auditor** | `auditor` | `AUDIT_READ` (ReadOnly access to Audit Logs) |
| **Integrator** | `integrator` | `OBSERVATION_WRITE` (Headless IoMT ingestion) |
| **Patient** | `patient_user` | `PATIENT_SELF_READ` (Access to own record only) |

### 3.2 Audit Logging
*   **Secure & Immutable**: Every Write (Create/Update/Delete) and specific Reads are logged.
*   **Storage**: `audit_event` collection in MongoDB.
*   **Fields Logged**: Timestamp, User ID, Resource Type, Resource ID, Operation, Outcome.
*   **Access**: strictly limited to `AUDITOR` role.

---

## 4. Operational Guide

### 4.1 Configuration
Configuration is managed via `application.yml` and environment variables.

**Critical Variables**:
*   `SPRING_DATA_MONGODB_URI`: Database connection.
*   `SPRING_REDIS_HOST`: Redis Host.
*   `SERVER_PORT`: Default 8080.
*   `SPRING_SECURITY_USERS_*{ROLE}*_PASSWORD`: Overrides for default passwords.

### 4.2 Monitoring & Observability
*   **Health Check**: `GET /actuator/health` (Used by Kubernetes Liveness probes).
*   **Metrics**: `GET /actuator/prometheus` provides standard JVM and Spring MVC metrics.
*   **Logs**: Application logs output to STDOUT (json formatted in production recommended).

### 4.3 Troubleshooting Common Issues

**Issue**: `403 Forbidden` on valid request.
*   **Cause**: The user role lacks the specific **Authority** for that HTTP Method + Resource.
*   **Fix**: Check `SecurityConfig.java` against the user's role. e.g., A `Nurse` cannot *Write* a `MedicationRequest`.

**Issue**: `412 Precondition Failed`.
*   **Cause**: Optimistic Locking conflict. Another user updated the record since you read it.
*   **Fix**: Re-read the resource to get the latest `versionId`, re-apply changes, and submit again.

**Issue**: New Data not showing in Search.
*   **Cause**: Search bypasses cache, so this is likely a DB write latency or index issue.
*   **Fix**: Check MongoDB replicaset status.

---

## 5. Developer References

### 5.1 Project Structure
*   `com.al.lifelog.model`: Data entities (MongoDocuments) and DTOs.
*   `com.al.lifelog.provider`: The REST Controllers (FHIR Resource Providers).
*   `com.al.lifelog.service`: Business Logic, Validations, and Event Publishing.
*   `com.al.lifelog.config`: Spring Beans, Security Chains.

### 5.2 Adding a New Resource
1.  **Model**: Create Class extending `DomainResource`. Application `Mongo{Resource}` wrapper.
2.  **Repo**: Create `CodeRepository` interface.
3.  **Service**: Implement CRUD logic, Caching, and Auditing calls.
4.  **Provider**: Extend `IResourceProvider`, expose `@Create`, `@Read`, etc.
5.  **Config**: Register Provider in `FhirRestfulServerConfig`.
6.  **Security**: Define authorities in `SecurityConfig`.
