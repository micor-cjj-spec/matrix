# AI Runtime Refactor Phase 8: Correlated Tool Audit and Operational Search

## Goal

Make one controlled finance-tool execution traceable across the public conversation, model runtime, and finance execution boundary, then provide a bounded internal search API for operations and incident investigation.

## Correlation flow

```text
base-service
  conversationId
  requestId
      -> ai-service
         selected modelName
         modelTraceId generated before model execution
             -> Spring AI ToolContext
                conversationId
                requestId
                modelName
                modelTraceId
                    -> fi-service
                       bizfi_ai_tool_execution
```

The same model trace identifier is used by the Spring AI tool invocation and the final synchronous or streaming model response. This prevents a stream response from reporting a trace that cannot be matched to the finance execution.

## Persisted correlation fields

The audit table adds:

```text
fconversationid
fmodelname
fmodeltraceid
```

Together with the existing unique `frequestid`, these fields allow investigation by:

- public AI conversation;
- selected tool-calling model;
- model-runtime trace;
- authenticated user;
- authorized organization;
- accounting period;
- execution status and time.

The audit table still does not store prompts, chat messages, knowledge chunks, voucher details, internal secrets, or complete finance responses.

## Internal search endpoint

```text
GET /api/internal/ai/tools/executions
X-Matrix-AI-Tool-Token: <FINANCE_AI_TOOL_INTERNAL_TOKEN>
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

Status is restricted to:

```text
STARTED
SUCCEEDED
FAILED
```

Results are ordered by creation time and ID descending. Page numbering starts at 1, the default size is 20, and the server caps size at 100.

The API deliberately does not support searching prompts, error-message substrings, voucher content, or arbitrary SQL sort expressions.

## Example

```text
GET /api/internal/ai/tools/executions?organizationId=10&period=2026-07&status=FAILED&page=1&size=20
```

Response shape:

```json
{
  "page": 1,
  "size": 20,
  "total": 1,
  "totalPages": 1,
  "items": [
    {
      "requestId": "tool_...",
      "conversationId": "conv_...",
      "modelName": "gpt-tool-model",
      "modelTraceId": "trace_...",
      "toolName": "month-end-close-check",
      "userId": 7,
      "organizationId": 10,
      "period": "2026-07",
      "status": "FAILED"
    }
  ]
}
```

## Database migration

Apply migrations in order:

```text
sql/bizfi_ai_tool_audit_v1.sql
sql/bizfi_ai_tool_audit_v2.sql
```

The v2 migration is rerunnable and checks `information_schema` before adding columns and indexes.

## Security properties

- The endpoint remains internal-token protected.
- Correlation identifiers originate from trusted service code, not model-generated tool arguments.
- Query filters are exact and bounded.
- Page size is capped before SQL pagination is constructed.
- Ordering is fixed by server code.
- No prompt or finance-detail search surface is introduced.

## Rollout

1. Deploy the preceding controlled tool and audit phases.
2. Apply v1 and then v2 database migrations.
3. Deploy `base-service`, `ai-service`, and `fi-service` from the same compatible release set.
4. Run a synchronous month-end check and verify one request ID links the conversation, model trace, and finance execution.
5. Repeat with streaming chat and verify the final SSE trace equals the persisted model trace.
6. Verify filtered pagination by organization, period, status, and time range.
7. Keep the endpoint restricted to internal operations until operator-role authentication is added.

## Current limitations

- The recorded model name is the selected routed model at tool-call time; a provider may report a different canonical model name in final metadata.
- Search uses the finance-tool internal token and does not yet have named operator roles or per-operator audit logging.
- Retention, archival, export, and stale-STARTED reconciliation are follow-up work.
- The finance tool still uses a static service URL in this phase.
- No human-confirmed finance write tools are included.
