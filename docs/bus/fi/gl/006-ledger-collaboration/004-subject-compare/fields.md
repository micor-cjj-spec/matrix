# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| startDate | 开始日期 | 可空，默认当月第一天 |
| endDate | 结束日期 | 可空，默认当天 |
| accountCode | 科目编码 | 可空，支持前缀过滤 |
| diffOnly | 仅看差异 | `true / false` |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `startDate`
- `endDate`
- `accountCount`
- `diffAccountCount`
- `voucherDebitTotal`
- `glDebitTotal`
- `voucherCreditTotal`
- `glCreditTotal`
- `rows`
- `warnings`

## 对照行字段
| 字段 | 含义 |
|---|---|
| accountCode | 科目编码 |
| accountName | 科目名称 |
| voucherOpeningBalance | 凭证口径期初 |
| glOpeningBalance | 总账口径期初 |
| openingDiff | 期初差异 |
| voucherPeriodDebit | 凭证口径借方 |
| glPeriodDebit | 总账口径借方 |
| periodDebitDiff | 借方差异 |
| voucherPeriodCredit | 凭证口径贷方 |
| glPeriodCredit | 总账口径贷方 |
| periodCreditDiff | 贷方差异 |
| voucherClosingBalance | 凭证口径期末 |
| glClosingBalance | 总账口径期末 |
| closingDiff | 期末差异 |
| voucherLineCount | 凭证分录条数 |
| glEntryCount | 总账分录条数 |
| matchStatus | `MATCH / DIFF` |
| differenceReason | 差异说明 |
