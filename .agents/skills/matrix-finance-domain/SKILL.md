---
name: matrix-finance-domain
description: Implement or review Matrix finance-domain behavior. Use for vouchers, ledger entries, accounting periods, closing, AR/AP, reconciliation, write-off, multi-currency, and financial reports.
metadata:
  author: micor
  version: "1.0.0"
---

# Matrix Finance Domain

## Objective

Protect accounting correctness when implementing or changing financial behavior. A technically valid implementation is not complete if it violates accounting state, period, balance, traceability, or reconciliation rules.

## Required context

Before editing finance behavior:

1. Read `/AGENTS.md`.
2. Read the affected entity, service, mapper, SQL, and neighboring workflow.
3. Identify organization, ledger, accounting period, currency, source document, voucher, and operator scope.
4. Write down the current and target business states.
5. Read the relevant references in this skill.
6. Combine this skill with `matrix-java-backend` and, when schema changes are involved, `matrix-database-change`.

## Core concepts

### Voucher lifecycle

Use explicit states and validated transitions. The current project commonly uses:

```text
DRAFT -> SUBMITTED -> AUDITED -> POSTED -> REVERSED
            |
            -> REJECTED -> SUBMITTED
```

Do not infer that every transition is valid merely because the caller supplies a target status.

Read `references/voucher.md` before modifying voucher state, posting, reversal, numbering, or lines.

### Accounting period

Every financial mutation must determine its accounting period and whether that period permits the action.

Read `references/period-close.md` before modifying period checks, closing, reopening, carry-forward, or period-end processing.

### General ledger

- Posted ledger entries must be traceable to their source voucher and voucher line.
- Posting must not create duplicate entries for the same business effect.
- Ledger queries must define organization, ledger, period/date range, currency, and account scope.
- Do not change posted accounting effects through direct CRUD updates.

### AR/AP and reconciliation

- Preserve links between source documents, settlements, write-offs, vouchers, and remaining balances.
- A write-off must not exceed the open amount.
- Repeated settlement or write-off requests must be idempotent.
- Partial settlement and partial write-off behavior must be explicit.
- Reconciliation differences must remain explainable and auditable.

### Financial reports

- State the report's accounting basis and scope.
- Report figures must be derivable from posted accounting data or a documented snapshot.
- Avoid mixing draft and posted data unless the report explicitly supports forecast or simulation views.
- Totals and subtotals must use deterministic precision and rounding.
- Mapping changes require impact analysis on historical reports.

## Mandatory invariants

- Debit total equals credit total before posting.
- A voucher must have the minimum valid number of lines.
- Each line has a valid account and exactly one effective debit/credit direction.
- Negative values, zero values, and dual-sided values follow explicit business rules.
- Posted vouchers are immutable through ordinary edit APIs.
- Reversal creates a traceable opposite effect rather than deleting history.
- Closed periods reject prohibited mutations.
- Amounts and exchange rates use `BigDecimal` and explicit scales.
- Voucher numbers and business document numbers are protected by database uniqueness where required.
- Financial operations are auditable by operator and time.

## Multi-currency

For multi-currency behavior, define:

- Transaction currency.
- Base currency.
- Original amount.
- Exchange rate direction.
- Rate precision.
- Base amount precision.
- Rounding mode.
- Rate source and effective date.

Do not reverse an amount by recomputing with a current exchange rate; preserve the original accounting effect unless the business rule explicitly requires revaluation.

## State-transition implementation

For every transition:

1. Load the current persisted state.
2. Validate the source state.
3. Validate required dependent data.
4. Perform calculations with documented precision.
5. Write dependent accounting records.
6. Update the aggregate state.
7. Record operator, time, source, and traceability fields.
8. Ensure the whole operation has the intended transaction boundary.
9. Define repeated-request behavior.

## Testing expectations

Include tests for:

- Valid state transition.
- Invalid source state.
- Debit-credit imbalance.
- Missing or invalid lines.
- Closed period.
- Repeated posting or settlement request.
- Partial failure and rollback.
- Precision and rounding boundaries.
- Reversal traceability.
- Concurrent number generation or duplicate business key where relevant.

## Review questions

Before completion, answer:

1. Which accounting invariant is affected?
2. What are the permitted source and target states?
3. What prevents duplicate accounting effects?
4. What happens in a closed period?
5. How is the result traced to source documents and operators?
6. What precision and rounding rules apply?
7. Can the operation be reconciled after partial failure?
8. Do reports remain consistent with the change?

## Completion report

Report:

- Affected finance capability and workflow.
- State transitions and invariants enforced.
- Transaction and idempotency decisions.
- Database constraints or migrations.
- Test scenarios and results.
- Historical-data or reporting impact.
