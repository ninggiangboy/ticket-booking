# Ticket Booking — Event ticketing platform

A platform where organizers draw their own seat maps and sell tickets through Stripe, built around one invariant that must hold at any load: **never oversell**. The current phase covers sign-in (magic link), seat maps, ticket reservation and payment.

Original design: [`event-ticket-booking-sdd.md`](event-ticket-booking-sdd.md). All project documents: [`docs/README.md`](docs/README.md). The docs are written in Vietnamese.

## Tech stack

| Area | Choice |
| --- | --- |
| Backend | Java 25, Spring Boot 4.0.x, Spring Modulith, Spring Data JDBC (no JPA), Gradle Kotlin DSL |
| Data | PostgreSQL (source of truth), Redis (rate limiting, waiting room), Flyway |
| Frontend | React 19, Vite, TypeScript, pnpm 10, Node 22 |
| Infrastructure | Docker Compose (nginx, api, postgres, redis, S3-compatible storage, Mailpit) |

The architecture is a **modular monolith**; module boundaries are enforced with Spring Modulith and ArchUnit. See [`docs/03-architecture/code-architecture.md`](docs/03-architecture/code-architecture.md).

## Repository layout

```
backend/        Spring Boot API (root package io.ticket)
frontend/       React + Vite SPA
deploy/compose  docker-compose, nginx, .env.example
scripts/        migration, secret and bundle-size checks (called by make)
spikes/         completed technical spikes (S-01)
docs/           project docs (Vietnamese), master plan, decision register
Makefile        the single command interface
```

## Prerequisites

Docker with Compose v2, JDK 25 (Temurin), Node 22 (`.nvmrc`), pnpm 10 (`corepack enable`), GNU Make. Full guide: [`docs/09-operations/local-dev.md`](docs/09-operations/local-dev.md).

## Quick start

```bash
corepack enable && pnpm --dir frontend install --frozen-lockfile
make up      # start the whole stack; creates deploy/compose/.env from .env.example
make down    # stop the stack
make reset   # wipe data and rebuild
```

After `make up`, the app is at `http://localhost:$NGINX_PORT` and dev email is in Mailpit (`$MAILPIT_UI_PORT`); ports are set in `deploy/compose/.env`.

For development with hot reload (infrastructure via compose, API via `bootRun`, frontend via Vite): `make dev`.

## Common commands

| Command | What it does |
| --- | --- |
| `make lint` | Spotless, ESLint, Prettier, typecheck, i18n, migration and secret checks |
| `make fmt` | Auto-format backend and frontend |
| `make test` | Backend and frontend unit tests |
| `make it` | Integration tests (Testcontainers, needs Docker) |
| `make build` | Build jar, frontend, check bundle size, build Docker image |
| `make logs S=api` | Tail logs of one service |
| `make psql` | Open psql on the local database |

CI (`.github/workflows/ci.yml`) calls only these `make` targets.

## Conventions

- Code, logs, API error codes, commits and PR titles are in **English**; docs are in **Vietnamese**; the UI is localized (`en`, `vi`).
- Conventional Commits with the module as scope, e.g. `feat(notification): …`.
- `dev` is the main branch; one branch per task: `feat/<task-id>-<slug>`.
- Progress and task list: [`docs/00-master-plan.md`](docs/00-master-plan.md).

## For AI agents

See [`AGENTS.md`](AGENTS.md) (and [`CLAUDE.md`](CLAUDE.md) for Claude Code).
