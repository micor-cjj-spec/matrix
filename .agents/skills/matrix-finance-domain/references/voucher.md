# Voucher Domain Reference

## 1. Aggregate structure

A voucher consists of:

- Header: number, date, period, organization, ledger, type, summary, status, currency context, source, operator timestamps.
- Lines: account, accounting dimensions, summary, debit amount, credit amount, original currency amount, rate, cash-flow item, source-line reference.
- Derived entries: posted general-ledger entries and report effects.

Treat the voucher header and lines as one business aggregate for validation and posting.

## 2. State model

Recommended transition model for the current project:

```text
DRAFT
  -> SUBMITTED
      -> AUDITED
          -> POSTED
              -> REVERSED
      -> REJECTED
          -> SUBMITTED
```

Rules:

- `DRAFT`: header and lines may be edited and the voucher may be deleted.
- `REJECTED`: header and lines may be corrected and resubmitted.
- `SUBMITTED`: ordinary editing is blocked; the voucher may be audited or rejected.
- `AUDITED`: ordinary editing is blocked; the voucher may be posted.
- `POSTED`: ordinary editing and deletion are blocked; accounting effects exist.
- `REVERSED`: the original posting remains in history and is neutralized by a traceable reversal voucher.

Do not implement arbitrary target-state updates.

## 3. Validation before submission or posting

Validate at least:

- Voucher exists.
- Voucher date and accounting period are valid.
- Period is open for the requested operation.
- Voucher contains at least two valid lines.
- Every line has an account.
- Debit and credit are non-negative unless a documented rule says otherwise.
- A line is not simultaneously debit and credit.
- A line is not zero on both sides.
- Debit total equals credit total using normalized base-currency amounts.
- Required dimensions for the account are present.
- Voucher number is unique.

Submission may validate the complete voucher. Posting must validate again because persisted data or period status may have changed after submission.

## 4. Posting

Posting should be atomic:

1. Verify current state is `AUDITED`.
2. Revalidate period and balance.
3. Protect against repeated posting.
4. Create general-ledger entries from voucher lines.
5. Update voucher totals and status.
6. Record posting operator and time.
7. Commit all dependent writes together.

Idempotency options include:

- Unique constraint on the ledger entry's voucher-line reference.
- Conditional state update from `AUDITED` to `POSTED`.
- Delete-and-rebuild only when the operation is protected by state, transaction, and uniqueness rules.

A pre-query alone is not a sufficient duplicate guard under concurrency.

## 5. Reversal

A reversal should:

- Require a posted source voucher.
- Create a new voucher with a unique number.
- Reference the original voucher ID and number.
- Preserve relevant dimensions, currency, rate, and source context.
- Swap debit and credit accounting effects.
- Post the reversal atomically.
- Mark the original voucher as reversed or otherwise link the pair according to the chosen model.
- Retain the original voucher and entries for audit history.

Do not delete posted entries as a reversal implementation.

## 6. Voucher numbering

The number generator must define:

- Scope: global, organization, ledger, voucher type, year, or period.
- Prefix format.
- Sequence reset rule.
- Concurrency behavior.
- Database uniqueness constraint.
- Retry behavior after a duplicate-key conflict.

The pattern `select max number -> add one -> insert` has a concurrency window. Protect it with a sequence table, atomic counter, locking strategy, or duplicate-key retry backed by a unique index.

## 7. Precision

Define constants for:

- Base amount scale.
- Original amount scale per currency.
- Exchange-rate scale.
- Rounding mode.

Normalize amounts at controlled boundaries. Do not repeatedly round during intermediate calculations unless the accounting rule requires it.

## 8. Auditability

Record when applicable:

- Creator and creation time.
- Submitter and submission time.
- Auditor and audit time.
- Poster and posting time.
- Reversal operator and reversal time.
- Source system and source document identifiers.
- Idempotency key or external request identifier.

## 9. Minimum test matrix

- Draft creation with valid header.
- Draft update allowed; submitted update rejected.
- Submit balanced voucher.
- Reject unbalanced voucher.
- Audit only submitted voucher.
- Post only audited voucher.
- Repeated post creates no duplicate entries.
- Posting failure rolls back entries and status.
- Closed-period submission/posting rejected.
- Reversal produces opposite entries and traceability.
- Concurrent voucher-number generation remains unique.
