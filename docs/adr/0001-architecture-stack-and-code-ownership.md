# ADR-0001: Architecture Stack and Code Ownership

- Status: Accepted
- Date: 2026-07-28
- Owners: lele, julie, ommen, lucas
- Scope: Full repository (ui, backend, database, setup scripts)

## Context

This repository is implemented as a full-stack transaction monitoring system with:

- Frontend: React + TypeScript + Vite + Ant Design
- Backend: Spring Boot + Java 17
- Database: MySQL 8

Recent implementation and release work has aligned the codebase around these technologies, including:

- Premium monitoring-style UI and responsive behavior in ui
- Alert lifecycle APIs and rule processing in backend
- MySQL runtime configuration and seeded mock data workflow

To reduce architecture drift and improve collaboration, we need an explicit architectural decision and named owners.

## Decision

We standardize on the following architecture:

1. Frontend remains React + TypeScript + Vite in ui.
2. Backend remains Spring Boot (Java 17) in backend.
3. Database remains MySQL 8 with root runtime credentials configured in backend/src/main/resources/application.yml for local development.
4. API communication is through /api proxy from frontend to backend.
5. UI/UX direction follows a monitoring-oriented visual language with dense tables, clear status color semantics, and mobile-friendly layouts.

Code ownership and decision responsibility are assigned to four maintainers:

- lele
- julie
- ommen
- lucas

## Ownership Model

- Architecture changes (stack swap, framework migration, data model strategy) require review by at least 2 of the 4 owners.
- UI design-system changes require review by at least 1 owner familiar with frontend standards.
- Backend API contract or alert-lifecycle changes require review by at least 1 owner familiar with backend domain logic.
- Production-affecting config changes (DB connection, ports, environment profiles) should be reviewed by at least 1 owner before merge.

## Consequences

Positive:

- Clear technical baseline for future contributors.
- Reduced churn from accidental framework/style divergence.
- Explicit accountability for core decisions.

Trade-offs:

- Major changes may move slightly slower due to ownership review requirements.
- Contributors need to align with established stack conventions instead of introducing alternatives ad hoc.

## Follow-up

- Future architecture changes should add new ADR files under docs/adr.
- If ownership changes, update this ADR and record superseding ADR status.
