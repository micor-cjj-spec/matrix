# 往来自动核销样例说明

## 1. 执行请求样例
```json
{
  "docTypeRoot": "AR",
  "counterparty": "CP001",
  "asOfDate": "2026-04-30",
  "operator": "u001"
}
```

## 2. 返回样例
```json
{
  "planCode": "WO20260430001",
  "logId": 10086,
  "message": "自动核销执行成功",
  "sourceDocCount": 12,
  "targetDocCount": 8,
  "linkCount": 6,
  "totalAmount": 280000.00,
  "warnings": [
    "部分单据仅部分核销，请人工复核剩余金额。"
  ],
  "records": [
    {
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
  "planCode": "",
  "logId": null,
  "message": "未找到可执行核销方案",
  "sourceDocCount": 0,
  "targetDocCount": 0,
  "linkCount": 0,
  "totalAmount": 0,
  "warnings": [],
  "records": []
}
```

## 4. 条件带入样例
- `docTypeRoot`
- `counterparty`
- `asOfDate`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
