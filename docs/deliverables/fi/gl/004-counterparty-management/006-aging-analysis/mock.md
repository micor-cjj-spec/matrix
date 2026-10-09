# 账龄分析表样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "asOfDate": "2026-04-30"
}
```

## 2. 返回样例
```json
{
  "counterpartyCount": 12,
  "warningCount": 3,
  "totalOpenAmount": 420000.00,
  "warnings": [
    "存在超额度与逾期往来方，请重点关注。"
  ],
  "rows": [
    {
      "counterparty": "CP001",
      "openAmount": 80000.00,
      "bucket0to30": 20000.00,
      "bucket31to60": 15000.00,
      "bucket61to90": 10000.00,
      "bucket90plus": 35000.00,
      "creditLimit": 50000.00,
      "overdueThreshold": 60,
      "riskStatus": "HIGH"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "counterpartyCount": 0,
  "warningCount": 0,
  "totalOpenAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
