# Matrix Pre-Merge Review Checklist

Use this as a final coverage check. Mark an item as not applicable only when the reason is clear.

## Scope and architecture

- [ ] Owning module is correct.
- [ ] No unnecessary cross-module dependency was introduced.
- [ ] Controller contains no business workflow.
- [ ] Service owns state and transaction logic.
- [ ] Mapper contains persistence logic only.
- [ ] Existing API and data compatibility were considered.

## Financial correctness

- [ ] Source and target states are validated.
- [ ] Debit and credit remain balanced where applicable.
- [ ] Closed-period restrictions are enforced.
- [ ] Posted history is not directly edited or deleted.
- [ ] Reversal or correction remains traceable.
- [ ] Amounts and rates use `BigDecimal` with explicit precision.
- [ ] Organization, ledger, currency, and period scope are explicit.
- [ ] Reporting and reconciliation impact was reviewed.

## Transactions and concurrency

- [ ] Transactional method is public and called through a Spring proxy.
- [ ] Dependent financial writes are atomic.
- [ ] Batch transaction model is intentional.
- [ ] Idempotency key or repeated-request behavior is defined.
- [ ] Database unique constraint protects critical duplicates.
- [ ] Conditional update results are checked.
- [ ] External success cannot be incorrectly rolled back locally.
- [ ] Retry and reconciliation behavior are documented.

## Database

- [ ] A new versioned migration was added.
- [ ] Existing rows remain valid during rollout.
- [ ] Nullability and defaults are safe.
- [ ] Monetary columns use appropriate `DECIMAL` types.
- [ ] Indexes match actual query predicates.
- [ ] Large-table changes have a bounded rollout plan.
- [ ] Data changes have before/after count or total validation.
- [ ] SQL is parameterized.

## API and security

- [ ] Authentication is required where appropriate.
- [ ] Authorization and ownership are enforced.
- [ ] Tenant isolation is preserved.
- [ ] Request validation is complete.
- [ ] Responses do not expose secrets or internal errors.
- [ ] Mutating APIs define idempotency behavior.
- [ ] List APIs are paginated or otherwise bounded.
- [ ] High-risk actions require appropriate authorization/confirmation.

## AI capabilities

- [ ] Model credentials are externalized.
- [ ] Model timeout and context bounds are configured.
- [ ] Conversation ownership is checked.
- [ ] Retrieval scope, limits, and citations are correct.
- [ ] Fallback is labeled and does not invent financial facts.
- [ ] Prompt injection cannot bypass application authorization.
- [ ] Tools are allowlisted and parameters are validated.
- [ ] Trace and usage metadata are recorded where available.

## Reliability and performance

- [ ] No unbounded query, loop, cache collection, or model context was added.
- [ ] N+1 queries were considered.
- [ ] Critical queries were checked with suitable indexes/`EXPLAIN`.
- [ ] External calls have timeout and failure handling.
- [ ] Long transaction and lock scope were considered.
- [ ] Logs contain enough context without sensitive data.

## Validation

- [ ] Normal path test exists or was run.
- [ ] Invalid state and validation tests exist or were run.
- [ ] Repeated-request behavior was tested.
- [ ] Rollback/partial-failure behavior was tested.
- [ ] Authorization failure was tested where relevant.
- [ ] Closed-period behavior was tested for financial writes.
- [ ] Affected Maven module and dependencies compile.
- [ ] Relevant test command and result are reported.
- [ ] No `.idea/`, `target/`, credential, or production-only configuration was added.
