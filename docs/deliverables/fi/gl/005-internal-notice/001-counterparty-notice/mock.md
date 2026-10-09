# 往来通知单样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "status": "OPEN",
  "severity": "HIGH",
  "asOfDate": "2026-04-30"
}
```

## 2. 查询返回样例
```json
{
  "noticeCount": 12,
  "openCount": 8,
  "resolvedCount": 4,
  "highCount": 5,
  "amount": 520000.00,
  "openAmount": 140000.00,
  "warnings": [
    "存在高优先级未关闭通知，请及时处理。"
  ],
  "rows": [
    {
      "noticeNo": "NT20260430001",
      "status": "OPEN",
      "severity": "HIGH",
      "amount": 80000.00,
      "openAmount": 50000.00,
      "suggestion": "优先跟进催收"
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
  "message": "往来通知单生成完成"
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
  "openAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 5. 跳转参数样例
- 跳转对账单：带当前通知关联的往来方与统计日期
- 跳转勾稽页：带当前通知单号或关联标识

## 6. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
