# Agent Validation Workflow

This file documents deterministic checks that AI agents and developers can run after changing Matrix.

## Skill structure

```bash
python .agents/scripts/validate_skills.py
```

Checks that every directory under `.agents/skills/` contains a valid `SKILL.md`, that the frontmatter `name` matches the directory name, and that a useful `description` is present.

## SQL changes

```bash
python .agents/skills/matrix-database-change/scripts/validate_sql.py sql/change.sql
```

With no explicit path, the script checks changed and untracked SQL files detected by Git.

Use `--strict` to treat warnings as failures.

## Java controller contracts

```bash
python .agents/skills/matrix-java-backend/scripts/check_controller_contract.py base-service/src/main/java/.../ExampleController.java
```

With no explicit path, the script checks changed and untracked Java controller files. It reports heuristic API-contract risks such as raw map responses and unnamed path variables.

## Sensitive content

```bash
python .agents/skills/matrix-code-review/scripts/scan_sensitive_content.py path/to/changed/file
```

With no explicit path, the script checks changed and untracked files. Public addresses are warnings; credentials, private keys, and recognizable access tokens are errors.

## Recommended order

1. Run the applicable deterministic checks.
2. Run Maven tests or a module build.
3. Perform the semantic review from `matrix-code-review`.
4. Report commands, results, skipped checks, and remaining risks.

These scripts supplement tests and review. They do not prove business correctness.