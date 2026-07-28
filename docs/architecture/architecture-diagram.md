# Architecture Diagram

This diagram reflects the current implemented stack in this repository: React + Vite frontend, Spring Boot backend, and MySQL database.

```mermaid
flowchart LR
  subgraph UserSide[User Browser]
    U[Analyst / Investigator]
  end

  subgraph Frontend[Frontend: ui]
    Vite[Vite Dev Server :3001]
    React[React + TypeScript + Ant Design]
    Pages[Dashboard / Alerts / Transactions / Rules]
  end

  subgraph Backend[Backend: backend]
    API[Spring Boot REST API :8080]
    Services[Alert Service + Rule Engine + Dashboard Service]
    JPA[JPA Repositories]
  end

  subgraph Data[Data Layer]
    MySQL[(MySQL 8\ntransaction_monitoring)]
  end

  subgraph Seed[Mock Data & Ops]
    SeedScript[seed-data.ps1]
    Health[Actuator Health Endpoint]
  end

  U -->|HTTP| Vite
  Vite --> React --> Pages
  Vite -->|/api proxy| API

  API --> Services --> JPA --> MySQL
  SeedScript -->|POST /api/...| API
  API --> Health

  classDef front fill:#e8f3ff,stroke:#2e6f9d,color:#10324a
  classDef back fill:#eef9f1,stroke:#2f855a,color:#173f2d
  classDef data fill:#fff4e6,stroke:#b7791f,color:#5a3a12
  classDef ops fill:#f3efff,stroke:#6b46c1,color:#2d1b69

  class Vite,React,Pages front
  class API,Services,JPA,Health back
  class MySQL data
  class SeedScript ops
```

## Runtime Ports

- Frontend: 3001
- Backend: 8080
- Database: 3306 (MySQL default)

## Notes

- Frontend calls backend through Vite proxy on path /api.
- Alert lifecycle actions and rule evaluation are processed in backend services.
- MySQL is the system of record for transactions, alerts, rules, and status history.
