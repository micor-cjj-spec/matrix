# 科目余额表样例说明

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
    "accountCount": 18,
    "periodDebit": 1250000.00,
    "periodCredit": 1180000.00,
    "activeAccountCount": 12
  },
  "warnings": [
    "部分科目本期发生额异常，请关注。"
  ],
  "records": [
    {
      "accountCode": "1001",
      "accountName": "库存现金",
      "openingBalance": 15000.00,
      "periodDebit": 5000.00,
      "periodCredit": 2000.00,
      "closingBalance": 18000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "summary": {
    "accountCount": 0,
    "periodDebit": 0,
    "periodCredit": 0,
    "activeAccountCount": 0
  },
  "warnings": [],
  "records": []
}
```

## 4. 路由 query 样例
- `startDate=2026-04-01`
- `endDate=2026-04-30`
- `accountCode=1001`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
