# 科目余额对照样例说明

## 1. 查询请求样例
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "accountCode": "1122",
  "diffOnly": true
}
```

## 2. 返回样例
```json
{
  "accountCount": 25,
  "diffAccountCount": 3,
  "voucherDebitTotal": 520000.00,
  "glDebitTotal": 510000.00,
  "voucherCreditTotal": 520000.00,
  "glCreditTotal": 510000.00,
  "warnings": [
    "存在科目借贷差异，请优先复核异常科目。"
  ],
  "rows": [
    {
      "accountCode": "1122",
      "voucherDebit": 120000.00,
      "glDebit": 110000.00,
      "voucherCredit": 30000.00,
      "glCredit": 30000.00,
      "balanceDiff": 10000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "accountCount": 0,
  "diffAccountCount": 0,
  "voucherDebitTotal": 0,
  "glDebitTotal": 0,
  "voucherCreditTotal": 0,
  "glCreditTotal": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转到科目余额表页面：带 `accountCode`、`startDate`、`endDate`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
