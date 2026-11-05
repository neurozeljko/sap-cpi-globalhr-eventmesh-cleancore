# Enterprise Integration Architecture: Global HR & Payroll Event-Driven Mesh to SAP S/4HANA & SAP Concur

[![SAP Integration Suite](https://img.shields.io/badge/SAP%20Integration%20Suite-Cloud%20Integration-0A85EA?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![Clean Core](https://img.shields.io/badge/Clean%20Core-S%2F4HANA%20OData%20V4-green?style=for-the-badge&logo=sap&logoColor=white)](https://www.sap.com)
[![SAP Concur](https://img.shields.io/badge/SAP%20Concur-SCIM%20Identity%20v4-0070F2?style=for-the-badge&logo=sap&logoColor=white)](https://developer.concur.com)
[![Workday](https://img.shields.io/badge/Workday-PECI%20Change%20Index-F35325?style=for-the-badge&logo=workday&logoColor=white)](https://www.workday.com)
[![Error Governance](https://img.shields.io/badge/Governance-SAP%20AIF%20Fiori-8A2BE2?style=for-the-badge&logo=sap&logoColor=white)](https://help.sap.com)
[![Protocol](https://img.shields.io/badge/Protocol-HTTPS%20Webhook%20%7C%20AMQP-orange?style=for-the-badge)](https://oasis-open.org)
[![Security](https://img.shields.io/badge/Security-GDPR%20%7C%20Zero--Trust%20PII-red?style=for-the-badge)](https://gdpr.eu)

---

## Executive Summary

This enterprise integration project demonstrates the architecture, implementation, and regulatory compliance validation for real-time human capital master data replication within a global enterprise landscape. It connects **Workday HCM (Payroll Effective Change Index - PECI)** to **SAP S/4HANA Cloud** and **SAP Concur Travel & Expense**.

The architecture decouples core personnel events from ERP ledger posting schedules. By ingesting real-time change manifests via secure HTTPS webhooks (AMQP Event Mesh ready), the pipeline executes an in-memory **XSLT 3.0** canonicalization engine to strip operational XML overhead, routes data concurrently across a multi-threaded **Parallel Multicast**, transforms payloads via memory-safe **Groovy Streaming Readers** into native **S/4HANA OData V4 (`API_BUSINESS_PARTNER`)** and **SAP Concur SCIM 2.0 (RFC 7644)** endpoints, and delegates business error remediation directly to the **SAP Application Interface Framework (AIF)**.

---

## Architecture Overview

### High-Level Topology

```
[Workday HCM PROD (PECI Ingestion Source)]
       │
       │ HTTPS POST /hr/workday/peci (JSON/XML Webhook)
       │ Client Mutual TLS / OAuth 2.0 Bearer Token (ESBMessaging.send)
       ▼
[SAP Integration Suite (Cloud Integration Runtime)]
 │
 ├── Security & Audit Context Layer:
 │    └─ Content Modifier (CM_PECI_Audit_Headers)
 │         ├─ CorrelationId: SAP CPI Message GUID (${header.SAP_MessageProcessingLogID})
 │         ├─ WorkerEventOrigin: Workday Inbound Tenant ID
 │         └─ OriginalPayloadBackup: Retained in Exchange Property for DLQ Governance
 │
 ├── Transformation & Canonicalization Pipeline:
 │    └─ XSLT 3.0 Engine (ParsePECI.xsl)
 │         ├─ In-memory filtering of inactive / non-hire operational entries
 │         ├─ Generates normalized canonical schema (<CanonicalHRData>)
 │         └─ Normalizes company codes and ISO-4217 currency structures
 │
 ├── Asynchronous Fan-Out & Parallel Processing:
 │    └─ Parallel Multicast (Parallel_Replication_FanOut)
 │         │
 │         ├── Branch 1: SAP S/4HANA Clean Core Enterprise Replication
 │         │    ├─ Groovy Engine (formatS4HANA.groovy - java.io.Reader streaming)
 │         │    ├─ Schema Mapping: Canonical to S/4HANA Business Partner OData V4
 │         │    └─ Target HTTP: POST /sap/opu/odata4/sap/api_businesspartner/srvd_a2x/sap/businesspartner/0001/
 │         │
 │         └── Branch 2: SAP Concur Travel & Expense Provisioning
 │              ├─ Groovy Engine (formatConcur.groovy - Zero-Heap SCIM 2.0 Builder)
 │              ├─ Schema Mapping: Canonical to RFC 7644 SCIM JSON Identity Schema
 │              └─ Target HTTP: POST /profile/identity/v4/Users
 │
 └── Exception Subprocess & Governance Layer (AIF Dead-Letter Queue):
      ├─ Error Interceptor: Catches HTTP 4xx/5xx, timeouts, or locked Cost Centers
      ├─ Incident Formatter: formatAIFHRError.groovy
      └─ Outbound Routing: S/4HANA AIF HTTP Gateway (/HR_PAYROLL/ Namespace)
```

---

## Technical Specifications

| Component | Technical Implementation | Architectural Justification |
| :--- | :--- | :--- |
| **Inbound Interface** | HTTPS Webhook Adapter (`/hr/workday/peci`) | Low-latency asynchronous push ingestion eliminating nocturnal batch polling overhead and server load spikes. |
| **Audit Context** | Content Modifier (`CM_PECI_Audit_Headers`) | Injects immutable correlation IDs, timestamps, and preserves the unparsed raw payload for regulatory traceability. |
| **Pipeline Transform 1** | XSLT 3.0 Engine (`ParsePECI.xsl`) | Native in-memory canonicalization providing deterministic data filtering and schema decoupling between HRIS and ERP. |
| **Parallel Fan-Out** | Parallel Multicast Pattern | Thread-isolated fan-out ensuring that downstream S/4HANA latency never blocks SAP Concur identity provisioning. |
| **Clean Core Gatekeeper** | Groovy Engine (`formatS4HANA.groovy`) | Constructs atomic OData V4 JSON entities directly compliant with released S/4HANA Cloud Public APIs. |
| **Outbound ERP** | HTTP Client (`api_businesspartner` OData V4) | Adheres strictly to SAP Clean Core principles using released public standard APIs, completely bypassing custom ABAP RFC/BAPI code. |
| **Outbound Expense** | HTTP Client (`Concur Identity v4 /Users`) | Native RFC 7644 SCIM 2.0 enterprise specification matching travel authority profiles with HR cost assignment records. |
| **Memory Optimization** | Groovy `java.io.Reader` Streaming | Prevents JVM heap exhaustion by parsing payloads as character streams rather than heavy in-memory DOM nodes. |
| **Fault Governance** | Exception Subprocess (`formatAIFHRError.groovy`) | Direct dispatch into S/4HANA AIF Fiori cockpit, enabling business HR administrators to remediate and reprocess messages without IT tickets. |

---

## Enterprise HR Governance & Regulatory Framework (GDPR / Clean Core / AIF)

Operating global enterprise workforce automation requires adherence to strict architectural, data protection, and governance principles:

1. **Zero-Trust PII & In-Memory Encryption (GDPR Compliance):**
   * Employee personal identifiable information (PII) and compensation figures are parsed strictly in volatile JVM memory buffers.
   * Sensitive values are never persisted in unencrypted plain-text execution trace logs (MPL).
   * Temporary payload snapshots stored in exchange properties are scoped strictly to the execution exchange lifecycle and destroyed upon route completion.

2. **Clean Core Compliance (Zero S/4HANA Modifications):**
   * Implements direct consumption of SAP standard **OData V4 (`API_BUSINESS_PARTNER`)** services.
   * Eliminates brittle point-to-point batch files (ABAP custom reports, custom Z-tables, flat file IDocs) in favor of modern API-led connectivity.
   * Ensures seamless S/4HANA quarterly and annual cloud upgrades without regressions in the integration layer.

3. **Audit Logging & ALCOA+ Traceability:**
   * Custom audit properties attached to the transaction context:
     * `PECI_CorrelationId`: SAP CPI Message GUID.
     * `PECI_WorkdayEventId`: Upstream Workday transaction UUID.
     * `PECI_ExecutionTimestamp`: UTC ISO-8601 transmission stamp.
     * `PECI_ProcessingStatus`: Fan-out dispatch verification flag.

4. **Dead-Letter Resiliency & Business-Led Error Resolution (SAP AIF):**
   * If a Cost Center is temporarily locked in S/4HANA Finance, the pipeline does not stall integration queues.
   * The **Exception Subprocess** catches the runtime exception, builds an enriched incident envelope containing error stacks and the original payload, and posts it to S/4HANA AIF under the `/HR_PAYROLL/` namespace.
   * Functional HR controllers resolve master data discrepancies in SAP Fiori and re-trigger processing directly from the AIF monitor without IT intervention.

---

## Message Mapping Details (Workday PECI to S/4HANA & Concur)

| Workday PECI XML XPath | Target S/4HANA OData V4 Field | Target Concur SCIM Field | Business Semantic |
| :--- | :--- | :--- | :--- |
| `wd:Worker/wd:Employee_ID` | `BusinessPartner` | `urn:...:enterprise:2.0:User:employeeNumber` | Global unique worker identifier |
| `wd:Personal_Data/wd:First_Name` | `FirstName` | `name.givenName` | Worker legal given name |
| `wd:Personal_Data/wd:Last_Name` | `LastName` | `name.familyName` | Worker legal family name |
| `wd:Personal_Data/wd:Primary_Work_Email` | `SearchTerm1` | `userName`, `emails[type='work'].value` | SSO enterprise identity and user login |
| `wd:Position_Data/wd:Cost_Center_Reference_ID` | `to_Employment/CostCenter` | `urn:...:enterprise:2.0:User:costCenter` | Controlling cost center allocation |
| `wd:Position_Data/wd:Company_Reference_ID` | `to_Employment/CompanyCode` | `N/A` | Legal entity and general ledger boundary |
| `wd:Compensation_Data/wd:Base_Pay_Amount` | `to_Employment/BaseSalary` | `N/A` | Base compensation for payroll accounting |
| `wd:Compensation_Data/wd:Currency` | `to_Employment/Currency` | `N/A` | ISO currency code for financial valuation |
| `wd:Employee_Status` (`Active` / `Hire`) | `to_Employment/EmploymentStatus = '3'` | `active = true` | Workforce validity and access enablement |

---

## Repository Structure

```
├── README.md                                  # Comprehensive architectural & governance documentation
├── ADR-001-Workday-EventMesh-Architecture.md  # Architecture Decision Record (Event-Driven vs Batch)
├── Workday_PECI_to_ERP_Router.jar            # Production OSGi runtime bundle export
├── ParsePECI.xsl                              # High-performance XSLT 3.0 canonical mapping engine
├── formatS4HANA.groovy                        # Streaming Groovy builder for S/4HANA OData V4 Business Partner
├── formatConcur.groovy                        # Streaming Groovy builder for SAP Concur SCIM 2.0 User
├── formatAIFHRError.groovy                    # Exception handler generating SAP AIF incident payloads
├── Workday_PECI_Sample_Input.xml              # Valid enterprise Workday PECI XML test event
├── Workday_PECI_Schema.xsd                    # Workday PECI input validation schema
├── Concur_SCIM_User_Target.json               # RFC 7644 SCIM 2.0 JSON validation schema
└── .gitignore                                 # Standard Git exclusions for enterprise projects
```

---

## Deployment & Verification Runbook

1. **Deploy Artifact:** Deploy `Workday_PECI_to_ERP_Router.jar` onto the SAP Integration Suite tenant runtime via **Manage Integration Content**.
2. **Endpoint Exposure:** Confirm the runtime status resolves to **Started** with exposed endpoint:
   `https://<tenant-runtime>.cfapps.<region>.hana.ondemand.com/http/hr/workday/peci`
3. **Execution Verification (cURL):**
   ```bash
   curl -X POST \
     'https://<tenant-runtime>.cfapps.<region>[.hana.ondemand.com/http/hr/workday/peci](https://.hana.ondemand.com/http/hr/workday/peci)' \
     -H 'Content-Type: application/xml' \
     -H 'Authorization: Bearer <BTP_OAUTH_TOKEN>' \
     --data-binary '@Workday_PECI_Sample_Input.xml'
   ```
4. **Monitoring & Trace Inspection:**
   * Navigate to **Monitor Message Processing** in SAP Integration Suite.
   * Verify non-blocking parallel branch completion.
   * Validate outbound payloads against S/4HANA OData V4 and Concur SCIM 2.0 schema targets.
   * In case of simulated backend error, inspect incident in S/4HANA Fiori AIF interface `/HR_PAYROLL/`.

---

## Author

**Željko Popov**  
*Integration Architect | SAP Integration Suite & Clean Core Specialist*  
[GitHub Repositories](https://github.com/neurozeljko)
