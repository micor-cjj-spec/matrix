Follow the repository-wide instructions in `/AGENTS.md`.

Canonical reusable project skills are stored under `/.agents/skills/`.

Before changing code:

1. Identify the owning Maven module and any impact on the sibling `matrix-web` repository.
2. Read every applicable `SKILL.md` under `/.agents/skills/`.
3. Preserve financial invariants, transaction boundaries, idempotency, authorization, and migration safety.
4. Run the relevant Maven build or tests.
5. Report changed behavior, validation results, and remaining risks.

Do not duplicate project rules in this file. Update `/AGENTS.md` or the canonical skill instead.
