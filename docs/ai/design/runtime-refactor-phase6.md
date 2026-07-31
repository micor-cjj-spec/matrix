# AI Runtime Refactor Phase 6: Controlled Month-End Close Tool

## Goal

Turn the existing month-end workbench into the first business-value AI tool without giving the model write access or authority to choose security-sensitive parameters.

## Request flow

```text
Authenticated user
  -> base-service /ai/chat or /ai/chat/stream
  -> DefaultAiToolPolicyService
       - tool feature flag
       - tool allow-list
       - accounting period validation
       - organization-scope authorization
       - server-generated request ID
  -> ai-service internal model API
  -> Spring AI ChatClient
       - tool registered only for authorized tool-calling requests
       - ToolContext contains server-controlled user/org/period values
  -> FinanceMonthEndCloseTool
  -> fi-service /internal/ai/tools/month-end-close-check
  -> existing BizfiFiPeriodProcessService.monthEndWorkbench
  -> bounded read-only result
  -> model explanation
```

## Security boundary

The model does not generate or receive tool arguments for:

- user ID
- organization ID
- accounting period
- internal request ID
- internal service token

Those values are provided through Spring AI ToolContext after base-service authorization. The visible tool has no model-controlled business parameters.

Tool calling is disabled by default:

```text
AI_TOOL_CALLING_ENABLED=false
AI_TOOL_ALLOW_ALL_ORGANIZATIONS=false
AI_TOOL_ALLOWED_ORGANIZATION_IDS=
```

An organization is allowed when one of the following is true:

1. a matching authority is present, such as `ORG:10` or `organization_10`;
2. the organization ID is in `AI_TOOL_ALLOWED_ORGANIZATION_IDS`;
3. `AI_TOOL_ALLOW_ALL_ORGANIZATIONS=true` is explicitly enabled for a controlled development environment.

## Supported tool

```text
month-end-close-check
```

The Spring AI tool name is:

```text
monthEndCloseCheck
```

It is read-only and cannot:

- post vouchers;
- approve documents;
- close or reopen periods;
- create accounting entries;
- update finance master data.

## Reused finance capability

The tool delegates to the existing `monthEndWorkbench` implementation, which already evaluates:

- organization finance configuration;
- foundation-data health;
- accounting period status;
- unposted and exceptional vouchers;
- general-ledger balance;
- month-end processing modules;
- report readiness;
- final close decision.

The internal tool response removes voucher details and limits output to a configurable number of check items and warnings.

## Internal protocol

Endpoint:

```text
POST /internal/ai/tools/month-end-close-check
X-Matrix-AI-Tool-Token: <shared secret>
```

Request:

```json
{
  "requestedByUserId": 7,
  "organizationId": 10,
  "period": "2026-07",
  "requestId": "tool_..."
}
```

The response contains readiness, close status, aggregate voucher counts, bounded check items, warnings, and `readOnly=true`.

## Public request example

```json
{
  "userMessage": "检查 2026 年 7 月月结阻塞项，并给出处理顺序",
  "taskType": "tool-calling",
  "toolName": "month-end-close-check",
  "organizationId": 10,
  "accountingPeriod": "2026-07"
}
```

## Configuration

`base-service`:

```text
AI_MODEL_ADAPTER=spring-ai
AI_TOOL_CALLING_ENABLED=true
AI_TOOL_ALLOWED_ORGANIZATION_IDS=10
```

`ai-service`:

```text
FINANCE_SERVICE_BASE_URL=http://127.0.0.1:10003/api
FINANCE_AI_TOOL_INTERNAL_TOKEN=<shared secret>
FINANCE_AI_TOOL_TIMEOUT_SECONDS=20
```

`fi-service`:

```text
FINANCE_AI_TOOL_INTERNAL_TOKEN=<same shared secret>
FINANCE_AI_TOOL_MAX_CHECK_ITEMS=20
FINANCE_AI_TOOL_MAX_WARNINGS=20
```

## Observability

Tool execution adds:

```text
matrix.ai.tools.calls
matrix.ai.tools.duration
```

Tags are limited to the bounded tool name and outcome. User IDs, organization IDs, periods, prompts, and request IDs are not metric tags.

## Known limitations

- Organization authorization currently relies on security authorities or a configured organization allow-list; a dedicated organization-permission service should replace the configuration fallback.
- The finance tool client uses a static base URL in this phase.
- Tool execution audit persistence is not yet implemented; only request IDs and metrics are available.
- Streaming tool calls depend on provider support for tool calling in streamed ChatClient interactions.
