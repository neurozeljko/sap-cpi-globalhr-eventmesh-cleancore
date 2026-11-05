# Enterprise SAP Integration Suite: Global HR & Payroll Event-Driven Mesh

[![SAP Integration Suite](https://img.shields.io/badge/SAP%20Integration%20Suite-Cloud%20Integration-0A85EA?logo=sap&logoColor=white)](https://www.sap.com)
[![Clean Core](https://img.shields.io/badge/Clean%20Core-S%2F4HANA%20OData%20V4-green)](https://www.sap.com)
[![Concur API](https://img.shields.io/badge/SAP%20Concur-SCIM%20Identity%20v4-blue)](https://developer.concur.com)
[![Error Governance](https://img.shields.io/badge/Governance-SAP%20AIF%20Fiori-purple)](https://help.sap.com)

Production-grade, decoupled event-driven integration blueprint built on **SAP Integration Suite (Cloud Integration)**. Consumes real-time **Workday PECI** events, canonicalizes via **XSLT 3.0**, and broadcasts in parallel to **SAP S/4HANA OData V4** and **SAP Concur Identity v4 (SCIM)**, governed by **SAP AIF**.

---

## Architecture Highlights
* **Decoupled Ingestion:** Real-time Workday PECI webhook ingestion via HTTPS POST `/hr/workday/peci`
* **XSLT 3.0 Canonicalization:** Normalizes raw PECI XML into canonical HR Master Data structures
* **Parallel Multicast:** Simultaneous branch dispatch to S/4HANA OData V4 and SAP Concur SCIM
* **Streaming Groovy:** Memory-safe `java.io.Reader` streaming transformation
* **AIF Error Governance:** Centralized error routing and monitoring in S/4HANA AIF (`/HR_PAYROLL/`)

---

## Repository Contents
* `Workday_PECI_to_ERP_Router.jar`: Production runtime bundle export for SAP Integration Suite.
* `ADR-001-Workday-EventMesh-Architecture.md`: Architecture Decision Record.
* `ParsePECI.xsl`: XSLT 3.0 canonicalization engine.
* `formatS4HANA.groovy`: Streaming OData V4 payload builder.
* `formatConcur.groovy`: SCIM 2.0 Concur user payload builder.
* `formatAIFHRError.groovy`: SAP AIF enterprise incident formatter.
* `Workday_PECI_Sample_Input.xml`: Sample enterprise input payload.
* `Workday_PECI_Schema.xsd`: Input schema definition.
* `Concur_SCIM_User_Target.json`: Target SCIM 2.0 JSON schema.
* `.gitignore`: Git exclusions.

---

## Author
**Željko Popov**  
*Principal Integration Architect | SAP Integration Suite & Clean Core Specialist*  
[GitHub Repositories](https://github.com/neurozeljko)
