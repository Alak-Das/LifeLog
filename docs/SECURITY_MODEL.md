# LifeLog Security & Access Control Model

This document outlines the **active** security architecture for the LifeLog EHR, implementing a **Maximum Granularity RBAC** model.

## 1. Active Granular RBAC Model
The system enforces strict compliance and least-privilege using a **12-role** model.
These roles access Patient Health Information (PHI) for care delivery.

| Role | Description | Access Rights |
| :--- | :--- | :--- |
| **`PHYSICIAN`** | Fully licensed doctor. | **Read/Write**: All Clinical Resources (`Observation`, `Condition`, `CarePlan`, `MedicationRequest`).<br>**Read-Only**: `Patient` (Demographics).<br>**Write**: `Encounter` (Visit notes). |
| **`NURSE`** | RN / LPN supporting care. | **Read**: All Clinical Resources.<br>**Write**: `Observation` (Vitals), `Immunization` (Administering), `Specimen`.<br>**Deny**: `MedicationRequest` (Cannot prescribe). |
| **`PHARMACIST`** | Medication expert. | **Read**: `Patient`, `Condition`, `AllergyIntolerance`, `MedicationRequest`.<br>**Write**: `MedicationDispense`, `MedicationAdministration`. |
| **`LAB_TECH`** | Laboratory specialist. | **Read**: `ServiceRequest` (Orders).<br>**Write**: `DiagnosticReport`, `Observation` (Lab Results).<br>**Deny**: Access to general `Condition` or `Encounter` notes. |

### 2.2 Administrative Roles
 These roles manage the business and operational side of the clinic.

| Role | Description | Access Rights |
| :--- | :--- | :--- |
| **`REGISTRAR`** | Front desk/Reception. | **Read/Write**: `Patient` (Demographics), `Appointment`, `Coverage` (Insurance).<br>**Deny**: All Clinical Resources (Cannot view medical history). |
| **`SCHEDULER`** | Appointment coordination. | **Read/Write**: `Appointment`, `Schedule`, `Slot`.<br>**Read-Only**: `Practitioner` (Availability). |
| **`BILLER`** | Medical coding & billing. | **Read**: `Encounter`, `Condition`, `Procedure` (for coding).<br>**Write**: `Claim`, `Account`, `Invoice`. |
| **`PRACTICE_MGR`** | Org-level management. | **Read/Write**: `Practitioner`, `Organization`, `Location`, `HealthcareService`.<br>**Read-Only**: `AuditLog`. |

### 2.3 System & Compliance Roles
These roles ensure the system runs securely and audits are performed.

| Role | Description | Access Rights |
| :--- | :--- | :--- |
| **`SYS_ADMIN`** | Technical administrator. | **read/Write**: `Subscription`, `Endpoint`, `Parameters`.<br>**Deny**: Direct access to PHI (except via system maintenance). |
| **`AUDITOR`** | Compliance officer. | **Read-Only**: `AuditEvent`, `Provenance`.<br>**Deny**: All operational writes. |
| **`INTEGRATOR`** | System-to-System Bot. | **Write-Only**: `Observation` (from IoMT devices).<br>**Read-Only**: `CarePlan` (for specific apps). |

### 2.4 Patient Roles
| Role | Description | Access Rights |
| :--- | :--- | :--- |
| **`PATIENT`** | The subject of care. | **Read-Only**: Own `Patient` record, `Observation`, `Condition`, etc.<br>**Write**: `QuestionnaireResponse` (Pre-visit forms), `Appointment` (Self-booking). |

---

## 2. Implementation Overview
The system uses authority-based checks (`hasAuthority`) rather than generic role checks. This allows for extremely granular control over FHIR resource operations.

### Example Authority Mapping:
| Authority | Resource Impact |
| :--- | :--- |
| `PATIENT_WRITE` | Create/Update/Delete `/fhir/Patient` |
| `OBSERVATION_WRITE` | Create/Update `/fhir/Observation` |
| `DIAGNOSTIC_READ` | Read `/fhir/DiagnosticReport` |

Full authority lists are managed in `SecurityConfig.java`.

---

## 3. Active Enforcement
Access control is enforced at the entry point via Spring Security's `SecurityFilterChain`. Additionally, a `SmartOnFhirInterceptor` provides FHIR-native rule enforcement for deep resource-level access (e.g. metadata discovery).

### Request Lifecycle with RBAC:
1. **Authentication**: Basic credentials checked against `InMemoryUserDetailsManager`.
2. **Authority Assignment**: User is granted specific authorities based on their role.
3. **Filter Check**: `authorizeHttpRequests` validates the HTTP method and path against required authorities.
4. **FHIR Rule Check**: `AuthorizationInterceptor` (HAPI FHIR) validates the action against the internal rule builder.

