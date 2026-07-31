---
name: matrix-code-review
description: Review Matrix code changes before merge. Use for pull requests, generated code, release checks, risk analysis, and validation of finance, transaction, database, security, performance, and maintainability concerns.
metadata:
  author: micor
  version: "1.0.0"
---

# Matrix Code Review

## Objective

Find concrete defects and release risks before merge. Prioritize correctness and financial consistency over cosmetic preferences.

## Required context

Before reviewing:

1. Read `/AGENTS.md`.
2. Read the applicable implementation skills.
3. Understand the requested behavior and acceptance criteria.
4. Inspect the complete diff and enough surrounding code to understand call paths.
5. Identify changed APIs, states, tables, queries, transactions, configuration, and external calls.
6. Use `assets/review-checklist.md` as the final coverage check.

## Review priority

Review in this order:

1. Financial and business correctness.
2. Data loss or corruption risk.
3. Transaction, concurrency, and idempotency.
4. Authorization, tenant isolation, and secret handling.
5. Database migration and compatibility.
6. API compatibility and error behavior.
7. Reliability and external-service failure handling.
8. Performance and resource bounds.
9. Test coverage.
10. Maintainability and style.

Do not bury high-severity findings under formatting comments.

## Severity levels

### Critical

Likely to cause financial corruption, unauthorized access, secret exposure, irreversible data loss, or a production-wide outage.

### High

Likely to cause incorrect business results, partial financial writes, duplicate effects, broken migrations, major security gaps, or frequent production failures.

### Medium

Causes incorrect edge behavior, weak operational recovery, degraded performance, confusing API behavior, or significant maintainability risk.

### Low

Localized quality issue with limited functional impact.

## Finding format

Every finding should include:

```text
Severity: High
Location: path/to/File.java:methodName

Problem:
Explain the concrete defect.

Impact:
Explain the realistic business or operational consequence.

Recommendation:
Describe the smallest reliable correction.
```

Reference exact files and lines when available. Do not report speculative issues without explaining the execution path that makes them plausible.

## Finance review

Check:

- Valid state transitions.
- Debit-credit balance.
- Closed-period restrictions.
- Posted-history immutability.
- Reversal traceability.
- Amount and rate precision.
- Organization, ledger, currency, and period scope.
- Source-document linkage.
- Reporting and reconciliation impact.

## Transaction and concurrency review

Check:

- Public method owns the transaction.
- Call passes through a Spring proxy.
- Dependent financial writes are atomic.
- Batch atomicity is intentional.
- Repeated request behavior is defined.
- Database uniqueness is the final duplicate guard where appropriate.
- Conditional updates check affected rows.
- External side effects have status, retry, compensation, and reconciliation behavior.

## Database review

Check:

- Migration is versioned and new.
- Existing rows remain valid.
- Nullability and defaults support rollout.
- Amount columns use appropriate `DECIMAL` definitions.
- Unique keys protect business invariants.
- Indexes match real query predicates.
- Large-table changes have a safe rollout plan.
- Data repairs define scope and reconciliation totals.
- SQL does not concatenate untrusted input.

## API and security review

Check:

- Authentication and authorization.
- Tenant and resource ownership.
- DTO validation.
- Sensitive information in responses and logs.
- Backward compatibility.
- Error classification.
- Pagination and bounded result sizes.
- Idempotency for mutating APIs.
- High-risk financial actions require appropriate confirmation and authorization.

## AI review

Check:

- Credentials are externalized.
- Model calls have timeouts and bounded context.
- Conversation ownership is enforced.
- Retrieval scope and citations are correct.
- Fallback is labeled and does not fabricate facts.
- Prompt injection cannot bypass authorization.
- Tools are allowlisted and validated.
- Usage, traces, and sanitized failures are observable.

## Performance review

Check:

- N+1 queries.
- Unbounded list queries or in-memory accumulation.
- Missing selective indexes.
- Functions or implicit conversions preventing index use.
- Long transactions and lock scope.
- Repeated external calls inside loops.
- Unbounded Redis keys or collections.
- Blocking model calls without timeout.

## Test review

Verify tests cover the changed behavior, including:

- Normal path.
- Invalid state.
- Boundary values and precision.
- Repeated request.
- Concurrent or duplicate behavior where relevant.
- Rollback after intermediate failure.
- Authorization failure.
- Closed period for financial writes.
- Migration compatibility or validation queries.

Do not accept a test that only asserts HTTP 200 when the important outcome is persisted state or accounting entries.

## Review output

Return:

1. Findings ordered by severity.
2. Open questions and assumptions.
3. Validation gaps.
4. A concise merge recommendation: block, revise, or acceptable.

If no defect is found, state that clearly but still list unverified areas and tests that were not run.
