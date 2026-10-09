# 往来核销日志样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "counterparty": "CP001",
  "planCode": "WO20260430001",
  "startDate": "2026-04-01",
  "endDate": "2026-04-30"
}
```

## 2. 返回样例
```json
{
  "logCount": 2,
  "linkCount": 6,
  "totalAmount": 280000.00,
  "warnings": [
    "部分批次存在部分核销记录，请人工复核。"
  ],
  "records": [
    {
      "planCode": "WO20260430001",
      "operator": "u001",
      "operateTime": "2026-04-30 10:30:00",
      "totalAmount": 280000.00,
      "linkCount": 6
    }
  ],
  "linkDetails": [
    {
      "planCode": "WO20260430001",
      "sourceDocNo": "AR202604001",
      "targetDocNo": "SK202604009",
      "writeoffAmount": 50000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "logCount": 0,
  "linkCount": 0,
  "totalAmount": 0,
  "warnings": [],
  "records": [],
  "linkDetails": []
}
```

## 4. 查看明细联动样例
- 点击批次行后，把该批次 `planCode` 带回查询并刷新 `linkDetails`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
