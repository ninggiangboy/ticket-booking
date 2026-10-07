<!-- PR title must follow Conventional Commits, scope = module: feat(notification): outbox relay (P1-06) -->

## What and why

<!-- What changed and why. Link the task, e.g. P2-08, and any DR-nn involved. -->

Refs: <!-- P2-08 -->

## How to verify

<!-- Commands run or manual steps. -->

## Checklist

- [ ] `make fmt lint test` passes locally
- [ ] `make it` passes (required when touching persistence, migrations, or outbox/jobs)
- [ ] Never-oversell invariant is not weakened; status changes use conditional updates, not `save()`
- [ ] Module boundaries respected; architecture tests (`ModularityTests`, `io.ticket.arch`) are unchanged and green
- [ ] New migration is a new file (no edit of an applied one) and follows `V<yyyymmddHHmm>__<snake_case>.sql`
- [ ] UI strings and error messages added in both `en` and `vi`
- [ ] Docs updated (in Vietnamese) if code and docs disagreed
- [ ] No secrets, no `latest` dependency versions; one dependency upgrade per PR
