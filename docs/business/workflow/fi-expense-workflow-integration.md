# FI Expense Workflow Integration

## Scope

This phase introduces the first real finance document that uses `workflow-service` as its approval engine.

The business document remains owned by `fi-service`. Workflow definitions, instances, tasks and approval history remain owned by `workflow-service`.

## Business lifecycle

```text
DRAFT
  -> APPROVING
  -> RETURNED -> APPROVING
  -> APPROVED | REJECTED | CANCELLED
```

`fi-service` never changes a document to `APPROVED` directly. Terminal states are applied from signed workflow callback events.

## Submit flow

```text
POST /api/fi/expense-reimbursements/{expenseId}/submit
  -> validate applicant and status
  -> update expense to APPROVING
  -> upsert fi_workflow_binding
  -> insert fi_event_outbox
  -> commit local transaction
```

The scheduled Outbox dispatcher calls workflow APIs after the business transaction commits.

- First submission: `POST /api/workflow/instances`
- Submission after return: `POST /api/workflow/instances/{instanceId}/resubmit`

Remote failures do not roll back the submitted business transaction. The Outbox row moves to `FAILED` and is retried with exponential backoff.

## Public APIs

### Create expense

```http
POST /api/fi/expense-reimbursements
```

```json
{
  "tenantId": "default",
  "applicantId": "10001",
  "departmentCode": "D001",
  "amount": 1280.50,
  "currency": "CNY",
  "description": "Customer visit travel reimbursement"
}
```

### Submit expense

```http
POST /api/fi/expense-reimbursements/{expenseId}/submit
X-Request-Id: expense-submit-20260722-001
```

```json
{
  "operatorId": "10001",
  "definitionKey": "expense-reimbursement",
  "comment": "Submit for approval"
}
```

### Query expense

```http
GET /api/fi/expense-reimbursements/{expenseId}
```

### Workflow callback

```http
POST /api/fi/expense/workflow/events
X-Workflow-Event-Id: <eventId>
X-Workflow-Timestamp: <epochSeconds>
X-Workflow-Signature: sha256=<hmac>
```

The signature input is:

```text
timestamp + "." + rawRequestBody
```

Both services must use the same `WORKFLOW_CALLBACK_SECRET`.

## Callback mapping

| Workflow event | Expense status | Binding status |
| --- | --- | --- |
| `INSTANCE_RETURNED` | `RETURNED` | `WAITING_RESUBMIT` |
| `INSTANCE_RESUBMITTED` | `APPROVING` | `RUNNING` |
| `INSTANCE_COMPLETED` | `APPROVED` | `COMPLETED` |
| `INSTANCE_REJECTED` | `REJECTED` | `REJECTED` |
| `INSTANCE_CANCELLED` | `CANCELLED` | `CANCELLED` |

`fi_workflow_event_log.event_id` is the callback idempotency key.

## Database migration

Run:

```text
fi-service/src/main/resources/sql/fi_workflow_expense_v1.sql
```

It creates:

- `fi_expense_reimbursement`
- `fi_workflow_binding`
- `fi_event_outbox`
- `fi_workflow_event_log`

## Configuration

`fi-service`:

```text
FI_WORKFLOW_BASE_URL
FI_WORKFLOW_CALLBACK_URL
WORKFLOW_CALLBACK_SECRET
WORKFLOW_CALLBACK_MAX_SKEW_SECONDS
FI_WORKFLOW_OUTBOX_BATCH_SIZE
FI_WORKFLOW_OUTBOX_DISPATCH_DELAY_MS
```

`workflow-service`:

```text
WORKFLOW_CALLBACK_SECRET
```

Production environments must use HTTPS and a non-default callback secret.

## Deferred

- attachment pre-check in the submit endpoint
- expense edit API after return
- user-facing approval detail aggregation
- cancellation API from `fi-service`
- reconciliation scheduler and manual recovery UI
- role membership resolution through Auth Service
