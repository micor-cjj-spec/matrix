# 日报表样例说明

## 1. 查询请求样例
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "accountCode": "1001"
}
```

## 2. 返回样例
```json
{
  "summary": {
    "dayRowCount": 30,
    "periodDebit": 1250000.00,
    "periodCredit": 1180000.00,
    "voucherCount": 86
  },
  "warnings": [
    "2026-04-15 借贷波动较大，请关注。"
  ],
  "records": [
    {
      "reportDate": "2026-04-15",
      "dayDebit": 250000.00,
      "dayCredit": 180000.00,
      "voucherCount": 8,
      "balanceDirection": "DEBIT",
      "balance": 380000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "summary": {
    "dayRowCount": 0,
    "periodDebit": 0,
    "periodCredit": 0,
    "voucherCount": 0
  },
  "warnings": [],
  "records": []
}
```

## 4. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
