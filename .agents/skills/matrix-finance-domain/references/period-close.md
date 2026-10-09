# Accounting Period and Closing Reference

## 1. Period identity

A period must be identified by the accounting context required by the product, typically including:

- Organization.
- Ledger or accounting book.
- Fiscal year.
- Period number or year-month.
- Currency context where applicable.

A global environment-variable list of closed months may be useful for a prototype, but it is not sufficient for organization- and ledger-specific production accounting.

## 2. Suggested states

Use explicit period states, for example:

```text
OPEN -> PRE_CLOSING -> CLOSED
                      -> REOPENED -> CLOSED
```

The exact names may differ, but the allowed operations in each state must be documented.

- `OPEN`: ordinary voucher creation, submission, audit, posting, and approved adjustments are allowed.
- `PRE_CLOSING`: normal posting may be restricted while checks and adjustments are performed.
- `CLOSED`: prohibited mutations are rejected.
- `REOPENED`: controlled adjustments are allowed with authorization and audit records.

## 3. Closing checks

Before closing, verify applicable checks such as:

- Unposted or unaudited vouchers.
- Debit-credit inconsistencies.
- Subledger-to-general-ledger differences.
- AR/AP unreconciled exceptions.
- Cash-flow assignment completeness.
- Required depreciation, accrual, allocation, or carry-forward processing.
- Report consistency.
- Interface or outbox failures affecting financial completeness.

Each check should return:

- Check code.
- Status.
- Severity.
- Count or amount affected.
- Actionable details.
- Whether it blocks closing.

## 4. Closing transaction

Closing should:

1. Confirm the period is in an allowed source state.
2. Run or verify required blocking checks.
3. Prevent concurrent posting into the period.
4. Generate required period-end vouchers or snapshots idempotently.
5. Record the closing operator and time.
6. Move the period to `CLOSED` atomically where practical.

Long-running checks may execute outside one database transaction, but the final transition must verify that the checked version or cutoff is still valid.

## 5. Reopening

Reopening is a controlled operation, not a direct status edit.

Require:

- Authorization.
- Reason.
- Operator and timestamp.
- Source closed state.
- Impact assessment on later periods and reports.
- Decision on whether later periods must also be reopened or recalculated.

Keep an audit trail of close and reopen events.

## 6. Cross-period restrictions

When a period is closed, reject prohibited operations based on the voucher's accounting date and accounting context. Do not rely only on the current calendar month.

Consider:

- Backdated voucher creation.
- Voucher reversal into a different period.
- Exchange-rate adjustment.
- Carry-forward regeneration.
- Report snapshot invalidation.
- Reconciliation changes that affect a closed period.

## 7. Concurrency

Avoid the race:

```text
posting checks period OPEN
closing changes period to CLOSED
posting commits after closing
```

Possible protections:

- Lock the period row during final close and posting authorization.
- Include expected period state/version in conditional updates.
- Use a version column and retry/conflict handling.
- Establish a closing cutoff and block new mutations before finalization.

## 8. Minimum test matrix

- Open period permits valid posting.
- Closed period rejects posting and prohibited edits.
- Closing fails with blocking checks.
- Closing succeeds when checks pass.
- Repeated close is idempotent or returns a clear state conflict.
- Concurrent posting cannot commit after final close.
- Reopen requires authorization and reason.
- Reopen event is auditable.
- Later-period impact is handled according to the documented policy.
