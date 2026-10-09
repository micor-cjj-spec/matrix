# Matrix Workflow Service MVP

## 1. Scope

The first phase provides a lightweight workflow engine for business documents. It supports:

- versioned workflow definitions
- idempotent workflow startup
- user tasks
- service tasks through registered handlers
- safe exclusive conditions
- approve and reject actions
- business object lookup
- task inbox queries
- terminal event Outbox callbacks with retry

The first phase deliberately excludes parallel gateways, countersign, add-sign, subprocesses, definition migration and arbitrary scripts.

## 2. Service boundary

Business systems own document data. Workflow Service owns definitions, instances, node instances, tasks, action logs and delivery events.

A business document is linked through:

```text
sourceSystem + businessType + businessId
```

The workflow database must not duplicate complete reimbursement, payment or contract records.

## 3. Database initialization

Create the database and execute:

```text
workflow-service/src/main/resources/sql/workflow_v1.sql
```

Runtime datasource settings:

```text
WORKFLOW_DB_URL
WORKFLOW_DB_USERNAME
WORKFLOW_DB_PASSWORD
```

Nacos settings:

```text
NACOS_ADDR
NACOS_GROUP
NACOS_NAMESPACE
```

## 4. Example definition

```json
{
  "tenantId": "default",
  "definitionKey": "expense_reimbursement",
  "definitionName": "费用报销审批",
  "createdBy": "admin",
  "definition": {
    "nodes": [
      {
        "key": "start",
        "name": "开始",
        "type": "START"
      },
      {
        "key": "attachmentCheck",
        "name": "影像检查",
        "type": "SERVICE_TASK",
        "handlerKey": "attachment-check",
        "config": {
          "requiredCategories": ["INVOICE", "PAYMENT_PROOF"]
        }
      },
      {
        "key": "firstReview",
        "name": "初审",
        "type": "USER_TASK",
        "assigneeRule": {
          "type": "ROLE",
          "value": "FINANCE_FIRST_REVIEWER"
        }
      },
      {
        "key": "amountGateway",
        "name": "金额判断",
        "type": "EXCLUSIVE_GATEWAY"
      },
      {
        "key": "secondReview",
        "name": "复审",
        "type": "USER_TASK",
        "assigneeRule": {
          "type": "VARIABLE",
          "value": "secondReviewerId"
        }
      },
      {
        "key": "end",
        "name": "结束",
        "type": "END"
      }
    ],
    "transitions": [
      {"from": "start", "to": "attachmentCheck", "priority": 0},
      {"from": "attachmentCheck", "to": "firstReview", "priority": 0},
      {"from": "firstReview", "to": "amountGateway", "priority": 0},
      {
        "from": "amountGateway",
        "to": "secondReview",
        "priority": 10,
        "condition": {
          "field": "amount",
          "operator": "GT",
          "value": 10000
        }
      },
      {"from": "amountGateway", "to": "end", "priority": 0},
      {"from": "secondReview", "to": "end", "priority": 0}
    ]
  }
}
```

Create and publish:

```text
POST /api/workflow/definitions
POST /api/workflow/definitions/expense_reimbursement/versions/1/publish?tenantId=default
```

## 5. Start a workflow

```http
POST /api/workflow/instances
Idempotency-Key: fi-expense-202607220028
Content-Type: application/json
```

```json
{
  "tenantId": "default",
  "definitionKey": "expense_reimbursement",
  "sourceSystem": "fi-service",
  "businessType": "expense_claim",
  "businessId": "EXP202607220028",
  "initiatorId": "10001",
  "callbackUrl": "http://fi-service/api/workflow/callback",
  "variables": {
    "amount": 12000,
    "secondReviewerId": "10008",
    "attachmentCategories": ["INVOICE", "PAYMENT_PROOF"]
  }
}
```

Repeated requests with the same tenant, source system and `Idempotency-Key` return the original instance.

## 6. Task operations

Query an inbox:

```text
GET /api/workflow/tasks?tenantId=default&assigneeType=ROLE&assigneeValue=FINANCE_FIRST_REVIEWER&status=PENDING
```

Approve:

```http
POST /api/workflow/tasks/{taskId}/actions
X-Request-Id: approve-request-001
Content-Type: application/json
```

```json
{
  "action": "APPROVE",
  "operatorId": "10008",
  "comment": "影像和金额核对无误",
  "variables": {
    "approved": true
  }
}
```

Reject by changing `action` to `REJECT`.

For `USER` tasks the service checks the operator directly. For `ROLE` tasks, Gateway or Auth Service must verify role membership before forwarding the request.

## 7. Outbox callbacks

Terminal workflow changes insert an Outbox event in the same database transaction. A scheduler claims events using a conditional status update, posts the payload to `callbackUrl`, and retries failures with exponential backoff.

Business systems must use the `X-Workflow-Event-Id` header as an idempotency key and store processed event IDs with a unique index.

Current delivery is HTTP callback. RabbitMQ publishing and HMAC callback signatures are planned extensions.

## 8. Extension rules

Custom automatic nodes implement:

```java
WorkflowNodeHandler
```

Each handler has a unique `handlerKey`. Workflow definitions may configure handler parameters but cannot execute arbitrary Java, Groovy, SpEL or JavaScript code.

## 9. Next phase

Recommended next work:

1. integrate Auth Service role membership checks
2. add attachment metadata and presigned upload APIs
3. add return-to-initiator and resubmission
4. add transfer, claim and add-sign actions
5. add signed callback requests
6. add workflow monitoring and manual Outbox retry screens
7. implement a Vue Flow definition designer in matrix-web
