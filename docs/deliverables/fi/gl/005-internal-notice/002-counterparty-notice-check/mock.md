# 往来通知单勾稽样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "status": "OPEN",
  "asOfDate": "2026-04-30"
}
```

## 2. 返回样例
```json
{
  "noticeCount": 12,
  "ongoingCount": 5,
  "resolvedCount": 7,
  "snapshotOpenAmount": 180000.00,
  "currentOpenAmount": 120000.00,
  "warnings": [
    "部分历史通知已自然解除，可复核后关闭。"
  ],
  "rows": [
    {
      "noticeNo": "NT20260430001",
      "status": "OPEN",
      "snapshotOpenAmount": 50000.00,
      "currentOpenAmount": 10000.00,
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
  "snapshotOpenAmount": 0,
  "currentOpenAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转到往来对账单：带当前往来方与统计日期
- 若勾稽结果显示仍存在未清余额，则带 `openOnly=true`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
