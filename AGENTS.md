# AGENTS.md

Guidance for AI coding agents working in this repository. Humans: see `README.md`.

## Project

Event ticket booking platform: organizers draw seat maps and sell tickets via Stripe. Core invariant: **never oversell** under any load. Backend is a Spring Boot 4 / Java 25 modular monolith (PostgreSQL is the source of truth, Redis for rate limiting and waiting room); frontend is React 19 + Vite + TypeScript.

The project is documentation-driven. Before non-trivial work, read the relevant docs instead of guessing:

- `docs/README.md` — index of all documents and their status
- `docs/00-master-plan.md` — phases and tasks (`P1-07`, `P2-03.2`, …) with acceptance criteria
- `docs/00-decision-register.md` — decisions (`DR-nn`); do not contradict a settled DR
- `docs/03-architecture/code-architecture.md` — **layers, module rules, where to put classes** (read before writing backend code)
- `docs/03-architecture/tech-stack-and-versions.md` — pinned libraries
- `docs/06-design/error-handling.md`, `docs/07-api/api-guidelines.md` — errors and API conventions

If the code and a doc disagree, say so and fix the doc in the same change (or ask).

## Layout

```
backend/        Gradle Kotlin DSL, single project; package root io.ticket.<module>
frontend/       pnpm 10, Vite; map-core lives in src/map-core (not an npm package)
deploy/compose/ docker-compose and .env.example
scripts/        check-migrations.sh, check-secrets.sh, check-bundle-size.sh
docs/           Vietnamese project docs
Makefile        the single command interface
```

## Commands

Always use `make` targets; CI calls only these.

```bash
make lint     # spotlessCheck, eslint, prettier, tsc, i18n:check, migration + secret checks
make fmt      # spotlessApply + prettier
make test     # backend unit + frontend vitest
make it       # integration tests (Testcontainers; needs Docker)
make build    # bootJar, frontend build, bundle size, docker image
make up / down / reset / dev / logs S=<svc> / psql
```

Run `make fmt lint test` before finishing; run `make it` when touching persistence, migrations, or outbox/jobs. Single backend test: `./backend/gradlew -p backend test --tests '<FQCN>'`. Single frontend test: `pnpm --dir frontend test --run <file>`.

## Backend rules (summary; full text in code-architecture.md)

- Per module `io.ticket.<module>`: root package = public API (`…Api`, DTO records, events); sub-packages `controller | job | listener | service | repository | client | entity | dto | config` are internal.
- Calls go downward only: controller/job/listener → service → repository/client. `@Transactional` only in `service`. Entities never leave the service layer; map to DTO records by hand (no MapStruct).
- Other modules may use only a module's root package. No dependency cycles; allowed dependencies are declared in each `package-info.java` via `@ApplicationModule(allowedDependencies = …)`. Each table/Redis key family has exactly one owning module.
- Spring Data JDBC + `JdbcClient` with hand-written SQL. No JPA. Never `save()` to change an entity's `status`; use conditional updates.
- Migrations: Flyway, `V<yyyymmddHHmm>__<snake_case>.sql`; never edit an applied migration.
- Errors: throw `common.error` exceptions; responses are RFC 9457 Problem Details with i18n messages in `messages_en.properties` and `messages_vi.properties` (add both).
- Architecture tests (`ModularityTests`, `io.ticket.arch`) must stay green; do not weaken them to make code pass.

## Frontend rules

- TypeScript strict; UI strings go through i18n with both `en` and `vi` keys (`pnpm --dir frontend i18n:check` enforces parity).
- No hard-coded user-facing strings, secrets, or `latest` dependency versions.

## Conventions

- Code, identifiers, logs, API error codes, commits, PR titles: **English**. Docs: **Vietnamese**. UI: `en` + `vi`.
- Conventional Commits, scope = module, e.g. `feat(notification): outbox relay (P1-06)`; footer `Refs: P2-08` when tied to a task. Branches `feat/<task-id>-<slug>`; default branch `dev`.
- Never commit secrets; `scripts/check-secrets.sh` runs in lint. `.env` files stay local (use `.env.example`).
- Pin versions (version catalog `backend/gradle/libs.versions.toml`, `pnpm-lock.yaml`). One dependency upgrade per PR.
- Keep changes scoped to the task; match surrounding style and comment density. Do not commit or push unless asked.
