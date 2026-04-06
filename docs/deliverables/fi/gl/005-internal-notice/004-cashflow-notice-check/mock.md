# 现金流通知单勾稽样例说明

## 1. 查询请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "status": "OPEN",
  "currency": "CNY"
}
```

## 2. 返回样例
```json
{
  "noticeCount": 12,
  "ongoingCount": 5,
  "resolvedCount": 7,
  "snapshotAmount": 180000.00,
  "currentAmount": 120000.00,
  "warnings": [
    "部分历史现金流通知已自然解除，可复核后关闭。"
  ],
  "rows": [
    {
      "noticeNo": "CFN20260430001",
      "status": "OPEN",
      "snapshotAmount": 50000.00,
      "currentAmount": 10000.00,
      "reconcileStatus": "ONGOING"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "noticeCount": 0,
  "ongoingCount": 0,
  "resolvedCount": 0,
  "snapshotAmount": 0,
  "currentAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转到凭证页：带当前通知关联的 `voucherNo`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
