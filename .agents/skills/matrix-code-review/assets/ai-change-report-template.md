# Matrix AI Change Report

## Scope

- Task:
- Owning module(s):
- Related frontend/backend repository:
- Applicable skills read:

## Behavior changed

Describe the user-visible or business behavior. Do not only list files.

## Files changed

| File | Purpose |
|---|---|
| `path/to/file` | Reason for the change |

## Financial invariants

- [ ] Debit and credit remain balanced where applicable.
- [ ] Closed-period restrictions remain enforced.
- [ ] Posting/reversal remains idempotent and traceable.
- [ ] Amount precision and rounding are explicit.
- [ ] Organization, ledger, currency, and period scope are correct.
- [ ] Not applicable; reason recorded below.

Notes:

## Transaction and concurrency review

- Transaction boundary:
- Batch transaction model:
- Idempotency key or uniqueness control:
- Retry/reconciliation behavior:
- Concurrent update behavior:

## API and database compatibility

- API contract impact:
- Database migration:
- Index impact:
- Backward compatibility:
- Rollback approach:

## Validation

| Command/check | Result |
|---|---|
| `python .agents/scripts/validate_skills.py` | |
| SQL/controller/sensitive-content checks | |
| Maven test/build command | |
| Manual verification | |

## Remaining risks

List unresolved risks, assumptions, unavailable dependencies, and checks that could not run. Use `None identified` only after an explicit review.