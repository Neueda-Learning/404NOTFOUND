# FRAML Transaction Monitoring & Alerts Dashboard

This project implements a training-grade Transaction Monitoring system using a **FRAML foundation model**:

- Rules engine + behavioral checks
- Risk-band aware thresholds
- Alert deduplication and grouping
- Full alert lifecycle and audit trail
- Rule governance with versioning and change log

## Stack

- Backend: Node.js + Express
- Database: SQLite
- Frontend: Vanilla HTML/CSS/JS dashboard
- Tests: Jest + Supertest

## FRAML Baseline Design

1. Detection: real-time rule evaluation on transaction ingest.
2. Risk calibration: thresholds can vary by customer risk band.
3. Alert quality: deduplication avoids repetitive active alerts.
4. Case workflow: OPEN -> ACKNOWLEDGED -> INVESTIGATING -> CLOSED or DISMISSED.
5. Governance: rule versioning, toggle, and change-log entries.
6. Optimization loop: rule performance endpoint for tuning.

## API Overview

- `POST /api/transactions` - ingest transaction and run FRAML checks.
- `GET /api/transactions` - list/search/filter transactions.
- `GET /api/alerts` - list alerts with status/severity filters.
- `GET /api/alerts/:id` - alert details with transaction + timeline.
- `PATCH /api/alerts/:id/status` - lifecycle action.
- `GET /api/alerts/history` - resolved/dismissed history.
- `GET /api/rules` - list monitoring rules.
- `POST /api/rules` - create new rule.
- `PUT /api/rules/:id` - update rule.
- `PATCH /api/rules/:id/toggle` - activate/deactivate rule.
- `GET /api/rules/performance` - operational tuning metrics.
- `GET /api/dashboard/summary` - KPI cards for frontend.

## Run Locally

```bash
npm install
npm run start
```

Open:

- Dashboard: `http://localhost:3000`
- API health: `http://localhost:3000/api/health`

## Test & Coverage

```bash
npm test
```

Coverage thresholds are enforced in `jest.config.js`:

- lines/statements/functions: >= 70%
- branches: >= 60%

## Suggested Team Workflow (Spec-Aligned)

- Branching: `main` (stable), `qa` (integration), `feature/*`.
- No direct commits to `main`/`qa`; use Pull Requests.
- Every PR requires human teammate review.
- Use meaningful commit messages that explain **why**.

## GitHub Submission Steps

1. Initialize and commit:

```bash
git init
git add .
git commit -m "Initial FRAML transaction monitoring platform"
```

2. Connect to GitHub repository and push:

```bash
git branch -M main
git remote add origin <YOUR_GITHUB_REPO_URL>
git push -u origin main
```

If your class requires `qa` and feature branches, create them before collaborative development.
