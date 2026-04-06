# 现金流量表样例说明

## 1. 查询请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "currency": "CNY",
  "templateId": 1,
  "showZero": false
}
```

## 2. 返回样例
```json
{
  "templateId": 1,
  "templateName": "标准现金流量表",
  "checks": [
    {
      "code": "UNKNOWN_ITEM",
      "level": "WARN",
      "message": "存在未知现金流项目编码。"
    }
  ],
  "warnings": [
    "部分分录通过启发式分类归集，请人工复核。"
  ],
  "rows": [
    {
      "itemCode": "1001",
      "itemName": "销售商品、提供劳务收到的现金",
      "amount": 256000.00,
      "activityType": "OPERATING"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "templateId": 1,
  "templateName": "标准现金流量表",
  "checks": [],
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- “现金流量查询”跳转：沿用当前 `orgId / period / currency / templateId`
- “补充资料”跳转：沿用当前 `orgId / period / templateId`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
