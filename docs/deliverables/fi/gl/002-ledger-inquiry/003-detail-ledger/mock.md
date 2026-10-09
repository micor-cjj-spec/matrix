# 明细分类账样例说明

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
    "rowCount": 52,
    "debitTotal": 1250000.00,
    "creditTotal": 1180000.00,
    "accountCount": 8
  },
  "warnings": [
    "部分明细流水余额波动较大，请关注。"
  ],
  "records": [
    {
      "accountCode": "1001",
      "accountName": "库存现金",
      "voucherDate": "2026-04-06",
      "voucherNo": "V2026040008",
      "summary": "报销付款",
      "debit": 0,
      "credit": 2000.00,
      "balanceDirection": "DEBIT",
      "balance": 18000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "summary": {
    "rowCount": 0,
    "debitTotal": 0,
    "creditTotal": 0,
    "accountCount": 0
  },
  "warnings": [],
  "records": []
}
```

## 4. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
