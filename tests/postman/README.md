# LifeLog Integration Tests

This directory contains the Postman collection for verifying the LifeLog EHR Backend.

## Prerequisites
- Postman Desktop or Web
- LifeLog EHR Backend running locally (usually on `http://localhost:8080`)

## How to Run

1. **Import the Collection & Environment**:
   - Open Postman.
   - Click **Import**.
   - Select both `LifeLog_Integration_Tests.postman_collection.json` and `LifeLog_Local.postman_environment.json`.

2. **Select the Environment**:
   - In the top-right corner of Postman, select **LifeLog Localhost** from the environment dropdown.

3. **Run the Scenarios**:
   - **0. Discovery**: Verify the SMART configuration and CapabilityStatement are accessible.
   - **1. Patient Scenarios**: Run these in order. The "Create Patient" test will automatically capture the `patient_id`.
   - **2. Observation Scenarios**: Creates a Vital Signs observation linked to the patient.
   - **3. Advanced Scenarios**: Tests `_revinclude` and validation failures.
   - **4. Observability**: Verify health checks and Prometheus metrics.

## 🤖 Automated Running (Newman)

If you have [Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman/) installed, you can run the suite directly from your terminal:

```bash
newman run LifeLog_Integration_Tests.postman_collection.json -e LifeLog_Local.postman_environment.json --reporters cli
```

> [!TIP]
> Ensure the LifeLog backend is running (`docker-compose up`) before executing the tests.

## Key Scenarios Covered
- **Clinical CRUD**: Full lifecycle of Patient and Observation resources.
- **Search Logic**: Name-based search, LOINC code search, and FHIR date prefixes (`gt`).
- **Data Integrity**: FHIR Profile validation (Success and 422 Error scenarios).
- **Interoperability**: Reference solving with `_include` and `_revinclude`.
- **System Health**: Micrometer metrics and Spring Boot Actuator health endpoints.
