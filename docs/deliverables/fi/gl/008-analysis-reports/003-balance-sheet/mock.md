# 资产负债表样例说明

## 1. 主表请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "currency": "CNY",
  "templateId": 1,
  "showZero": false
}
```

## 2. 主表返回样例
```json
{
  "warnings": ["部分项目余额异常波动，请复核。"],
  "checks": ["资产总计与负债和所有者权益总计已平衡"],
  "assetTotal": 1200000.00,
  "liabilityEquityTotal": 1200000.00,
  "diff": 0,
  "balanced": true,
  "rows": [
    {
      "itemCode": "BS_1001",
      "itemName": "货币资金",
      "beginAmount": 200000.00,
      "endAmount": 260000.00,
      "drillable": true
    }
  ]
}
```

## 3. 下钻请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "currency": "CNY",
  "templateId": 1,
  "itemId": 10,
  "itemCode": "BS_1001"
}
```

## 4. 下钻返回样例
```json
[
  {
    "accountCode": "1001",
    "accountName": "库存现金",
    "beginAmount": 50000.00,
    "endAmount": 60000.00
  }
]
```

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 本轮未定位到后端实现文件，样例应以前端已接入字段为准
- 若返回结构扩展，应同步更新本文件
