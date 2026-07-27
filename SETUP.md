# FRAML Transaction Monitoring System

A full-stack transaction monitoring and alerting system built with **React + TypeScript** (frontend), **Spring Boot** (backend), and **MySQL** (database).

## Tech Stack

| Layer     | Technology                                |
|-----------|-------------------------------------------|
| Frontend  | React 18 + TypeScript + Vite + Ant Design |
| Backend   | Java 17 + Spring Boot 3.2 + Spring JPA    |
| Database  | MySQL 8.x                                 |
| API Docs  | SpringDoc OpenAPI / Swagger UI            |

## Project Structure

```
framl-transaction-monitoring/
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

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.x

### 1. Database Setup

Start MySQL and run:
```sql
CREATE DATABASE IF NOT EXISTS transaction_monitoring CHARACTER SET utf8mb4;
```

### 2. Backend Setup

Update database credentials in `backend/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/transaction_monitoring?...
    username: root
    password: your_password
```

Start the backend:
```bash
cd backend
mvn spring-boot:run
```

The API will start at **http://localhost:8080**.  
Swagger UI: **http://localhost:8080/swagger-ui.html**

### 3. Frontend Setup

```bash
cd ui
npm install
npm run dev
```

The frontend will start at **http://localhost:3001**.

## Features

### Dashboard
- KPI cards: Open Alerts, Under Investigation, Today's Alerts, High Risk, Today's Transactions, Alert Rate
- 7-day alert trend bar chart
- Severity distribution
- Top triggered rules
- Quick navigation to Alerts and Transactions

### Risk Alerts
- Paginated alerts table with filters (status, severity, keyword)
- Click-to-detail drawer with status history
- Full detail page with investigation panel
- Alert lifecycle: OPEN → ACKNOWLEDGED → INVESTIGATING → CLOSED/DISMISSED
- Confirmation dialogs for irreversible actions

### Transactions
- Paginated transaction catalog
- Filter by status, type, keyword
- Transaction detail modal with alert info
- Deep-link to alert detail from transaction

### Monitoring Rules
- View all configured rules
- Create / Edit / Delete rules
- Toggle active/inactive per rule
- Supports: Amount Threshold, Velocity, New Payee, Daily Limit

## API Endpoints

| Method | Path                            | Description                    |
|--------|---------------------------------|--------------------------------|
| POST   | /api/transactions               | Ingest & evaluate transaction  |
| GET    | /api/transactions               | List/search transactions       |
| GET    | /api/transactions/:id           | Get transaction detail         |
| GET    | /api/alerts                     | List/search alerts             |
| GET    | /api/alerts/:id                 | Get alert detail               |
| POST   | /api/alerts/:id/acknowledge     | Acknowledge alert              |
| POST   | /api/alerts/:id/investigate     | Start investigation            |
| POST   | /api/alerts/:id/close           | Close alert                    |
| POST   | /api/alerts/:id/dismiss         | Dismiss alert                  |
| GET    | /api/rules                      | List all rules                 |
| POST   | /api/rules                      | Create rule                    |
| PUT    | /api/rules/:id                  | Update rule                    |
| PATCH  | /api/rules/:id/toggle           | Toggle rule active status      |
| GET    | /api/dashboard/summary          | Dashboard KPI data             |

## Rule Types

| Type              | Description                                      |
|-------------------|--------------------------------------------------|
| AMOUNT_THRESHOLD  | Alert when a single transaction exceeds amount   |
| VELOCITY          | Alert on N+ transactions within T minutes         |
| NEW_PAYEE         | Alert on first transaction to a new payee         |
| DAILY_LIMIT       | Alert when daily cumulative amount exceeded       |

## Default Rules (auto-initialized on startup)

- **Large Transaction Alert** – DEBIT/TRANSFER > $10,000 USD (HIGH)
- **High Frequency Transactions** – 5+ transactions in 10 minutes (MEDIUM)
- **New Payee Transaction** – First transaction to new payee (LOW)
- **Daily Limit Exceeded** – Daily DEBIT total > $50,000 USD (HIGH)

## Sample Transaction (via API)

```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "transactionId": "TXN-001",
    "accountId": "ACC-100",
    "payeeId": "PAYEE-200",
    "payeeName": "John Doe",
    "type": "DEBIT",
    "amount": 15000.00,
    "currency": "USD",
    "status": "COMPLETED",
    "transactionTime": "2026-07-27T10:00:00Z"
  }'
```

This will automatically trigger the Large Transaction Alert rule and create a HIGH severity alert.
