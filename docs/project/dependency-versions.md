# Dependency Versions

This file is the single source of truth for core runtime and tooling versions used in this repository.

## Backend

| Component | Version |
|-----------|---------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| springdoc-openapi-starter-webmvc-ui | 2.5.0 |
| Lombok | 1.18.38 |
| JaCoCo Maven Plugin | 0.8.12 |

## Frontend

| Component | Version |
|-----------|---------|
| Node.js | >=20.0.0 |
| React | 19.2.8 |
| React DOM | 19.2.8 |
| React Router DOM | 7.18.1 |
| Ant Design | 6.5.2 |
| @ant-design/charts | 2.6.7 |
| TypeScript | 6.0.3 |
| Vite | 8.1.5 |
| Vitest | 4.1.10 |
| @vitest/coverage-v8 | 4.1.10 |
| Axios | 1.18.1 |
| Day.js | 1.11.21 |

## Maintenance Rule

When upgrading dependencies, update:

1. `backend/pom.xml` or `ui/package.json`
2. this file (`docs/project/dependency-versions.md`)
3. user-facing setup docs (`README.md`, `SETUP.md`) when prerequisites change