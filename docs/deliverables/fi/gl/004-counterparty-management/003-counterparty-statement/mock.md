# 往来对账单样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "counterparty": "CP001",
  "asOfDate": "2026-04-30",
  "openOnly": true
}
```

## 2. 返回样例
```json
{
  "docCount": 12,
  "counterpartyCount": 1,
  "totalAmount": 320000.00,
  "writtenOffAmount": 280000.00,
  "openAmount": 40000.00,
  "openDocCount": 3,
  "recentWriteoffCount": 2,
  "warnings": [
    "存在部分核销单据，请关注剩余未核销金额。"
  ],
  "rows": [
    {
      "docNo": "AR202604001",
      "totalAmount": 60000.00,
      "writtenOffAmount": 50000.00,
      "openAmount": 10000.00,
      "writeoffStatus": "PARTIAL",
      "latestPlanCode": "WO20260430001"
    }
  ],
  "recentLogs": [
    {
      "planCode": "WO20260430001",
      "totalAmount": 280000.00,
      "linkCount": 6
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "docCount": 0,
  "counterpartyCount": 0,
  "totalAmount": 0,
  "writtenOffAmount": 0,
  "openAmount": 0,
  "openDocCount": 0,
  "recentWriteoffCount": 0,
  "warnings": [],
  "rows": [],
  "recentLogs": []
}
```

## 4. 跳转参数样例
- 关联凭证跳转：`docNo` 或关联单据号
- 核销日志跳转：`planCode`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
