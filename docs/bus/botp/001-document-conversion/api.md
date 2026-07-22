# 单据下推反写 API

## 查询规则

`GET /api/botp/rules`

返回规则列表。若某规则存在未发布草稿，则列表优先返回草稿；否则返回最新已发布版本。

`GET /api/botp/rules/{ruleCode}`

返回规则当前草稿或最新已发布版本。

`GET /api/botp/rules/{ruleCode}/versions`

返回所有已发布的不可变版本。

## 保存规则草稿

`POST /api/botp/rules`

新建或保存规则草稿。

`PUT /api/botp/rules/{ruleCode}`

更新规则草稿。路径编码必须与请求体 `ruleCode` 一致。若已有已发布版本，则草稿版本自动使用最新已发布版本号加一。

```json
{
  "ruleCode": "DEMO_ORDER_TO_DELIVERY",
  "ruleName": "演示订单下推发货单",
  "sourceSystemCode": "DEMO",
  "sourceDocumentType": "DEMO_ORDER",
  "targetSystemCode": "DEMO",
  "targetDocumentType": "DEMO_DELIVERY",
  "headerMappings": [
    {
      "sourceType": "SOURCE_FIELD",
      "sourcePath": "orderNo",
      "targetPath": "sourceOrderNo",
      "constantValue": null,
      "required": true
    }
  ],
  "entryMappings": [],
  "writebackMappings": []
}
```

## 发布规则版本

`POST /api/botp/rules/{ruleCode}/publish`

将当前草稿发布为不可变版本。没有草稿时返回业务错误。

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

返回状态、规则版本、目标单列表和错误信息。

## 错误约定

- 400：参数、规则状态或映射校验失败。
- 404：规则、执行任务或适配器不存在。
- 409：幂等冲突或并发状态冲突。
- 500：适配器或目标领域服务异常。

## 外部系统安全预留

正式开放接口时必须增加 AppKey/AppSecret、时间戳、Nonce、签名、租户授权、限流和回调白名单。V1 内核不在 Controller 中硬编码安全实现。
