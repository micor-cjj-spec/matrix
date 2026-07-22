# Matrix 开放平台规则

## 1. 请求头

- `X-Matrix-App-Key`
- `X-Matrix-Timestamp`：Unix 毫秒时间戳。
- `X-Matrix-Nonce`：单次请求随机串。
- `X-Matrix-Signature`：HMAC-SHA256 十六进制小写摘要。
- `X-Matrix-Request-Id`：可选；缺失时由平台生成。

## 2. 签名原文

按以下顺序使用换行连接：

```text
HTTP_METHOD
REQUEST_PATH
SORTED_QUERY_STRING
SHA256(REQUEST_BODY)
TIMESTAMP
NONCE
```

`REQUEST_PATH` 使用稳定的 OpenAPI 路由路径，即去掉部署层统一前缀 `/api` 后的路径。例如外部访问 `/api/open-api/v1/fi/vouchers`，签名路径固定为 `/open-api/v1/fi/vouchers`。这样更换网关域名或部署前缀时不会改变 API 契约。

查询参数按名称和值排序并进行 URL 编码。第一期只支持 GET，请求体哈希固定为空正文的 SHA-256。

## 3. 安全规则

1. 时间戳与服务端时间差不得超过 300 秒。
2. `appKey + nonce` 在 300 秒内只能使用一次。
3. 应用、API 定义、授权必须同时处于启用状态和有效期内。
4. 客户端 IP 必须命中应用白名单；空白名单表示不限制。
5. 第一阶段按应用和 API 进行秒级限流。
6. AppSecret 使用 AES-GCM 加密存储，主密钥由环境变量注入。
7. 外部传入的内部身份头必须由 Gateway/OpenAPI 服务清理或覆盖。
8. `openapi-service` 调用 `fi-service` 内部适配器时必须携带独立内部令牌；外部请求不能直接访问内部路径。

## 4. 凭证权限

- 默认允许状态：`POSTED`。
- 授权 JSON 可以设置 `allowedStatuses` 和 `maxHistoryMonths`。
- 外部请求条件与授权条件取交集；不允许时返回 `OPENAPI_40303`。
- 单页大小不能超过应用、API 定义和系统上限三者中的最小值。

## 5. 错误码

- `OPENAPI_40001`：请求参数错误。
- `OPENAPI_40101`：AppKey 不存在。
- `OPENAPI_40102`：签名错误。
- `OPENAPI_40103`：时间戳过期。
- `OPENAPI_40104`：重复请求。
- `OPENAPI_40301`：应用不可用。
- `OPENAPI_40302`：API 未授权。
- `OPENAPI_40303`：数据范围无权限。
- `OPENAPI_40304`：IP 不在白名单。
- `OPENAPI_40401`：API 不存在或未发布。
- `OPENAPI_42901`：请求频率超限。
- `OPENAPI_50001`：内部服务异常。
