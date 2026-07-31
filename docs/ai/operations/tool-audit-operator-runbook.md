# AI Tool Audit Operator Runbook

## Purpose

This runbook covers the named-operator audit API and stale execution reconciliation introduced in Phase 9.

The feature only reads execution audit data and updates abandoned audit rows from `STARTED` to `TIMED_OUT`. It does not change finance business data.

## Database migrations

Apply in order:

```text
sql/bizfi_ai_tool_audit_v1.sql
sql/bizfi_ai_tool_audit_v2.sql
sql/bizfi_ai_tool_audit_v3.sql
```

V3 creates `bizfi_ai_audit_access_log` and adds the stale-execution lookup index.

## Required configuration

### base-service

```text
FINANCE_AI_AUDIT_BASE_URL=http://127.0.0.1:10003/api
FINANCE_AI_AUDIT_INTERNAL_TOKEN=<dedicated-audit-secret>
AI_AUDIT_VIEWER_USER_IDS=7
AI_AUDIT_RECONCILER_USER_IDS=8
```

The user-ID lists are migration-only. Once JWTs carry authorities, prefer:

```text
AI_TOOL_AUDIT_VIEW
AI_TOOL_AUDIT_RECONCILE
```

A reconciler automatically receives view permission.

### fi-service

```text
FINANCE_AI_AUDIT_INTERNAL_TOKEN=<same-dedicated-audit-secret>
FINANCE_AI_AUDIT_RECONCILIATION_ENABLED=true
FINANCE_AI_AUDIT_STARTED_TIMEOUT_MINUTES=15
FINANCE_AI_AUDIT_RECONCILIATION_DELAY_MS=300000
FINANCE_AI_AUDIT_RECONCILIATION_BATCH_SIZE=100
```

Do not reuse `FINANCE_AI_TOOL_INTERNAL_TOKEN` as the audit token.

## Public operator API

All endpoints require a valid Matrix login token.

### Detail

```http
GET /api/ai/admin/tool-executions/{requestId}
Authorization: Bearer <user-token>
```

Required permission: `AI_TOOL_AUDIT_VIEW`.

### Search

```http
GET /api/ai/admin/tool-executions?organizationId=10&period=2026-07&status=TIMED_OUT&page=1&size=20
Authorization: Bearer <user-token>
```

Supported exact filters:

```text
userId
organizationId
period
status
conversationId
modelTraceId
createdFrom
createdTo
page
size
```

Allowed statuses:

```text
STARTED
SUCCEEDED
FAILED
TIMED_OUT
```

### Manual stale reconciliation

```http
POST /api/ai/admin/tool-executions/reconcile-stale
Authorization: Bearer <user-token>
```

Required permission: `AI_TOOL_AUDIT_RECONCILE`.

## Internal protocol

`base-service` calls:

```text
GET  /api/internal/ai/audit/tool-executions/{requestId}
GET  /api/internal/ai/audit/tool-executions
POST /api/internal/ai/audit/tool-executions/reconcile-stale
```

Required headers:

```text
X-Matrix-AI-Audit-Token
X-Matrix-Audit-Operator-Id
X-Matrix-Audit-Operator-Roles
X-Matrix-Audit-Request-Id
```

The operator headers are generated from the authenticated server context. Clients must not be allowed to supply or override them directly.

## Access-log verification

After a successful detail request:

```sql
SELECT faccessrequestid,
       foperatorid,
       foperatorroles,
       faction,
       ffiltersummary,
       foutcome,
       fresultcount,
       fdurationms,
       ferrorcode,
       fcreatetime
FROM bizfi_ai_audit_access_log
ORDER BY fid DESC
LIMIT 10;
```

Expected action values:

```text
DETAIL
SEARCH
RECONCILE
```

Successful audit reads fail closed: if the access log cannot be inserted, the API returns service unavailable rather than returning untracked audit data.

## Stale execution verification

Create or identify a test row that remains `STARTED` longer than the configured timeout. After scheduled or manual reconciliation, verify:

```sql
SELECT frequestid,
       fstatus,
       ferrorcode,
       ferrormessage,
       fdurationms,
       fendtime
FROM bizfi_ai_tool_execution
WHERE frequestid = '<test-request-id>';
```

Expected values:

```text
fstatus      = TIMED_OUT
ferrorcode   = EXECUTION_TIMEOUT
fendtime     = reconciliation time
```

The update includes `WHERE fstatus = 'STARTED'`, so a concurrently completed execution cannot be overwritten by the reconciler.

## Troubleshooting

### 401 from fi-service

Check that `FINANCE_AI_AUDIT_INTERNAL_TOKEN` matches between `base-service` and `fi-service`.

### 403 from base-service

The authenticated user lacks the viewer or reconciler permission. Check JWT authorities or the migration user-ID lists.

### 503 on successful query

The access-log insert failed. Check that V3 was applied and that `bizfi_ai_audit_access_log` is writable.

### Records remain STARTED

Check:

- reconciliation is enabled;
- the timeout has elapsed;
- the scheduler delay is reasonable;
- `idx_ai_tool_started_timeout` exists;
- scheduled reconciliation errors in `fi-service` logs.
