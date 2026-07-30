# ADR-0002: Protected Branch Governance for main and qa

- Status: Accepted
- Date: 2026-07-28
- Owners: lele, julie, ommen, lucas

## Context

The project needs consistent release quality and auditable collaboration across frontend, backend, and database changes. Direct pushes to stable branches increase risk of regressions and unreviewed architecture drift.

## Decision

Apply protected branch governance to both `main` and `qa` with the following controls:

1. Mandatory Pull Requests (no direct pushes).
2. At least one human approval before merge.
3. Required passing CI checks from GitHub Actions workflow `CI`:
   - `Backend Tests (Maven)`
   - `Frontend Tests & Build (Vitest)`
4. Require all review discussions to be resolved before merge.

Repository policy additions:

- Use meaningful commit messages (e.g., `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`).
- Use PR template checklist for governance consistency.
- Keep architecture-impacting decisions recorded as ADRs under `docs/adr`.

## Consequences

Positive:

- Better change quality gates before merge.
- Traceable decision history and review outcomes.
- Reduced risk of unstable releases.

Trade-offs:

- Slightly slower merge throughput.
- Additional reviewer and CI wait time.

## Follow-up

- Ensure branch `qa` exists remotely.
- Configure GitHub branch protection/rulesets for both `main` and `qa`.
- Keep CI workflow and PR template aligned with branch protection requirements.
