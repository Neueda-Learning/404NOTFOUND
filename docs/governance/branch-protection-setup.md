# Branch Protection Setup (main and qa)

This repository includes CI and PR governance files. Apply the following GitHub settings to enforce them.

## Precondition

- Remote branches exist: `main`, `qa`.
- Workflow exists: `.github/workflows/ci.yml`.

## Required Branch Protection Rules

Apply to **both** `main` and `qa`:

1. Require a pull request before merging.
2. Require approvals: at least 1.
3. Dismiss stale approvals when new commits are pushed.
4. Require status checks to pass before merging.
5. Required status checks:
   - `Backend Tests (Maven)`
   - `Frontend Tests & Build (Vitest)`
6. Require conversation resolution before merging.
7. Restrict direct pushes (admins optional by team policy).

## Why these checks

- PR-only flow prevents unreviewed direct commits.
- Human approval catches logic/design issues not covered by tests.
- CI checks ensure backend tests and frontend build/tests remain healthy.
- Conversation resolution ensures review feedback is fully addressed.
