# 单据下推反写 API

## 查询规则

`GET /api/botp/rules`

返回所有可用规则摘要。

`GET /api/botp/rules/{ruleCode}`

返回规则定义和当前版本。

## 预览转换

`POST /api/botp/executions/preview`

```json
{
  "requestId": "ERP-20260722-000001",
  "sourceSystem": "ERP",
  "tenantId": "default",
  "ruleCode": "DEMO_ORDER_TO_DELIVERY",
  "sourceDocuments": [
    {
      "systemCode": "DEMO",
      "documentType": "DEMO_ORDER",
      "documentId": "ORDER-001",
      "entryIds": []
    }
  ],
  "parameters": {
    "operatorId": "admin"
  },
  "executionMode": "SYNC"
}
```

预览只返回目标草稿，不创建目标单。

## 执行下推

`POST /api/botp/executions`

请求结构与预览一致。同步模式返回最终执行结果；异步模式在持久化和消息能力完成前暂不开放。

## 查询执行结果

`GET /api/botp/executions/{executionId}`

返回状态、目标单列表和错误信息。

## 错误约定

- 400：参数或映射校验失败。
- 404：规则、执行任务或适配器不存在。
- 409：规则状态不允许执行或幂等冲突。
- 500：适配器或目标领域服务异常。

## 外部系统安全预留

正式开放接口时必须增加 AppKey/AppSecret、时间戳、Nonce、签名、租户授权、限流和回调白名单。V1 内核不在 Controller 中硬编码安全实现。
