---
name: matrix-database-change
description: Design, implement, or review Matrix database changes. Use for tables, columns, indexes, constraints, migrations, seed data, mapper SQL, performance, and data repair.
metadata:
  author: micor
  version: "1.0.0"
---

# Matrix Database Change

## Objective

Make database changes that are safe, versioned, reversible where practical, compatible with existing data, and aligned with real query and financial-consistency requirements.

## Required context

Before changing the database:

1. Read `/AGENTS.md`.
2. Identify the owning module and business aggregate.
3. Read the entity, mapper, mapper XML, service, existing SQL migrations, and relevant queries.
4. Determine expected row count, data history, write frequency, and critical query patterns.
5. Identify compatibility requirements for old application versions and existing data.
6. Combine this skill with the relevant backend and domain skills.

## Migration rules

- Put new migrations under `sql/` using a descriptive, versioned file name.
- Do not rewrite an already-released migration to represent a new production change.
- Keep schema change, data backfill, and destructive cleanup logically separated when rollout safety requires phases.
- Include comments explaining business purpose and rollout assumptions.
- Use idempotent DDL only when the deployment platform and MySQL version support it reliably.
- Document rollback or forward-fix strategy.

Recommended naming pattern:

```text
sql/<domain>_<capability>_v<version>.sql
```

Example:

```text
sql/bizfi_fi_voucher_posting_v2.sql
```

## Table and column design

- Use names consistent with existing domain prefixes.
- Use `BIGINT` for identifiers when matching existing project conventions.
- Use `DECIMAL` with documented precision and scale for amounts and rates.
- Use `DATETIME` or the established project type consistently for business timestamps.
- Define nullability from business meaning, not convenience.
- Give status columns explicit allowed values through application validation and, where practical, database constraints compatible with the target MySQL version.
- Include creation/update audit fields where required by the aggregate.
- Preserve source document, external request, and traceability identifiers for financial operations.

## Keys and constraints

For every table, determine:

- Primary key.
- Natural or business key.
- Unique constraints for idempotency and numbering.
- Foreign-key strategy or application-managed relationship policy.
- Delete behavior.
- Historical-data retention requirements.

Application duplicate checks do not replace unique constraints under concurrency.

## Index design

Create indexes from concrete access patterns:

- Equality filters first in composite indexes when selective and commonly used.
- Range columns usually follow equality columns.
- Match ordering requirements where this materially avoids sorting.
- Avoid redundant indexes that are prefixes of existing composite indexes unless justified.
- Review write amplification and storage cost.
- Use `EXPLAIN` for important or changed queries.

For finance queries, common dimensions may include organization, ledger, period/date, status, account, source document, and voucher number. Do not add all possible combinations blindly.

## Safe rollout patterns

For a non-null new column on existing data, prefer a phased rollout:

1. Add nullable column or column with a safe default.
2. Deploy code that writes the new value.
3. Backfill historical rows in bounded batches.
4. Validate completeness.
5. Add `NOT NULL` or stricter constraints later.

For large tables:

- Assess lock and online-DDL behavior.
- Avoid one unbounded update transaction.
- Batch backfills by primary-key range.
- Record progress and make the job restartable.
- Validate replica and backup impact where applicable.

## Financial data rules

- Never delete posted financial history as ordinary cleanup.
- Prefer status, reversal, archive, or audit-preserving correction mechanisms.
- Backfills affecting amounts, periods, balances, or reports require reconciliation queries.
- Data-repair SQL must define affected scope and expected before/after counts or totals.
- Monetary backfills must use deterministic precision and rounding.

## Mapper and SQL review

- Parameterize values; never concatenate untrusted input into SQL.
- Keep date boundaries explicit and consistent.
- Avoid implicit type conversion on indexed columns.
- Avoid functions on indexed filter columns when a range predicate can be used.
- Avoid unbounded queries in online request paths.
- Verify pagination count queries for complex joins.
- Review transaction and locking behavior for `SELECT ... FOR UPDATE` and bulk writes.

## Verification

Before completion:

1. Review migration syntax for the target MySQL version.
2. Apply the migration in a disposable or development database when available.
3. Verify entity and mapper compatibility.
4. Run `EXPLAIN` for changed critical queries.
5. Validate before/after row counts and financial totals for data changes.
6. Run the affected module tests.
7. Check that no credentials or production-only values are embedded.

## Completion report

Report:

- Migration files and schema changes.
- Existing-data and compatibility impact.
- Index rationale.
- Rollout and rollback/forward-fix strategy.
- Validation queries and results.
- Remaining operational risks.
