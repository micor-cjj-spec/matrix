# 财务月结工作台 Mock

```json
{
  "code": 200,
  "data": {
    "forg": 1,
    "period": "2026-04",
    "periodSource": "PARAM",
    "baseCurrency": "CNY",
    "periodStatus": "OPEN",
    "closeStatus": "BLOCKED",
    "readinessScore": 68,
    "canClose": false,
    "blockingCount": 2,
    "warningCount": 3,
    "passedCount": 5,
    "checkItems": [
      {
        "code": "VOUCHER_POSTING",
        "name": "期间凭证过账",
        "category": "VOUCHER",
        "status": "BLOCKED",
        "severity": "HIGH",
        "message": "当前期间仍有 3 张凭证未过账。",
        "actionHint": "先处理未过账凭证，再推进关账。",
        "routePath": "/ledger/voucher",
        "relatedCount": 3,
        "blocking": true
      }
    ]
  }
}
```

