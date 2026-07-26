# From Dev to Architect Roadmap

This roadmap is the public, reviewer-friendly summary of the TeamPulse learning
and delivery plan. It covers a 28-month core program followed by an optional
specialization year.

The roadmap is intentionally more than a list of technologies. Each weekly
increment starts from a functional or operational problem and connects:

- one or more documented requirements;
- the relevant Architecture Decision Records (ADRs);
- implementation and automated validation;
- an architect-facing artifact such as a diagram, runbook, checklist,
  dashboard, or postmortem;
- the corresponding Slidev learning material.

The detailed planning workbook remains available in
[`docs/teampulse-roadmap-v9-code-infra-detaille.xlsx`](docs/teampulse-roadmap-v9-code-infra-detaille.xlsx).
The workbook describes intent; merged pull requests, requirements, ADRs, tests,
and release tags are the evidence of delivered work.

## Program at a glance

| Measure              | Target                                      |
| -------------------- | ------------------------------------------- |
| Core duration        | 122 weeks, approximately 28 months          |
| Weekly rhythm        | Approximately 7 hours                       |
| Guided learning      | Approximately 300 hours                     |
| TeamPulse delivery   | Approximately 500 hours                     |
| Total planned effort | Approximately 800 hours                     |
| Named roadmap ADRs   | At least 21, plus ticket-specific decisions |
| Operational runbooks | At least 15                                 |

## Current status

**Phase 1 is in progress.** The current public increment is
[`W001 - TeamPulse backend foundation`](docs/besoins/W001/README.md).

Delivered through `main`:

- `W001-T01`: modular Spring Boot backend foundation;
- `W001-T02`: local PostgreSQL with module-owned Flyway schemas;
- `W001-T03`: executable architecture rules with ArchUnit and Spring Modulith.

The next W001 sequence is:

1. `W001-T04`: organization-scoped multi-tenancy with `org_id`;
2. `W001-T05`: minimal users API and persistence vertical;
3. `W001-T06`: production-oriented multi-stage container image;
4. `W001-T07`: explicit local and LocalStack configuration profiles.

An increment is considered delivered only when its requirement, ADR,
implementation, tests, documentation, and learning material agree.

## Core program

| Phase                          | Weeks     | Months  | TeamPulse target                                                            | Main capabilities                                                                                                                                     |
| ------------------------------ | --------- | ------- | --------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. Foundations**             | W001-W026 | M1-M6   | A containerized modular monolith with its first reproducible AWS deployment | Java, Spring Boot, Angular, PostgreSQL, Docker, CI/CD, Terraform, AWS foundations, authentication, quality gates                                      |
| **2. Modern platform**         | W027-W065 | M7-M15  | A service-based system running on Kubernetes/EKS                            | Kubernetes, Helm, EKS, Kafka, API gateway, observability, OpenTelemetry, microservice boundaries, saga and outbox patterns                            |
| **3. Enterprise architecture** | W066-W122 | M16-M28 | An enterprise-ready, multi-environment platform                             | GitOps, progressive delivery, advanced Terraform, multi-account AWS, security hardening, disaster recovery, reliability, FinOps, architecture reviews |

### Selected milestones

| Weeks     | Expected evidence                                                                                                                       |
| --------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| W001      | Modular boundaries, local persistence, executable architecture rules, multi-tenancy foundation, first API vertical, container packaging |
| W021-W026 | Failure scenarios, a simulated postmortem, Well-Architected review, and the Phase 1 portfolio                                           |
| W027-W038 | Kubernetes and EKS foundations with deployment and operational evidence                                                                 |
| W039-W046 | Metrics, logs, traces, alerting, incident management, and the first SRE portfolio                                                       |
| W047-W065 | Service decomposition, Kafka, reliable event delivery, saga/outbox trade-offs, and the Phase 2 portfolio                                |
| W066-W085 | Reusable infrastructure, GitOps, multi-account design, security hardening, chaos drills, and a complete architecture review             |
| W086-W100 | Data and operational maturity, security and cost drills, postmortems, system-design exercises, and Portfolio v3                         |
| W101-W122 | Migration and enterprise design scenarios, Well-Architected review, final validation, and the public portfolio                          |

## Near-term maintainer priorities

Over the next six months, the project aims to:

- deliver approximately 18-24 additional weekly increments without weakening
  the requirement-to-ADR-to-code traceability;
- complete the W001 backend foundation and progress through the first
  production-shaped TeamPulse capabilities;
- publish the Slidev material as a browsable public demonstration;
- expand English onboarding and contributor-ready issues;
- automate consistency checks across requirements, ADRs, code, tests, and
  learning material;
- publish reusable architecture-review, security, reliability, and operational
  checklists as they are validated by TeamPulse.

These are directional goals rather than a promise to merge work that has not
met the project's quality and review requirements.

## Optional specialization year

After W122, the detailed workbook defines an optional W123-W174 extension.
One track should receive approximately 70% of the effort and a secondary track
approximately 30%.

| Track                                 | Focus                                                                                               | Expected outcomes                                                                                                       |
| ------------------------------------- | --------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| **A. Platform / DevOps Architecture** | Delivery experience, infrastructure as code, GitOps, observability, cost, and operational standards | Golden paths, mature GitOps, reusable infrastructure modules, SLOs, runbooks, and a platform handbook                   |
| **B. Cloud Security Architecture**    | IAM, secrets, detection and response, supply-chain security, posture, and auditability              | Threat models, security standards, incident playbooks, evidence packs, policy checks, and security KPIs                 |
| **C. Network / Hybrid Architecture**  | VPC design, routing, hybrid connectivity, global edge, latency, and multi-region recovery           | Network standards, troubleshooting runbooks, tested infrastructure modules, hybrid designs, and recovery game days      |
| **D. Data / Streaming Architecture**  | Event contracts, Kafka, data products, governance, quality, lineage, and DataOps                    | Event standards, mature outbox and DLQ handling, streaming SLOs, data quality controls, lineage, and a DataOps handbook |

## How progress is tracked

- Requirements: [`docs/besoins`](docs/besoins)
- Architecture decisions: [`docs/adr`](docs/adr)
- Detailed roadmap workbook:
  [`docs/teampulse-roadmap-v9-code-infra-detaille.xlsx`](docs/teampulse-roadmap-v9-code-infra-detaille.xlsx)
- Learning material: [`slidev`](slidev)
- Delivery evidence: merged pull requests, automated checks, and `W00X-TYY`
  release tags
- Community work:
  [open issues](https://github.com/amrine/from-dev-to-architect/issues) and
  [`CONTRIBUTING.md`](CONTRIBUTING.md)

Roadmap changes may adjust sequencing as TeamPulse exposes new constraints, but
they must not silently rewrite completed decisions. Significant changes are
documented through an issue and, when architectural, an ADR.
