# Contributing

Thanks for contributing. This project is documentation-driven: read the relevant docs before non-trivial work. Setup and commands are in [`README.md`](README.md); AI agents also follow [`AGENTS.md`](AGENTS.md).

## Before you start

- Find your task in [`docs/00-master-plan.md`](docs/00-master-plan.md) (`P1-07`, `P2-03.2`, …) and read its acceptance criteria.
- Do not contradict a settled decision in [`docs/00-decision-register.md`](docs/00-decision-register.md) (`DR-nn`).
- Backend: read [`docs/03-architecture/code-architecture.md`](docs/03-architecture/code-architecture.md) first (layers, module rules, where to put classes).
- If code and docs disagree, fix the doc in the same change, or ask.

## Workflow

1. Branch from `dev`: `feat/<task-id>-<slug>` (e.g. `feat/p2-08-seat-hold`). Use `fix/`, `docs/`, `chore/` for non-feature work.
2. `dev` is protected: direct pushes are only allowed for the repository owner. Everyone else opens a pull request.
3. Keep the change scoped to one task. One dependency upgrade per PR.
4. Run `make fmt lint test` before pushing, and `make it` when touching persistence, migrations, or outbox/jobs.
5. Open a PR into `dev` using the template. CI must be green (`lint`, `test`, `it`, `build`, `pr-title`).

## Commits and PR titles

[Conventional Commits](https://www.conventionalcommits.org/), scope = module, in English:

```
feat(notification): outbox relay with lease and backoff (P1-06)
fix(booking): release expired holds in one statement

Refs: P2-08
```

Allowed types: `feat fix docs refactor test chore perf build ci`. The PR title is checked by CI against the same rule.

## Code rules

Summary; the full text is in `AGENTS.md` and `code-architecture.md`.

- Backend: modular monolith, calls go downward only (controller/job/listener → service → repository/client); `@Transactional` only in `service`; entities never leave the service layer; Spring Data JDBC and hand-written SQL, no JPA.
- Never oversell. Never `save()` to change an entity's `status`; use conditional updates.
- Migrations: Flyway, `V<yyyymmddHHmm>__<snake_case>.sql`; never edit an applied migration.
- Errors: throw `common.error` exceptions; add messages to both `messages_en.properties` and `messages_vi.properties`.
- Frontend: TypeScript strict; UI strings through i18n with both `en` and `vi` keys.
- Do not weaken architecture tests to make code pass.

## Language

Code, identifiers, logs, API error codes, commits, PR titles: **English**. Docs: **Vietnamese**. UI: `en` + `vi`.

## Security

Never commit secrets; `.env` files stay local (use `.env.example`). `scripts/check-secrets.sh` runs in `make lint`. Report vulnerabilities privately to the repository owner rather than in a public issue.
