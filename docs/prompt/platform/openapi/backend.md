# 开放平台后端开发提示词

## 目标

基于 Matrix 现有 Spring Cloud 多模块结构，实现凭证只读 OpenAPI 第一阶段。

## 输入上下文

- `docs/bus/platform/openapi/business.md`
- `docs/bus/platform/openapi/rules.md`
- `docs/bus/platform/openapi/api.md`
- `docs/bus/fi/gl/voucher/`

## 输出要求

1. 新增独立 `openapi-service` 模块。
2. 实现应用、API 定义、授权和调用日志模型。
3. 实现 AppKey + HMAC-SHA256、时间戳、Nonce、防重放、IP 白名单和限流。
4. 使用独立 OpenAPI DTO，不直接返回财务实体。
5. 通过 Feign 调用 `fi-service` 内部只读适配器。
6. 第一阶段不得开放凭证写操作。
7. 列出新增、修改文件和验证结果。
