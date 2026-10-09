# 结转清单样例说明

## 1. 查询请求样例
```json
{
  "forg": 1001,
  "period": "2026-04"
}
```

## 2. 返回样例
```json
{
  "period": "2026-04",
  "periodSource": "CURRENT",
  "baseCurrency": "CNY",
  "currentPeriod": "2026-04",
  "periodStatus": "OPEN",
  "defaultVoucherType": "记",
  "foundationHealthy": true,
  "periodVoucherCount": 128,
  "carryVoucherCount": 6,
  "periodVoucherAmount": 3256000.00,
  "tasks": [
    {
      "taskCode": "CHECK_DEFAULT_VOUCHER_TYPE",
      "taskName": "默认凭证字检查",
      "status": "PASS"
    }
  ],
  "relatedVouchers": [
    {
      "voucherNo": "V2026040001",
      "voucherDate": "2026-04-30",
      "status": "AUDITED"
    }
  ],
  "warnings": [
    "当前期间仍存在未过账相关凭证，请复核。"
  ]
}
```

## 3. 空结果样例
```json
{
  "tasks": [],
  "relatedVouchers": [],
  "warnings": []
}
```

## 4. 跳转参数样例
- 凭证号：来自相关凭证行的 `voucherNo`
- 跳转目标：`/ledger/voucher`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
