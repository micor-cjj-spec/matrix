@AGENTS.md

# Claude-specific entry point

- Treat `/AGENTS.md` as the repository-wide source of truth.
- Canonical project skills are stored under `/.agents/skills/`.
- Before implementing a task, identify and read every applicable `SKILL.md`.
- Load detailed files under `references/` only when relevant to the current task.
- Use the validation and completion requirements from `/AGENTS.md`.
- Do not duplicate or redefine project rules in this file; update the canonical source instead.
