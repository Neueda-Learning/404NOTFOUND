# Transaction Monitoring Development Package v3.0

## Files

- `Transaction_Monitoring_Complete_Requirements_v3.0.docx` — product requirements, five-page frontend design, backend design, MySQL model, API mapping, tests and acceptance.
- `Transaction_Monitoring_OpenAPI_By_Page_v3.0.yaml` — OpenAPI 3.0.3 contract, grouped by frontend page.
- `Transaction_Monitoring_MySQL8_v3.0.sql` — MySQL 8.0 / InnoDB DDL, constraints, indexes and append-only audit triggers.
- `Transaction_Monitoring_Seed_Rules_v3.0.json` — four default active rules.

## Recommended implementation baseline

- Frontend: React 18, TypeScript, Vite, React Router, TanStack Query, Zustand, Ant Design.
- Backend: Java 21, Spring Boot 3.3, Spring JDBC or jOOQ, Flyway.
- Database: MySQL 8.0, UTC, InnoDB, utf8mb4.

## Startup order

1. Run the MySQL DDL.
2. Insert Account and Payee fixture data.
3. Load the four seed rules and write initial RuleHistory rows.
4. Start the backend and verify `/api/v1/health/ready` and Swagger.
5. Generate the frontend API client from the OpenAPI file.
6. Run unit, integration, E2E and performance tests described in the DOCX.

## Contract rules

- Amounts are JSON strings and database DECIMAL values.
- All API times are UTC RFC 3339.
- Lists use keyset/cursor pagination.
- Alert actions require `expectedStatus`, `expectedVersion`, and `Idempotency-Key`.
- Transaction creation is idempotent by Transaction ID plus canonical payload hash.
