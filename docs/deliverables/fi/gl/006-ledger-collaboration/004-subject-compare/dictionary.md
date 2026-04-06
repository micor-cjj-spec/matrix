# 科目余额对照口径说明

## 1. 查询条件口径
- `startDate`：开始日期
- `endDate`：结束日期
- `accountCode`：科目编码
- `diffOnly`：仅看差异

## 2. 汇总统计口径
- `accountCount`：科目数
- `diffAccountCount`：差异科目数
- `voucherDebitTotal`：凭证借方合计
- `glDebitTotal`：总账借方合计
- `voucherCreditTotal`：凭证贷方合计
- `glCreditTotal`：总账贷方合计

## 3. 科目对照结果口径
- `rows`：按科目展示凭证分录口径与总账分录口径的对照结果
- 支持展示借方、贷方、余额、期初、发生、期末等差异信息
- `diffOnly` 用于只展示存在差异的科目

## 4. warnings 与跳转口径
- warnings 由后端返回
- 行内可跳转到科目余额表页面

## 5. 待确认项
- 借贷差值与余额差额的最终展示字段是否需要独立文档化
- 差异口径是否需要区分期初、发生和期末三个层次单独说明
