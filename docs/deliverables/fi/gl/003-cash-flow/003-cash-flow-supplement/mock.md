# 现金流量补充资料样例说明

## 1. 查询请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "currency": "CNY"
}
```

## 2. 返回样例
```json
{
  "postedVoucherCount": 120,
  "cashVoucherCount": 34,
  "cashAccountCount": 6,
  "cashflowItemCount": 18,
  "directCount": 10,
  "heuristicCount": 12,
  "unknownCount": 4,
  "mixedCount": 3,
  "transferCount": 5,
  "cashInAmount": 560000.00,
  "cashOutAmount": 430000.00,
  "netAmount": 130000.00,
  "warnings": [
    "存在未知编码和多编码凭证，请人工复核。"
  ],
  "tasks": [
    {
      "taskCode": "UNKNOWN_ITEM",
      "taskName": "未知编码复核",
      "count": 4
    }
  ],
  "categories": [
    {
      "categoryCode": "OPERATING",
      "count": 20,
      "amount": 320000.00
    }
  ],
  "pendingVouchers": [
    {
      "voucherNo": "V2026040012",
      "sourceType": "HEURISTIC",
      "amount": 12000.00,
      "summary": "差旅报销付款"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "postedVoucherCount": 0,
  "cashVoucherCount": 0,
  "cashAccountCount": 0,
  "cashflowItemCount": 0,
  "directCount": 0,
  "heuristicCount": 0,
  "unknownCount": 0,
  "mixedCount": 0,
  "transferCount": 0,
  "cashInAmount": 0,
  "cashOutAmount": 0,
  "netAmount": 0,
  "warnings": [],
  "tasks": [],
  "categories": [],
  "pendingVouchers": []
}
```

## 4. 查看凭证跳转参数样例
- 凭证号：来自当前行 `voucherNo`
- 跳转目标：凭证页

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
