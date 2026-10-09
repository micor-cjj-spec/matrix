# BOTP V3 API

## 执行恢复

- `GET /api/botp/executions/{executionId}/logs`
- `POST /api/botp/executions/{executionId}/resume`
- `POST /api/botp/executions/{executionId}/retry-writeback`

`resume` 仅支持目标单已经创建后的阶段。`retry-writeback` 只处理已有补偿任务。

## 关系生命周期

- `POST /api/botp/relations/target-events`
- `POST /api/botp/relations/{relationId}/invalidate`
- `POST /api/botp/relations/{relationId}/recompute`

目标事件示例：

```json
{
  "eventId": "FI-PAYMENT-VOID-2001",
  "tenantId": "default",
  "targetSystemCode": "MATRIX",
  "targetDocumentType": "FI_PAYMENT_APPLICATION",
  "targetDocumentId": "2001",
  "targetStatus": "VOID",
  "reason": "付款申请作废",
  "operator": "admin"
}
```

## 反写任务

- `GET /api/botp/operations/writeback-tasks`
- `POST /api/botp/operations/writeback-tasks/{taskId}/retry`

## 自动对账

- `GET /api/botp/operations/reconciliation-issues`
- `POST /api/botp/operations/reconciliation/run?autoFix=true`
- `POST /api/botp/operations/reconciliation-issues/{issueId}/fix`
- `POST /api/botp/operations/reconciliation-issues/{issueId}/ignore`

## FI 内部接口

- `POST /api/arap-doc/internal/botp/payment-applications/{fid}/void`

付款申请作废提交后同步通知 BOTP。通知异常只记录日志，由 BOTP 对账补偿。
