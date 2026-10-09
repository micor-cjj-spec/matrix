# Matrix 统一消息推送平台：三期业务闭环

三期不新增前端页面，重点完善多业务系统正式接入所需的后端闭环。

## 本期能力

1. **可靠性加固**
   - Outbox `PROCESSING` 超时恢复。
   - RabbitMQ 渠道队列配置死信交换机和 DLQ。
   - Broker 异常不再无限 requeue，死信持久化到 `im_dead_letter`。
   - 定时对账消息聚合状态，以及 LOCAL 渠道成功但站内消息缺失的异常。

2. **动态应用接入**
   - `im_application` 保存租户、允许渠道、IP 白名单、限流、回调地址和状态。
   - AppSecret 与 CallbackSecret 使用 AES-256-GCM 加密保存。
   - 支持应用新增/更新、密钥轮换和只读列表。
   - 保留配置文件凭证作为迁移期兼容方案。

3. **并发幂等**
   - 对 `(app_code, request_id)` 并发唯一键冲突进行事务外查询。
   - 重复请求返回原 `messageNo`，并标记 `idempotentReplay=true`。

4. **最终结果回调**
   - 最终消息状态生成 `im_callback_task`。
   - 回调使用 `X-IM-Timestamp`、`X-IM-Nonce`、`X-IM-Signature` 和 HMAC-SHA256。
   - 支持指数退避、超时恢复、最终 DEAD 状态。
   - 回调地址必须与应用登记地址一致，防止任意 URL 回调。

5. **Scheduler 真实接入**
   - Scheduler 告警和 `matrix_scheduler_im_outbox` 在同一事务内创建。
   - 异步 HMAC 调用 IM，不影响调度告警主事务。
   - IM 最终状态回调 Scheduler，形成 `告警 → IM → LOCAL/EMAIL → 回调` 闭环。

## 部署顺序

1. 执行 `docs/sql/im-platform-phase3.sql`。
2. 执行 `scheduler-service/src/main/resources/db/migration/V4__scheduler_im_notification_outbox.sql`。
3. 配置独立的 `IM_SECRET_MASTER_KEY`。
4. 调用 `POST /api/im/applications` 创建 `scheduler` 应用，并保存只返回一次的密钥。
5. 将应用密钥、回调地址和接收用户配置到 Scheduler。
6. 设置 `SCHEDULER_IM_ENABLED=true`。
