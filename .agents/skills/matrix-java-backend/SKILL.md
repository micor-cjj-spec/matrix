---
name: matrix-java-backend
description: Implement or modify Matrix Java backend features. Use for controllers, services, mappers, entities, Spring configuration, transactions, Redis, Feign, validation, performance, and backend tests.
metadata:
  author: micor
  version: "1.0.0"
---

# Matrix Java Backend

## Objective

Implement backend changes while preserving module boundaries, financial correctness, transactional consistency, API compatibility, and maintainability.

## Required context

Before editing:

1. Read `/AGENTS.md`.
2. Identify the owning Maven module.
3. Read an existing implementation with similar behavior.
4. Trace controller, service, mapper, database, and cross-service calls.
5. Determine whether the change affects financial state, concurrency, idempotency, or transactions.
6. Read `references/transaction.md` for transaction-sensitive changes.

## Layering

### Controller

- Receive and validate request data.
- Call a service interface.
- Return `ApiResponse<T>`.
- Do not implement business state transitions.
- Do not directly call mappers.
- Prefer request and response DTOs for new APIs.

### Service

- Own business rules and state-transition validation.
- Own transaction boundaries.
- Enforce idempotency and concurrency behavior.
- Coordinate repositories, external services, and domain calculations.
- Return domain results rather than HTTP-specific objects where practical.

### Mapper

- Perform persistence operations.
- Keep SQL explicit and reviewable.
- Do not hide multi-step business workflows inside mapper logic.
- Add indexes for real query patterns, not speculatively.

## Dependency injection

- Use constructor injection for new classes.
- Prefer immutable dependencies declared as `final`.
- Avoid adding new `@Autowired` fields.
- Where legacy classes use field injection, refactor only when the task scope supports safe validation.

## Data and precision

- Use `BigDecimal` for financial amounts and exact calculations.
- Set scale and rounding mode explicitly at defined business boundaries.
- Do not compare `BigDecimal` values with `equals` when scale differences are irrelevant; use `compareTo`.
- Do not expose database entities as writable public API contracts in new features.
- Validate enum and state values at service boundaries.

## Transaction and consistency rules

- Put `@Transactional` on public service methods.
- Use `rollbackFor = Exception.class` for financial mutations unless a narrower rule is intentionally documented.
- Do not rely on same-class method calls to activate Spring proxies.
- Define atomicity for batch operations explicitly.
- Combine a financial state update and dependent ledger-entry writes in the same transaction.
- Design external calls using idempotency keys, status transitions, retries, and reconciliation rather than assuming database rollback can undo them.

## Concurrency and idempotency

For mutable business operations, identify:

- The business idempotency key.
- The database uniqueness constraint.
- The allowed source and target states.
- The expected behavior for repeated requests.
- The locking or compare-and-set strategy.
- The retry boundary.

Application-level duplicate checks alone are not sufficient under concurrency. Use database constraints as the final consistency boundary where applicable.

## Exceptions and responses

- Throw business exceptions with actionable messages.
- Do not leak SQL, secrets, stack traces, or provider credentials to clients.
- Distinguish validation errors, missing resources, state conflicts, and infrastructure failures.
- Keep global exception handling consistent with the repository's existing pattern.

## Redis

- Define key format, ownership, TTL, and invalidation strategy.
- Do not use Redis as the only source of truth for financial state.
- Ensure serialization is stable and version-compatible.
- Avoid unbounded keys or collections.
- Design cache failure behavior explicitly.

## Feign and external services

- Set explicit timeouts through configuration.
- Define retry behavior carefully; avoid retrying non-idempotent operations blindly.
- Propagate only necessary authentication and tracing context.
- Validate provider responses before updating local success state.
- Record failure status for later compensation when appropriate.

## Implementation workflow

1. State the current behavior and required behavior.
2. Identify the owning module and package.
3. Identify affected states, tables, APIs, and callers.
4. Define transaction and idempotency boundaries.
5. Implement the smallest complete change.
6. Add tests for normal, boundary, repeated-call, and failure behavior.
7. Run the affected module's tests with required dependencies.
8. Review the diff using `matrix-code-review`.

## Verification

Prefer:

```bash
mvn -pl <module> -am test
```

For compile-only validation when explicitly appropriate:

```bash
mvn -pl <module> -am clean package -DskipTests
```

Check:

- Compilation succeeds.
- Relevant tests pass.
- Transaction boundaries are effective through Spring proxies.
- Repeated requests do not create duplicate financial effects.
- No secret, production-only address, IDE file, or build output was added.
- API and database compatibility impacts are documented.

## Completion report

Report:

- Changed modules and files.
- Business behavior before and after.
- Transaction and idempotency decisions.
- Validation commands and results.
- Remaining risks or follow-up work.
