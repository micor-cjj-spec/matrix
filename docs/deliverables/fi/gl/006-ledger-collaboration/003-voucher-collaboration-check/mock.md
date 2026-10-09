# 凭证协同检查样例说明

## 1. 查询请求样例
```json
{
  "startDate": "2026-04-01",
  "endDate": "2026-04-30",
  "issueCode": "ENTRY_MISSING",
  "severity": "HIGH",
  "status": "POSTED",
  "onlyIssue": true
}
```

## 2. 返回样例
```json
{
  "voucherCount": 120,
  "issueCount": 15,
  "issueVoucherCount": 10,
  "highCount": 4,
  "healthyCount": 110,
  "warnings": [
    "存在高风险凭证协同异常，请优先处理。"
  ],
  "rows": [
    {
      "voucherNo": "V202604001",
      "issueCode": "ENTRY_MISSING",
      "severity": "HIGH",
      "status": "POSTED",
      "message": "缺少总账分录"
    }
  ]
}
```

## 3. 空结果样例
```json
{
  "voucherCount": 0,
  "issueCount": 0,
  "issueVoucherCount": 0,
  "highCount": 0,
  "healthyCount": 0,
  "warnings": [],
  "rows": []
}
```

## 4. 跳转参数样例
- 跳转查看凭证：带 `voucherNo`

## 5. 说明
- 当前样例用于联调、页面展示与测试验证
- 若返回结构扩展，应同步更新本文件
