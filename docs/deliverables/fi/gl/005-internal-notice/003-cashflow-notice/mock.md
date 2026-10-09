# 现金流通知单样例说明

## 1. 查询请求样例
```json
{
  "orgId": 1001,
  "period": "2026-04",
  "status": "OPEN",
  "severity": "HIGH",
  "sourceCode": "UNKNOWN_ITEM",
  "currency": "CNY"
}
```

## 2. 查询返回样例
```json
{
  "noticeCount": 12,
  "openCount": 8,
  "resolvedCount": 4,
  "highCount": 5,
  "amount": 320000.00,
  "warnings": [
    "存在高优先级未解决现金流通知，请及时处理。"
  ],
  "rows": [
    {
      "noticeNo": "CFN20260430001",
      "status": "OPEN",
      "severity": "HIGH",
      "sourceCode": "UNKNOWN_ITEM",
      "amount": 80000.00,
      "suggestion": "补录现金流项目并复核"
    }
  ]
}
```

## 3. 生成返回样例
```json
{
  "generatedCount": 10,
  "insertedCount": 6,
  "updatedCount": 3,
  "resolvedAutoCount": 1,
  "message": "现金流通知单生成完成"
}
```

## 4. 空结果样例
```json
{
  "noticeCount": 0,
  "openCount": 0,
  "resolvedCount": 0,
  "highCount": 0,
  "amount": 0,
  "warnings": [],
  "rows": []
}
```

## 5. 跳转参数样例
- 跳转凭证页：带当前通知关联的 `voucherNo`
- 跳转现金流通知单勾稽页：带当前通知单号或关联标识

## 6. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
