# 往来核销方案样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "counterparty": "CP001",
  "asOfDate": "2026-04-30",
  "auditedOnly": true
}
```

## 2. 返回样例
```json
{
  "sourceDocCount": 12,
  "targetDocCount": 8,
  "counterpartyCount": 1,
  "sourceOpenAmount": 320000.00,
  "targetOpenAmount": 300000.00,
  "suggestedAmount": 280000.00,
  "planCount": 6,
  "remainingSourceAmount": 40000.00,
  "remainingTargetAmount": 20000.00,
  "warnings": [
    "部分单据仅能部分核销，请人工确认。"
  ],
  "records": [
    {
      "sourceDocNo": "AR202604001",
      "targetDocNo": "SK202604009",
      "suggestedAmount": 50000.00,
      "remainingSourceAmount": 10000.00,
      "remainingTargetAmount": 0
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "sourceDocCount": 0,
  "targetDocCount": 0,
  "counterpartyCount": 0,
  "sourceOpenAmount": 0,
  "targetOpenAmount": 0,
  "suggestedAmount": 0,
  "planCount": 0,
  "remainingSourceAmount": 0,
  "remainingTargetAmount": 0,
  "warnings": [],
  "records": []
}
```

## 4. 跳转参数样例
- `docTypeRoot`
- `counterparty`
- `asOfDate`
- `auditedOnly`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
