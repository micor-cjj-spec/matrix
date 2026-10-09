# Matrix 统一消息推送平台：二期实时提醒

二期将一期的 LOCAL 站内消息升级为实时、多端、可恢复的本地提醒：WebSocket 在线推送、DELIVER_ACK、READ_ACK、Redis 在线路由和跨实例 Pub/Sub、用户单调递增版本号，以及 `/api/im/sync` 断线补拉。

数据库仍是事实来源。Redis Pub/Sub 只负责即时转发，用户离线或 Redis 暂时不可用不会使 LOCAL 渠道失败。

## 连接

`ws(s)://<gateway>/api/im/ws?access_token=<jwt>&deviceId=<device-id>&clientType=WEB`

网关仅对该 WebSocket 路径接受查询参数 token，完成 JWT 校验后移除 token 并注入可信的 `X-User-Id` 和 `X-Tenant-Id`。

## 事件

服务端持久事件：`NOTIFICATION_CREATED`、`NOTIFICATION_READ`、`NOTIFICATIONS_READ_ALL`。系统事件：`SYSTEM_CONNECTED`、`SYSTEM_PONG`。客户端事件：`SYSTEM_PING`、`DELIVER_ACK`、`READ_ACK`。

## 多实例

在线路由保存在 `im:route:{tenantId}:{userId}`，默认 TTL 90 秒；跨实例即时事件发布到 `im:push:{instanceId}`。目标实例只推送本地 Session，避免广播环路。

## 断线补拉

`GET /api/im/sync?afterVersion=12&limit=100`

客户端检测版本缺口后先补拉，再处理后续实时事件。

## 部署配置

`IM_WS_INSTANCE_ID`、`IM_WS_ROUTE_TTL_SECONDS`、`IM_WS_ALLOWED_ORIGINS`。生产环境必须限制 Origin。
