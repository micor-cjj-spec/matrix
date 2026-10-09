# Spring Transaction and Consistency Reference

## 1. Proxy boundary

Spring's declarative transaction support is normally applied through an AOP proxy. A call from one method to another method on the same object does not pass through that proxy.

Risk pattern:

```java
@Service
public class VoucherServiceImpl {

    public void postBatch(List<Long> ids) {
        for (Long id : ids) {
            post(id); // same-class call: transactional advice may not run
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void post(Long id) {
        // writes voucher and ledger entries
    }
}
```

Preferred design:

- Move the item-level transactional operation to a separate Spring service and call that bean from the batch coordinator; or
- Use `TransactionTemplate` when programmatic transaction control makes the intended boundary clearer.

Do not use self-injection or `AopContext.currentProxy()` as the default design. They obscure the boundary and increase coupling to Spring AOP internals.

## 2. Batch transaction models

Choose and document one model.

### Whole-batch transaction

All items succeed or all items roll back.

Use when:

- The batch is one indivisible business operation.
- Batch size is bounded.
- Lock duration and rollback cost are acceptable.

Risks:

- Long-running transaction.
- Larger lock scope.
- One invalid item fails the entire batch.

### Per-item transaction

Each item commits or rolls back independently.

Use when:

- Partial success is an accepted product behavior.
- The response reports success and failure per item.
- Failed items can be retried independently.

Requirements:

- Each item call must pass through a Spring proxy.
- The batch result must make partial success explicit.
- Idempotency must protect retries.

## 3. Atomic financial operations

The following changes should normally share one transaction:

- Validate voucher state.
- Validate voucher lines and debit-credit balance.
- Create or replace dependent general-ledger entries.
- Update voucher amount and status.
- Record posting operator and posting time.

A failure between dependent writes must not leave a partially posted voucher.

## 4. External calls

A database transaction cannot roll back:

- An HTTP request already accepted by another service.
- A bank or payment instruction.
- A message already published and consumed.
- An email or notification already delivered.

Use patterns such as:

- Transactional outbox.
- Explicit state machine.
- Business idempotency key and database unique constraint.
- Retry with bounded backoff.
- Reconciliation and compensation jobs.
- Provider request/response audit records.

Update local success state only after a response whose semantics are clearly understood.

## 5. Concurrency protection

For each financial write, determine whether to use:

- Unique constraint.
- Conditional update with expected state in the `WHERE` clause.
- Optimistic version column.
- Pessimistic row lock.
- Distributed lock only when the resource truly spans processes and the failure model is understood.

Prefer database-enforced invariants over pre-insert duplicate queries.

Example conditional state transition:

```sql
UPDATE bizfi_fi_voucher
SET fstatus = 'POSTED', fposted_time = NOW()
WHERE fid = ?
  AND fstatus = 'AUDITED';
```

Treat an affected-row count of zero as a state conflict or repeated operation, not as success by default.

## 6. Review questions

Before completing a transaction-sensitive change, answer:

1. What is the atomic business operation?
2. Which public method owns the transaction?
3. Does every call enter through a Spring proxy?
4. What happens when the operation is repeated?
5. What database constraint is the final duplicate guard?
6. What happens after a partial external success?
7. How is the operation reconciled later?
8. Which tests prove rollback and repeated-call behavior?
