# Project-Adaptive System Diagrams

This page provides three project-specific diagrams aligned with the current implementation (React + Vite frontend, Spring Boot backend, MySQL database), with concise notes beside each figure.

## 1) Project-Adaptive Architecture Diagram

| Figure | Notes |
|---|---|
| ```mermaid
flowchart LR
  subgraph Client[Client Layer]
    Analyst[Analyst Browser]
    Mobile[Mobile Browser]
  end

  subgraph FE[Presentation Layer - ui]
    Vite[Vite Dev Server :3001]
    Router[React Router + Page Modules]
    UI[Ant Design Components]
  end

  subgraph API[Application Layer - backend]
    Gateway[REST Controllers]
    TxSvc[Transaction Service]
    RuleSvc[Rule Evaluation Service]
    AlertSvc[Alert Service]
    DashSvc[Dashboard Service]
  end

  subgraph Data[Persistence Layer]
    Repo[JPA Repositories]
    MySQL[(MySQL 8\ntransaction_monitoring)]
  end

  subgraph Ops[Adaptive Ops Loop]
    Seed[seed-data.ps1]
    Rules[Rule tuning and updates]
    Observe[Coverage + health checks]
  end

  Analyst --> Vite
  Mobile --> Vite
  Vite --> Router --> UI
  Vite -->|/api proxy| Gateway

  Gateway --> TxSvc
  Gateway --> AlertSvc
  Gateway --> DashSvc
  TxSvc --> RuleSvc
  RuleSvc --> AlertSvc
  AlertSvc --> Repo
  DashSvc --> Repo
  TxSvc --> Repo
  Repo --> MySQL

  Seed --> Gateway
  Rules --> RuleSvc
  Observe --> DashSvc
``` | - UI and API are independently deployable, coupled only through HTTP contracts.<br>- Rule logic is isolated in Rule Evaluation Service for safe tuning and replay.<br>- Adaptive loop (seed, tuning, observation) supports rapid model/rule iteration without stack changes. |

## 2) Transaction Processing Sequence Diagram

| Figure | Notes |
|---|---|
| ```mermaid
sequenceDiagram
  autonumber
  actor Analyst
  participant UI as React UI
  participant API as TransactionController
  participant TS as TransactionService
  participant RE as RuleEvaluationService
  participant AR as AlertRepository
  participant TR as TransactionRepository

  Analyst->>UI: Submit transaction
  UI->>API: POST /api/transactions
  API->>TS: ingest(request)
  TS->>TR: save(transaction)
  alt status == COMPLETED
    TS->>RE: evaluate(transaction)
    RE->>AR: existsByDeduplicationKey(key)
    alt not exists
      RE->>AR: save(alert UUID + deduplicationKey)
      RE->>AR: save(alert history CREATED)
    else exists or unique-key conflict
      RE-->>TS: skip duplicate alert
    end
    TS->>TR: save(evaluatedAt)
  else status != COMPLETED
    TS-->>TS: skip evaluation
  end
  TS-->>API: TransactionResponse
  API-->>UI: 200 OK
  UI-->>Analyst: Render updated status/alerts
``` | - Option A deduplication is applied inside rule evaluation before insert and enforced again by DB unique key.<br>- Retries are safe: repeated same transaction/rule-window path does not create duplicate alerts.<br>- UUID alert IDs remove collision risk from counter-based generation. |

## 3) Alarm (Alert) State Diagram

| Figure | Notes |
|---|---|
| ```mermaid
stateDiagram-v2
  [*] --> OPEN: rule triggers
  OPEN --> ACKNOWLEDGED: acknowledge
  OPEN --> DISMISSED: dismiss
  ACKNOWLEDGED --> INVESTIGATING: investigate
  ACKNOWLEDGED --> DISMISSED: dismiss
  INVESTIGATING --> CLOSED: close
  INVESTIGATING --> DISMISSED: dismiss
  CLOSED --> [*]
  DISMISSED --> [*]
``` | - Terminal states are CLOSED and DISMISSED.<br>- All transitions are action-driven and recorded in alert history for auditability.<br>- Deduplication controls creation frequency; state machine controls investigation lifecycle. |

## Quick Reference

- Frontend entry: `ui`
- Backend entry: `backend`
- API base path (via frontend): `/api`
- Primary data store: MySQL `transaction_monitoring`
