# 接口说明

## 结果字段
以下接口返回结构化映射缺口：
- `GET /profit-statement`
- `GET /balance-sheet`
- `GET /analysis-report/enterprise-tax`

## 字段
- `mappingGaps`：映射缺口数组。

## 示例
```json
{
  "reportType": "PROFIT_STATEMENT",
  "accountCode": "1001",
  "accountName": "库存现金",
  "mappingType": "PL",
  "targetRoute": "/ledger/report-account-map?accountCode=1001&reportType=PROFIT_STATEMENT"
}
```
