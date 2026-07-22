# Matrix 统一消息推送平台：一期实现

## 1. 一期目标

一期将 `im-service` 定位为统一通知平台，而不是完整聊天系统。当前实现两种渠道：

- `LOCAL`：站内消息落库、未读计数、消息列表、单条已读和全部已读。
- `EMAIL`：通过 Spring Mail/SMTP 发送 HTML 邮件。

Matrix 内部服务和外部业务平台均通过统一开放接口接入。每个调用方使用 `appCode + requestId` 做业务幂等。

## 2. 可靠发送链路

```text
调用方
  -> HMAC-SHA256开放接口鉴权
  -> im_message_task / recipient / channel_task / outbox 同事务落库
  -> Outbox定时发布RabbitMQ
  -> 渠道消费者CAS抢占任务
  -> LOCAL或EMAIL执行器
  -> 渠道状态更新
  -> 汇总消息状态
```

开放接口返回 `ACCEPTED` 仅表示平台已经可靠落库，不代表所有渠道已经成功。

## 3. 状态模型

消息主任务：

- `ACCEPTED`
- `PROCESSING`
- `SUCCESS`
- `PARTIAL_SUCCESS`
- `FAILED`
- `UNKNOWN`
- `CANCELLED`
- `EXPIRED`

渠道任务：

- `PENDING`
- `PROCESSING`
- `SUCCESS`
- `RETRYING`
- `DEAD`
- `UNKNOWN`
- `CANCELLED`
- `EXPIRED`

邮件任务进入 `PROCESSING` 后若进程异常中断，平台不会自动重复发送，而是将任务标记为 `UNKNOWN`，避免普通 SMTP 无幂等能力时重复发信。本地提醒具有数据库唯一键，可以安全重试。

## 4. 开放接口鉴权

请求头：

```text
X-App-Code
X-Timestamp
X-Nonce
X-Signature
```

签名原文：

```text
HTTP_METHOD + "\n" +
REQUEST_URI + "\n" +
TIMESTAMP + "\n" +
NONCE + "\n" +
SHA256_HEX(REQUEST_BODY)
```

签名值：

```text
HEX(HMAC_SHA256(APP_SECRET, CANONICAL_TEXT))
```

Nonce 通过 Redis 防重放，默认签名有效窗口为 300 秒。应用密钥通过 Nacos 或环境变量配置，禁止写入仓库。

## 5. API

### 5.1 直接发送

`POST /api/open-api/v1/messages/send`

```json
{
  "requestId": "matrix-task-10001-1",
  "messageType": "TASK_FAILED",
  "title": "任务执行失败",
  "content": "日报任务执行失败",
  "channels": ["LOCAL", "EMAIL"],
  "recipients": [
    {
      "userId": "10001",
      "receiverName": "Micor",
      "email": "user@example.com"
    }
  ],
  "business": {
    "type": "TASK",
    "id": "10001",
    "actionUrl": "/task/10001"
  }
}
```

### 5.2 模板发送

`POST /api/open-api/v1/messages/template-send`

```json
{
  "requestId": "matrix-task-10001-2",
  "templateCode": "TASK_EXECUTION_FAILED",
  "templateParams": {
    "taskName": "每日报表",
    "errorMessage": "数据源连接超时"
  },
  "recipients": [
    {
      "userId": "10001",
      "email": "user@example.com"
    }
  ]
}
```

### 5.3 查询发送状态

`GET /api/open-api/v1/messages/{messageNo}`

### 5.4 模板管理

- `POST /api/im/templates`
- `GET /api/im/templates`

模板管理接口由 Matrix 网关用户鉴权保护。

### 5.5 本地通知

- `GET /api/im/notifications`
- `GET /api/im/notifications/unread-count`
- `POST /api/im/notifications/{notificationId}/read`
- `POST /api/im/notifications/read-all`

网关需要向下游传递可信的 `X-User-Id`，客户端不得自行伪造。

## 6. 配置项

```text
IM_MATRIX_APP_SECRET
RABBITMQ_HOST / RABBITMQ_PORT / RABBITMQ_USERNAME / RABBITMQ_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD
IM_EMAIL_FROM
```

生产环境必须替换默认应用密钥，并将 SMTP、Redis、RabbitMQ 和数据库配置放入 Nacos 或密钥管理服务。

## 7. 后续迭代

下一阶段按以下顺序推进：

1. WebSocket 在线弹窗及客户端 ACK。
2. 应用接入管理表、密钥轮换、IP 白名单和调用限流。
3. 回调任务、回调重试和主动查询补偿。
4. 用户通知偏好、免打扰时间和紧急消息策略。
5. 管理端消息任务、失败任务和人工重放页面。
