# 凭证汇总表样例说明

## 1. 查询请求样例
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-06",
  "status": "AUDITED",
  "summaryKeyword": "差旅"
}
```

## 2. 返回样例
```json
{
  "totalCount": 12,
  "totalAmount": 256000.00,
  "draftCount": 2,
  "submittedCount": 1,
  "auditedCount": 4,
  "postedCount": 4,
  "rejectedCount": 1,
  "reversedCount": 0,
  "postedAmount": 120000.00,
  "warnings": [
    "2026-04-05 待审核凭证较多，请及时处理"
  ],
  "rows": [
    {
      "voucherDate": "2026-04-05",
      "count": 5,
      "amount": 86000.00,
      "auditedCount": 2,
      "postedCount": 1
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "totalCount": 0,
  "totalAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 日期：当天日期
- 状态：当前行或当前查询状态
- 摘要关键字：沿用当前查询输入

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若后端返回结构扩展，应同步更新本文件
