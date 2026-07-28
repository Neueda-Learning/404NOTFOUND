# FRAML Transaction Monitoring System

A full-stack transaction monitoring and alerting system built with **React + TypeScript** (frontend), **Spring Boot** (backend), and **MySQL** (database), implementing a **FRAML** (Fraud/AML) foundation:

- Rules engine + behavioral checks
- Risk-band aware thresholds
- Alert deduplication and grouping
- Full alert lifecycle and audit trail
- Rule governance with versioning and change log

## Tech Stack

| Layer     | Technology                                |
|-----------|--------------------------------------------|
| Frontend  | React 18 + TypeScript + Vite + Ant Design |
| Backend   | Java 17 + Spring Boot 3.2 + Spring JPA    |
| Database  | MySQL 8.x                                 |
| API Docs  | SpringDoc OpenAPI / Swagger UI            |

## Project Structure

```
404NOTFOUND/
├── backend/          # Spring Boot REST API
│   ├── src/main/java/com/framl/monitoring/
│   │   ├── config/       # CORS, data initializer
│   │   ├── controller/   # REST controllers
│   │   ├── dto/          # Request/Response DTOs
│   │   ├── entity/       # JPA entities
│   │   ├── enums/        # Status/type enums
│   │   ├── repository/   # Spring Data repositories
│   │   └── service/      # Business logic & rule engine
│   └── src/main/resources/
│       └── application.yml
└── ui/               # React frontend
    ├── src/
    │   ├── api/       # Axios API clients
    │   ├── pages/     # Dashboard, Alerts, Transactions, Rules
    │   ├── types/     # TypeScript type definitions
    │   └── utils/     # Formatting helpers
    └── vite.config.ts
```

## FRAML Baseline Design

1. Detection: real-time rule evaluation on transaction ingest.
2. Risk calibration: thresholds can vary by customer risk band.
3. Alert quality: deduplication avoids repetitive active alerts.
4. Case workflow: OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED or DISMISSED.
5. Governance: rule versioning, toggle, and change-log entries.

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.x

See **[SETUP.md](SETUP.md)** for full local setup instructions (database creation, backend/frontend configuration, and run steps).

```bash
# Backend
cd backend
mvn spring-boot:run

# Frontend
cd ui
npm install
npm run dev
```

- Backend API: `http://localhost:8080` (Swagger UI: `/swagger-ui.html`)
- Frontend: `http://localhost:3001`

## API Overview

- `POST /api/transactions` - ingest transaction and run FRAML checks
- `GET /api/transactions` - list/search/filter transactions
- `GET /api/transactions/:id` - transaction detail
- `GET /api/alerts` - list alerts with status/severity filters
- `GET /api/alerts/:id` - alert details with transaction + timeline
- `POST /api/alerts/:id/acknowledge` / `/investigate` / `/close` / `/dismiss` - lifecycle actions
- `GET /api/rules` - list monitoring rules
- `POST /api/rules` - create rule
- `PUT /api/rules/:id` - update rule
- `PATCH /api/rules/:id/toggle` - activate/deactivate rule
- `GET /api/dashboard/summary` - KPI cards for frontend

## Rule Types

| Type              | Description                                        |
|-------------------|-----------------------------------------------------|
| AMOUNT_THRESHOLD  | Alert when a single transaction exceeds amount      |
| VELOCITY          | Alert on N+ transactions within T minutes            |
| NEW_PAYEE         | Alert on first transaction to a new payee            |
| DAILY_LIMIT       | Alert when daily cumulative amount exceeded          |

## Suggested Team Workflow

- Branching: `main` (stable), `feature/*` for in-progress work.
- No direct commits to `main`; use Pull Requests.
- Every PR requires human teammate review.
- Use meaningful commit messages that explain **why**.
