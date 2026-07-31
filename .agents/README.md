# Matrix AI Skills

This directory is the canonical project-level skill library for AI coding agents.

## Structure

Each direct child of `.agents/skills/` is one independent skill:

```text
.agents/skills/<skill-name>/
├── SKILL.md       # required entry point
├── references/    # detailed material loaded when relevant
├── scripts/       # deterministic validation or generation tools
└── assets/        # reusable templates and checklists
```

Only `SKILL.md` is required. Create the other directories only when they contain useful material.

## Loading model

1. Read `/AGENTS.md` for repository-wide rules.
2. Identify the task type.
3. Read every matching `SKILL.md`.
4. Load only the referenced documents needed for the current change.
5. Run applicable validation before reporting completion.

## Available skills

- `matrix-java-backend`: Java, Spring, service layering, transactions, Redis, Feign, and backend testing.
- `matrix-finance-domain`: vouchers, ledgers, periods, AR/AP, reconciliation, and reports.
- `matrix-database-change`: tables, columns, indexes, migrations, and SQL safety.
- `matrix-ai-development`: models, prompts, conversations, knowledge retrieval, streaming, and AI safety.
- `matrix-code-review`: structured correctness, finance, transaction, security, database, and release review.

## Deterministic validation

See `.agents/VALIDATION.md` for complete usage.

```bash
python .agents/scripts/validate_skills.py
python .agents/skills/matrix-database-change/scripts/validate_sql.py
python .agents/skills/matrix-java-backend/scripts/check_controller_contract.py
python .agents/skills/matrix-code-review/scripts/scan_sensitive_content.py
```

With no explicit file arguments, the change-oriented checks inspect changed and untracked files detected by Git. Use `--strict` when warnings should fail the check.

## Maintenance rules

- Keep project-specific rules here; keep generic personal skills such as Superpowers in the user's global skills directory.
- Put permanent repository-wide constraints in `/AGENTS.md`, not in every skill.
- Put task workflows in `SKILL.md`.
- Put detailed explanations in `references/`.
- Put executable checks in `scripts/`.
- Put reusable templates in `assets/`.
- Avoid copying the same rule into multiple files. Link to the canonical rule instead.
- Keep each `SKILL.md` focused and preferably below 500 lines.