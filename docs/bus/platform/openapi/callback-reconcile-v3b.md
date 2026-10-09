# OpenAPI 第三期 B：回调与对账闭环

## 目标

外部凭证草稿写入进入终态后，Matrix 主动推送结果；回调、写入任务、财务凭证和 Outbox 之间出现不一致时，系统自动发现并进入异常处理台。

## 回调配置

回调地址固定配置在外部应用上，不接受单次写入请求覆盖，避免将 OpenAPI 服务变成任意 URL 请求代理。

约束：

- 默认仅允许 HTTPS。
- 禁止 localhost、回环、链路本地、站点本地和组播地址。
- 不跟随 HTTP 重定向。
- 应用可以暂停回调；已生成但尚未发送的任务将标记为 `SKIPPED`。

## 回调生成

终态扫描器每 5 秒检查最近写入任务，并为以下状态补建唯一回调任务：

- `SUCCEEDED` → `VOUCHER_WRITE_SUCCEEDED`
- `MANUAL_REQUIRED` → `VOUCHER_WRITE_MANUAL_REQUIRED`

唯一键为 `write_request_id + event_type`。服务在状态提交后宕机也不会丢失回调任务。

## 回调签名

请求头：

- `X-Matrix-Callback-Event-Id`
- `X-Matrix-Timestamp`
- `X-Matrix-Nonce`
- `X-Matrix-Signature`

规范串：

```text
{eventId}\n{timestamp}\n{nonce}\n{SHA256(rawBody)}
```

使用应用当前 AppSecret 执行 HMAC-SHA256。接收方应以事件 ID 做幂等，并校验时间戳、Nonce 和签名。

## 回调重试

失败退避：1 分钟、5 分钟、15 分钟、1 小时、3 小时、6 小时。达到最大次数后进入 `DEAD`，管理员可在异常处理台重新激活。

## 每日对账

默认每天 02:30 扫描最近 7 天数据，最长可人工指定 90 天。检查：

1. `SUCCEEDED` 任务没有对应财务凭证。
2. 非成功任务已经存在财务凭证。
3. 任务凭证 ID 与财务实际凭证 ID 不一致。
4. Outbox 长时间处于 `PENDING/SENDING/FAILED/DEAD`。
5. 回调进入 `DEAD`。
6. 财务服务核验调用失败。

## 自动修复边界

允许自动或管理员触发修复：

- 财务凭证存在但任务未成功：按财务结果将任务修复为成功。
- Outbox 卡死：重新激活投递。
- 回调死信：重新激活回调任务。

不自动修改：

- 成功任务对应凭证缺失。
- 凭证 ID 不一致。

这些问题必须人工核查后关闭，避免错误创建或覆盖财务数据。

## 管理接口

```text
PUT  /api/openapi/admin/apps/{id}/callback
GET  /api/openapi/admin/callbacks
POST /api/openapi/admin/callbacks/{eventId}/retry
GET  /api/openapi/admin/reconciliation
POST /api/openapi/admin/reconciliation/run
POST /api/openapi/admin/reconciliation/{recordId}/repair
POST /api/openapi/admin/reconciliation/{recordId}/resolve
```

## 数据库

在 V1、V2、V3 后执行：

```bash
mysql < sql/matrix_open_api_v4.sql
```
