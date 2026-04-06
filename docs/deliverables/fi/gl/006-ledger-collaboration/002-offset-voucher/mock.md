# 对冲凭证样例说明

## 1. 查询请求样例
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "matchStatus": "ORPHAN",
  "keyword": "RV-"
}
```

## 2. 返回样例
```json
{
  "pairCount": 10,
  "orphanCount": 2,
  "originalAmount": 320000.00,
  "reverseAmount": 300000.00,
  "warnings": [
    "存在缺原凭证或缺对冲凭证的孤儿记录，请人工复核。"
  ],
  "rows": [
    {
      "originalVoucherNo": "V202604001",
      "reverseVoucherNo": "RV202604001",
      "matchStatus": "PAIRED",
      "originalAmount": 50000.00,
      "reverseAmount": 50000.00,
      "amountDiff": 0
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "pairCount": 0,
  "orphanCount": 0,
  "originalAmount": 0,
  "reverseAmount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转查看原凭证：带 `originalVoucherNo`
- 跳转查看对冲凭证：带 `reverseVoucherNo`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
