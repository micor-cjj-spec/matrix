# Matrix AI Runtime Refactor — Phase 9

## Goal

Phase 9 turns the finance-tool execution audit into an operator-safe operational capability.

It adds three controls:

1. a named operator authorization boundary for reading execution audit data;
2. a persistent access log for every detail, search, and reconciliation operation;
3. automatic reconciliation for executions that remain in `STARTED` beyond a bounded timeout.

This phase remains read-only with respect to finance business data. Reconciliation only updates audit-state metadata; it never posts vouchers, changes periods, approves documents, or invokes finance write operations.

## Current gaps

The Phase 8 internal endpoints are protected by the finance-tool token, but that token identifies a service rather than a human operator. Consequently:

- two operators using the same service token are indistinguishable;
- reading audit data is not itself audited;
- a process crash can leave an execution in `STARTED` indefinitely;
- operations cannot distinguish a running execution from an abandoned one.

## Trust boundaries

### Public operator boundary

`base-service` exposes the authenticated operator API:

```text
GET  /ai/admin/tool-executions/{requestId}
GET  /ai/admin/tool-executions
POST /ai/admin/tool-executions/reconcile-stale
```

`/ai/admin/**` is authenticated even though the compatibility `/ai/**` endpoints remain publicly routed and perform their existing ownership checks.

The operator permission boundary defines two roles:

```text
AI_TOOL_AUDIT_VIEW
AI_TOOL_AUDIT_RECONCILE
```

`AI_TOOL_AUDIT_RECONCILE` implies view permission. Until the platform identity service emits authorities in JWTs, exact configured user-ID lists are supported as a migration-only fallback.

### Internal finance boundary

Audit operations use a dedicated secret:

```text
FINANCE_AI_AUDIT_INTERNAL_TOKEN
```

It is separate from `FINANCE_AI_TOOL_INTERNAL_TOKEN`. The `base-service` proxy sends server-derived headers:

```text
X-Matrix-Audit-Operator-Id
X-Matrix-Audit-Operator-Roles
X-Matrix-Audit-Request-Id
```

`fi-service` validates both the dedicated token and the required operator role. Client-supplied operator identity is never forwarded directly.

## Access audit

A new table records every accepted audit operation:

```text
bizfi_ai_audit_access_log
```

Stored fields are bounded and operational only:

- access request ID;
- operator ID;
- normalized operator roles;
- action (`DETAIL`, `SEARCH`, or `RECONCILE`);
- safe exact-filter summary;
- outcome (`SUCCESS`, `DENIED`, or `FAILED`);
- result count;
- duration;
- bounded error code;
- creation time.

The access log never stores prompts, chat messages, knowledge snippets, voucher details, internal tokens, model API keys, or complete finance results.

Access-log persistence is fail-closed for successful reads: the service does not return audit data if it cannot persist the corresponding access record. This prevents silent, untracked reads.

## Stale execution reconciliation

A finance tool execution is stale when:

```text
status = STARTED
and
startTime < now - configured timeout
```

The reconciler changes only audit metadata:

```text
STARTED -> TIMED_OUT
```

It sets:

- `fendtime` and `fmodifytime`;
- bounded duration;
- `ferrorcode = EXECUTION_TIMEOUT`;
- a fixed, non-sensitive timeout message.

The update is conditional on the row still being `STARTED`, preventing a late reconciler from overwriting a concurrently completed execution.

The scheduler is configurable:

```text
FINANCE_AI_AUDIT_RECONCILIATION_ENABLED=true
FINANCE_AI_AUDIT_STARTED_TIMEOUT_MINUTES=15
FINANCE_AI_AUDIT_RECONCILIATION_DELAY_MS=300000
FINANCE_AI_AUDIT_RECONCILIATION_BATCH_SIZE=100
```

A manual reconciliation endpoint requires `AI_TOOL_AUDIT_RECONCILE`. Scheduled reconciliation is recorded as operator `system` with role `SYSTEM_RECONCILER`.

## Query behavior

Execution search remains exact and server bounded:

- page starts at 1;
- default size is 20;
- maximum size is 100;
- sort is fixed to newest first;
- `TIMED_OUT` is added to the allowed execution statuses;
- arbitrary sort, SQL fragments, prompt search, message search, and finance-content search remain unsupported.

## Database migration

Apply after audit v1 and v2:

```text
sql/bizfi_ai_tool_audit_v3.sql
```

The migration creates the operator access-log table and adds an index optimized for stale `STARTED` lookup.

## Rollout order

1. Merge the month-end tool and audit PR chain through Phase 8.
2. Apply `bizfi_ai_tool_audit_v1.sql` and `bizfi_ai_tool_audit_v2.sql`.
3. Apply `bizfi_ai_tool_audit_v3.sql`.
4. Configure distinct tool and audit internal tokens.
5. Configure at least one audit viewer and one reconciler operator.
6. Deploy compatible `base-service` and `fi-service` versions together.
7. Verify detail and search requests create access-log rows.
8. Create an intentionally stale `STARTED` row and verify it becomes `TIMED_OUT`.
9. Verify an operator with view-only permission cannot trigger manual reconciliation.

## Non-goals

Phase 9 does not add:

- finance write tools;
- arbitrary audit exports;
- prompt or message search;
- automatic deletion or retention;
- cross-tenant superuser access without explicit authorization;
- replacement of the platform identity and RBAC service.
