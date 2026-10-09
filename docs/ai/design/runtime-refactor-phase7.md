# AI Runtime Refactor Phase 7: Persistent Tool Audit and Permission Boundary

## Goal

Make controlled finance tool execution traceable after the request finishes, while separating organization authorization from AI tool orchestration.

## Architecture

```text
base-service
  DefaultAiToolPolicyService
    -> AiOrganizationPermissionService
    -> server-generated ToolContext/requestId

ai-service
  Spring AI monthEndCloseCheck tool
    -> fi-service internal tool endpoint

fi-service
  token validation
    -> audit STARTED
    -> existing monthEndWorkbench
    -> audit SUCCEEDED or FAILED
```

## Permission boundary

`AiOrganizationPermissionService` owns organization-scope authorization. The default implementation supports:

1. organization authorities such as `ORG:10`;
2. migration-only exact `userId:organizationId` pairs;
3. explicit development-only allow-all mode.

`DefaultAiToolPolicyService` now owns only tool feature flags, tool allow-list validation, adapter requirements, period validation, and ToolContext creation.

A future RBAC database or remote permission center can replace the default permission implementation without changing AI orchestration.

## Persistent audit table

Apply:

```text
sql/bizfi_ai_tool_audit_v1.sql
```

Table:

```text
bizfi_ai_tool_execution
```

The unique business key is the server-generated `requestId`.

Persisted fields include:

- request ID and tool name;
- authenticated user ID;
- authorized organization ID;
- accounting period;
- STARTED, SUCCEEDED, or FAILED status;
- readiness score, blocking count, warning count, and close status;
- duration;
- bounded error code and sanitized error message;
- start and end timestamps.

Prompts, model messages, voucher details, knowledge chunks, and complete finance results are not stored in this table.

## Execution semantics

The controller records STARTED after internal-token validation and before calling the existing month-end workbench.

On success it records the safe aggregate outcome. On failure it records a bounded error summary and rethrows the original business exception.

Audit write failures are logged and do not replace the real month-end result or the original finance failure. This preserves read availability, while the protected audit query exposes missing persistence during verification.

## Protected audit lookup

```text
GET /api/internal/ai/tools/executions/{requestId}
X-Matrix-AI-Tool-Token: <FINANCE_AI_TOOL_INTERNAL_TOKEN>
```

The endpoint returns only the persisted execution summary. It uses the same least-privilege internal token as the finance tool endpoint.

## Validation

Tests cover:

- tool-policy delegation to the permission boundary;
- authority, configured pair, wrong-user, default-deny, and development allow-all authorization;
- trusted execution context on audit start;
- success and failure audit updates;
- original finance exception preservation;
- protected lookup by request ID.

## Rollout

1. Apply `sql/bizfi_ai_tool_audit_v1.sql` to the `fi-service` database.
2. Deploy `fi-service` and verify the audit lookup endpoint with an unknown request ID returns 404.
3. Enable the month-end tool for one authorized test user and organization.
4. Execute one successful and one intentionally failing check.
5. Verify the audit rows, duration, status, and sanitized error fields.
6. Only then expand organization permissions.

## Remaining work

- replace migration-only permission pairs with the platform organization-permission source;
- add retention and archival policy for audit rows;
- add an operator-facing audit search API with pagination and role checks;
- persist model/provider and trace identifiers alongside the business execution through a signed internal correlation contract;
- add human-confirmed write-tool audit states before any finance mutation tool is introduced.
