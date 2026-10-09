# OpenAPI V3：凭证草稿异步写入

## 1. 目标与边界

V3A 允许获得 `fi.voucher.write` 授权的外部应用可靠创建 Matrix 凭证草稿。

本期只开放草稿创建，不开放：

- 凭证提交
- 凭证审核
- 凭证过账
- 凭证冲销
- 凭证删除
- 外部回调与每日对账（V3B）

## 2. 外部接口

### 2.1 受理写入请求

```http
POST /api/open-api/v1/fi/voucher-requests
```

请求示例：

```json
{
  "externalBizNo": "EXPENSE-20260722-0001",
  "idempotencyKey": "expense:EXPENSE-20260722-0001",
  "organizationId": "ORG-001",
  "bookId": "BOOK-001",
  "voucherDate": "2026-07-22",
  "summary": "员工差旅报销",
  "lines": [
    {
      "lineNo": 1,
      "accountCode": "660201",
      "summary": "差旅费",
      "debitAmount": 1200.00,
      "creditAmount": 0
    },
    {
      "lineNo": 2,
      "accountCode": "224101",
      "summary": "应付员工款",
      "debitAmount": 0,
      "creditAmount": 1200.00
    }
  ]
}
```

成功受理返回 HTTP 202，任务初始状态为 `ACCEPTED`。

### 2.2 按请求 ID 查询

```http
GET /api/open-api/v1/fi/voucher-requests/{requestId}
```

### 2.3 按外部业务单号查询

```http
GET /api/open-api/v1/fi/voucher-requests/by-external-no/{externalBizNo}
```

## 3. POST 签名

POST 与 GET 使用同一 HMAC-SHA256 规则，但 POST 的 Canonical Request 必须包含原始 HTTP 请求体字节的 SHA-256：

```text
HTTP_METHOD
REQUEST_PATH
CANONICAL_QUERY
SHA256_HEX(RAW_REQUEST_BODY)
TIMESTAMP
NONCE
```

注意：

- 签名必须使用实际发送的 JSON 原始字节，不能先解析后重新序列化。
- `Content-Type` 建议固定为 `application/json; charset=UTF-8`。
- 请求体默认上限为 1 MiB，可通过 `OPENAPI_MAX_REQUEST_BODY_BYTES` 调整。
- 网关部署前缀 `/api` 不参与稳定外部路径签名，签名路径为 `/open-api/...`。

## 4. 幂等规则

数据库唯一键：

```text
app_id + idempotency_key
app_id + external_biz_no
```

处理规则：

| 场景 | 结果 |
|---|---|
| 首次请求 | 创建写入任务、分录和 Outbox |
| 相同幂等键且请求体 SHA-256 相同 | 返回原任务 |
| 相同幂等键但请求体不同 | `OPENAPI_VOUCHER_40901` |
| 外部业务单号已存在 | `OPENAPI_VOUCHER_40902` |
| RabbitMQ 重复消费 | `tenant_id + source_request_id` 返回原凭证 |

## 5. 事务与消息链路

```text
外部 POST
  -> 校验应用、签名、Nonce、授权和额度
  -> 同一数据库事务写入：
       matrix_open_api_write_request
       matrix_open_api_write_request_line
       matrix_open_api_write_status_log
       matrix_open_api_outbox_event
  -> Outbox 调度器 CAS 抢占事件
  -> RabbitMQ
  -> 消费者 CAS 抢占写入任务
  -> Feign 调用 fi-service 内部草稿创建接口
  -> fi-service 通过 source_request_id 二次幂等
  -> 写回 SUCCEEDED 或进入重试
```

Outbox 状态：

```text
PENDING -> SENDING -> SENT
                  \-> FAILED -> SENDING
                  \-> DEAD
```

写入任务状态：

```text
ACCEPTED -> PROCESSING -> SUCCEEDED
                       \-> RETRYING -> PROCESSING
                       \-> MANUAL_REQUIRED
```

处理超过配置时间仍为 `PROCESSING` 时，恢复任务会将其切回 `RETRYING` 并产生新 Outbox。

## 6. 写入授权

示例：

```json
{
  "organizationIds": ["ORG-001"],
  "bookIds": ["BOOK-001"],
  "maxLinesPerVoucher": 200,
  "dailyWriteQuota": 10000
}
```

约束：

- 租户由应用固定，调用方不能提交租户。
- 组织和账簿必须在授权范围内。
- 单张凭证分录至少 2 行，最多 500 行。
- 每行必须且只能填写借方或贷方金额。
- 借贷合计必须相等。
- 写权限与只读权限分开授权。

## 7. 管理接口

```http
GET  /api/openapi/admin/write-requests
GET  /api/openapi/admin/write-requests/{requestId}
POST /api/openapi/admin/write-requests/{requestId}/retry
```

人工重试仅允许 `PROCESSING_FAILED` 或 `MANUAL_REQUIRED` 状态。

前端入口：

```text
/openapi -> 写入任务
```

## 8. 部署配置

必须先执行：

```bash
mysql < sql/matrix_open_api_v3.sql
```

RabbitMQ 变量：

```text
RABBITMQ_HOST
RABBITMQ_PORT
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
RABBITMQ_VHOST
OPENAPI_WRITE_EXCHANGE
OPENAPI_WRITE_QUEUE
OPENAPI_WRITE_ROUTING_KEY
OPENAPI_WRITE_CONSUMERS
OPENAPI_WRITE_MAX_CONSUMERS
```

调度变量：

```text
OPENAPI_OUTBOX_POLL_MS
OPENAPI_WRITE_RECOVERY_POLL_MS
OPENAPI_STALE_PROCESSING_MINUTES
OPENAPI_MAX_REQUEST_BODY_BYTES
```

`openapi-service` 与 `fi-service` 仍需配置相同的 `MATRIX_INTERNAL_OPENAPI_TOKEN`。

## 9. 监控建议

至少监控：

- `ACCEPTED` 长时间未进入 `PROCESSING`
- `PROCESSING` 超时数量
- `RETRYING` 和 `MANUAL_REQUIRED` 数量
- Outbox `FAILED`、`DEAD` 数量
- RabbitMQ 队列堆积
- 凭证创建成功率与平均耗时

## 10. V3B 后续

后续阶段补充：

- 回调签名、回调任务与指数退避
- 每日对账与异常补偿
- API SDK 和完整接入样例
- 错误类型分级，区分业务不可重试与基础设施可重试
