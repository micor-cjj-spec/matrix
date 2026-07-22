# Workflow Phase 2: Return, Resubmit and Cancel

## Scope

This phase completes the document correction loop on top of the workflow MVP:

```text
RUNNING
  ├─ APPROVE → next node / COMPLETED
  ├─ REJECT → REJECTED
  ├─ RETURN_TO_INITIATOR → WAITING_RESUBMIT
  └─ CANCEL by initiator → CANCELLED

WAITING_RESUBMIT
  ├─ RESUBMIT → RUNNING at the original approval node
  └─ CANCEL by initiator → CANCELLED
```

The workflow definition is not changed when a task is returned. The engine records the original approval node in reserved runtime variables and creates a synthetic `__RESUBMIT__` task assigned to the initiator.

## New task action

### Return to initiator

```http
POST /api/workflow/tasks/{taskId}/actions
X-Request-Id: return-expense-001
Content-Type: application/json
```

```json
{
  "action": "RETURN_TO_INITIATOR",
  "operatorId": "reviewer-1001",
  "comment": "The invoice image is unreadable",
  "variables": {}
}
```

Effects:

1. The current approval task becomes `RETURNED`.
2. The current node instance becomes `COMPLETED` with the return action in its output.
3. The workflow instance changes from `RUNNING` to `WAITING_RESUBMIT`.
4. A synthetic `__RESUBMIT__` task is created for the initiator.
5. `INSTANCE_RETURNED` is written to the Outbox in the same transaction.

## Resubmit API

```http
POST /api/workflow/instances/{instanceId}/resubmit
X-Request-Id: resubmit-expense-001
Content-Type: application/json
```

```json
{
  "operatorId": "initiator-2001",
  "comment": "Invoice image replaced",
  "variables": {
    "attachmentCount": 3,
    "amount": 12000
  }
}
```

Rules:

- Only the workflow initiator may resubmit.
- The instance must be in `WAITING_RESUBMIT`.
- The open `__RESUBMIT__` task is completed with status `RESUBMITTED` using optimistic locking.
- The instance returns to the approval node that issued the return.
- A fresh approval node instance and task are created; the historical returned task is never reused.
- `INSTANCE_RESUBMITTED` is written to the Outbox.

## Cancel API

```http
POST /api/workflow/instances/{instanceId}/cancel
X-Request-Id: cancel-expense-001
Content-Type: application/json
```

```json
{
  "operatorId": "initiator-2001",
  "reason": "The business document was voided"
}
```

Rules:

- Only the initiator may cancel.
- Cancellation is allowed in `RUNNING` and `WAITING_RESUBMIT`.
- The instance update uses `status + version` as the compare-and-set condition.
- All open tasks and active node instances are marked `CANCELLED` in the same transaction.
- `INSTANCE_CANCELLED` is written to the Outbox.

## Runtime states

### Workflow instance

| Status | Meaning |
|---|---|
| `RUNNING` | The process has an active approval or automatic node |
| `WAITING_RESUBMIT` | The initiator must modify the business document and resubmit |
| `COMPLETED` | The process reached an end node successfully |
| `REJECTED` | An approver rejected the whole process |
| `CANCELLED` | The initiator cancelled the process |

### Task

| Status | Meaning |
|---|---|
| `PENDING` | Waiting for processing |
| `CLAIMED` | Claimed by a processor |
| `APPROVED` | Approval completed successfully |
| `REJECTED` | Approval rejected the process |
| `RETURNED` | Approval returned the document to the initiator |
| `RESUBMITTED` | Initiator completed the correction task |
| `CANCELLED` | Task closed because the workflow was cancelled |

## Callback events

Business systems can receive the following additional events through the existing Outbox dispatcher:

```text
INSTANCE_RETURNED
INSTANCE_RESUBMITTED
INSTANCE_CANCELLED
```

The payload includes:

```json
{
  "eventId": "...",
  "eventType": "INSTANCE_RETURNED",
  "instanceId": "...",
  "tenantId": "default",
  "sourceSystem": "fi-service",
  "businessType": "expense_claim",
  "businessId": "EXP-20260722-001",
  "status": "WAITING_RESUBMIT",
  "variables": {},
  "occurredAt": "2026-07-22T17:00:00"
}
```

Recommended business status mapping:

| Event | Business status |
|---|---|
| `INSTANCE_RETURNED` | `RETURNED` |
| `INSTANCE_RESUBMITTED` | `APPROVING` |
| `INSTANCE_CANCELLED` | `CANCELLED` or `DRAFT`, according to business rules |

## Concurrency guarantees

- Approval, return and resubmit tasks use conditional updates on `task.status` and `task.version`.
- Return, resubmit and cancel instance transitions use conditional updates on `instance.status` and `instance.version`.
- Historical tasks are immutable after completion; resubmission creates a new approval task.
- Instance state changes, action logs, tasks and Outbox events are committed in one local database transaction.

## Deferred items

The following remain outside this phase:

- Return to an arbitrary earlier node
- Transfer, add-sign, countersign and parallel gateways
- Attachment metadata and presigned upload APIs
- Role membership verification through Auth Service
- Callback HMAC signatures
- Business document integration in `fi-service`
