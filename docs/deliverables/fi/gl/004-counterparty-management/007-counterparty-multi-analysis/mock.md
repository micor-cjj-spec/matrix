# 往来多维分析表样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "groupDimension": "COUNTERPARTY",
  "asOfDate": "2026-04-30"
}
```

## 2. 返回样例
```json
{
  "groupDimension": "COUNTERPARTY",
  "groupCount": 5,
  "totalAmount": 520000.00,
  "writtenOffAmount": 380000.00,
  "openAmount": 140000.00,
  "warnings": [
    "存在未核销余额集中在少数分组，请重点关注。"
  ],
  "rows": [
    {
      "dimensionValue": "CP001",
      "totalAmount": 200000.00,
      "writtenOffAmount": 150000.00,
      "openAmount": 50000.00
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "groupDimension": "COUNTERPARTY",
  "groupCount": 0,
  "totalAmount": 0,
  "writtenOffAmount": 0,
  "openAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
