# Matrix OpenAPI 治理 V2

## 1. 本期目标

将第一期凭证只读接口升级为可运营、可排查、可控制的开放平台：

- 应用配置维护与 AppSecret 轮换。
- API 授权修改、停用和撤销。
- 调用日志分页查询、Request ID 定位。
- 24 小时/自定义窗口调用看板。
- 凭证租户、组织、账簿 SQL 级数据权限。

## 2. 数据权限

应用固定绑定一个 `tenantId`，外部请求不能覆盖租户。

授权 JSON：

```json
{
  "allowedStatuses": ["POSTED"],
  "organizationIds": ["ORG-001", "ORG-002"],
  "bookIds": ["BOOK-001"],
  "maxHistoryMonths": 24
}
```

规则：

1. 最终范围等于请求条件与授权范围的交集。
2. 租户、组织、账簿、状态条件由 `fi-service` 写入 SQL。
3. `organizationIds` 或 `bookIds` 缺失时，为兼容 V1 授权，按应用租户内通配处理。
4. 新建和修改授权时，前台应显式提交组织与账簿范围。
5. 系统最大凭证状态仍固定为 `POSTED`，授权不能扩大为草稿、提交或审核状态。

## 3. 管理接口

- `PUT /api/openapi/admin/apps/{id}`：维护应用名称、有效期、IP、QPS、分页上限。
- `POST /api/openapi/admin/apps/{id}/rotate-secret`：立即轮换 AppSecret，明文仅返回一次。
- `PUT /api/openapi/admin/grants/{id}`：修改授权。
- `DELETE /api/openapi/admin/grants/{id}`：将授权标记为 `REVOKED`。
- `GET /api/openapi/admin/logs`：分页检索调用日志。
- `GET /api/openapi/admin/logs/{requestId}`：按 Request ID 查询调用详情。
- `GET /api/openapi/admin/dashboard`：查询调用量、成功率、平均耗时、P95、热门 API 和错误码。

## 4. 日志与看板

日志支持以下过滤条件：

- Request ID
- App ID
- API 编码
- 客户端 IP
- 成功/失败
- 错误码
- 开始与结束时间

看板单次最多聚合最近 10000 条调用记录，达到上限时返回 `sampleTruncated=true`，提示管理端缩小时间窗口。

## 5. 数据库迁移

按顺序执行：

1. `sql/matrix_open_api_v1.sql`
2. `sql/matrix_open_api_v2.sql`

V2 会给凭证增加 `tenant_id`、`org_id`、`book_id`，并增加开放查询所需的组合索引。

## 6. 安全边界

- AppSecret 轮换后旧密钥立即失效；接入方应先停流或具备快速配置切换能力。
- 管理接口继续经过 Gateway Bearer JWT 认证。
- 外部 OpenAPI 仍只支持 GET，不开放凭证写入、审核、过账和冲销。
