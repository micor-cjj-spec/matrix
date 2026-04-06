# 凭证折算规则样例说明

## 1. 查询请求样例
```json
{
  "docTypeRoot": "AR",
  "keyword": "invoice"
}
```

## 2. 返回样例
```json
{
  "ruleCount": 8,
  "auditedCount": 120,
  "generatedCount": 108,
  "pendingCount": 12,
  "warnings": [
    "存在部分规则待生成量较高，请关注积压。"
  ],
  "rows": [
    {
      "docType": "AR_INVOICE",
      "debitAccount": "1122",
      "creditAccount": "6001",
      "coverageRate": 0.90,
      "latestDocDate": "2026-04-30"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "ruleCount": 0,
  "auditedCount": 0,
  "generatedCount": 0,
  "pendingCount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转到应收业务单据页：带 `docTypeRoot=AR` 与当前行相关单据类型
- 跳转到应付业务单据页：带 `docTypeRoot=AP` 与当前行相关单据类型

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
