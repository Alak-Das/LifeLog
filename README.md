# LifeLog EHR Backend

A Clinical Data Backend built with **Spring Boot 4**, **HAPI FHIR**, **MongoDB**, and **Redis**.

This project implements a **HAPI FHIR Facade** architecture, enabling standard FHIR resources to be stored as JSON documents in MongoDB, with high-performance caching via Redis.

## 🚀 Tech Stack

- **Java**: 21
- **Framework**: Spring Boot 4.0.0
- **FHIR Standard**: HAPI FHIR 8.6.1 (R4 Model)
- **Database**: MongoDB (Stores raw FHIR JSON)
- **Cache**: Redis (Key-Value cache for Resources)
- **Containerization**: Docker & Docker Compose

## 🛠️ Architecture

Unlike the standard HAPI FHIR JPA Server (which requires SQL), **LifeLog** uses a "Plain Server" approach where custom Resource Providers delegate to a MongoDB repository.

1.  **Request**: `GET /fhir/Patient/123`
2.  **Provider**: `PatientResourceProvider` intercepts the request.
3.  **Service**: `PatientService` checks **Redis**.
    -   *Hit*: Returns cached JSON.
    -   *Miss*: Fetches from **MongoDB**, caches result in Redis, and returns.
4.  **Response**: Standard FHIR JSON.

## 📦 How to Run

### Option 1: Docker (Recommended)
Orchestrates the App, MongoDB, and Redis automatically.

```bash
docker-compose up --build
```
- API: `http://localhost:8080/fhir`
- Mongo: `localhost:27017`
- Redis: `localhost:6379`

### Option 2: Local Maven
Requires locally running MongoDB and Redis.

```bash
mvn spring-boot:run
```

## 🔗 API Endpoints

Base URL: `http://localhost:8080/fhir`

### Patient Resource

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/Patient` | Create a new Patient |
| **GET** | `/Patient/{id}` | Retrieve a Patient by ID |
| **GET** | `/Patient?name={name}` | Search by Family or Given name (Partial match) |
| **GET** | `/Patient?gender={code}` | Search by Administrative Gender (e.g., `male`) |

### Clinical Resources

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/Observation` | Record Vitals/Labs |
| **GET** | `/Observation?subject={id}` | Get Vitals for Patient |
| **POST** | `/Condition` | Record Diagnoses (e.g. Diabetes) |
| **GET** | `/Condition?subject={id}` | Get Diagnoses for Patient |
| **POST** | `/Encounter` | Record Visits |
| **GET** | `/Encounter?subject={id}` | Get Encounters for Patient |
| **POST** | `/MedicationRequest` | Prescribe Medications |
| **GET** | `/MedicationRequest?subject={id}` | Get Prescriptions for Patient |
| **POST** | `/AllergyIntolerance` | Record Allergies |
| **GET** | `/AllergyIntolerance?patient={id}` | Get Allergies for Patient |
| **POST** | `/Appointment` | Schedule Appointments |
| **GET** | `/Appointment?actor={id}` | Get Appointments for Patient |

### Example Payloads

**Create Patient**
```json
POST /fhir/Patient
{
  "resourceType": "Patient",
  "name": [
    {
      "family": "Doe",
      "given": ["John"]
    }
  ],
  "gender": "male",
  "birthDate": "1990-01-01"
}
```

**Record Condition (Diagnosis)**
```json
POST /fhir/Condition
{
  "resourceType": "Condition",
  "clinicalStatus": {
    "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active" }]
  },
  "code": {
    "coding": [{ "system": "http://snomed.info/sct", "code": "73211009", "display": "Diabetes mellitus" }]
  },
  "subject": { "reference": "Patient/123" }
}
```

**Record Encounter (Visit)**
```json
POST /fhir/Encounter
{
  "resourceType": "Encounter",
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB",
    "display": "ambulatory"
  },
  "subject": { "reference": "Patient/123" }
}
```

**Record Medication Request (Prescription)**
```json
POST /fhir/MedicationRequest
{
  "resourceType": "MedicationRequest",
  "status": "active",
  "intent": "order",
  "medicationCodeableConcept": {
    "coding": [{ "system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "1049630", "display": "Amoxicillin 500 MG" }]
  },
  "subject": { "reference": "Patient/123" },
  "requester": { "reference": "Practitioner/555" }
}
```

**Record Allergy Intolerance**
```json
POST /fhir/AllergyIntolerance
{
  "resourceType": "AllergyIntolerance",
  "clinicalStatus": {
    "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "code": "active" }]
  },
  "verificationStatus": {
    "coding": [{ "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-verification", "code": "confirmed" }]
  },
  "code": {
    "coding": [{ "system": "http://snomed.info/sct", "code": "373270004", "display": "Penicillin" }]
  },
  "patient": { "reference": "Patient/123" }
}
```

**Schedule Appointment**
```json
POST /fhir/Appointment
{
  "resourceType": "Appointment",
  "status": "booked",
  "description": "Follow-up checkup",
  "start": "2024-03-20T09:00:00Z",
  "end": "2024-03-20T10:00:00Z",
  "participant": [
    {
      "actor": { "reference": "Patient/123" },
      "status": "accepted"
    },
    {
      "actor": { "reference": "Practitioner/555" },
      "status": "accepted"
    }
  ]
}
```

## 🧪 Testing

Run unit tests (Services + Controllers) with Maven:

```bash
mvn test
```
