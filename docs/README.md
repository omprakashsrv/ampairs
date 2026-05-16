# Developer Documentation

All developer documentation for Ampairs lives here, organized by concern.

---

## API

| Document | Description |
|----------|-------------|
| [authentication.md](api/authentication.md) | API key authentication — setup, usage, rotation |
| [api-key-testing.md](api/api-key-testing.md) | Testing API key authentication flows |
| [app-updates.md](api/app-updates.md) | Desktop app update API — endpoints, versioning, checksums |

## Deployment

| Document | Description |
|----------|-------------|
| [deployment.md](deployment/deployment.md) | Production deployment — server setup, SSH, service config |
| [production-validation.md](deployment/production-validation.md) | Post-deployment validation checklist |
| [cicd-app-updates.md](deployment/cicd-app-updates.md) | CI/CD pipeline for desktop app release automation |

## Guides

| Document | Description |
|----------|-------------|
| [database-migrations.md](guides/database-migrations.md) | Flyway migration workflow, naming conventions, rollback |
| [configuration.md](guides/configuration.md) | Application configuration — Flyway DDL, Hibernate, environment vars |
| [spring-security.md](guides/spring-security.md) | Spring Security configuration reference |
| [file-storage.md](guides/file-storage.md) | File storage, S3 integration, MIME type handling |

## Features

| Document | Description |
|----------|-------------|
| [account-deletion.md](features/account-deletion.md) | Account deletion — architecture, API, testing, Google Play compliance |

---

## Root-level docs

| Document | Description |
|----------|-------------|
| [README.md](../README.md) | Project overview, quick start, structure |
| [CLAUDE.md](../CLAUDE.md) | Coding standards and architectural patterns |
| [AGENTS.md](../AGENTS.md) | Guidelines for AI agents working in this repo |
