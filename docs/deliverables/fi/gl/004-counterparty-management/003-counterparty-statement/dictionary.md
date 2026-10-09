# 往来对账单口径说明

## 1. 查询条件口径
- `docTypeRoot`：往来类型
- `counterparty`：往来方
- `asOfDate`：统计日期
- `openOnly`：仅看未核销

## 2. 汇总统计口径
- `docCount`：单据数
- `counterpartyCount`：往来方数
- `totalAmount`：单据原额
- `writtenOffAmount`：已核销金额
- `openAmount`：未核销金额
- `openDocCount`：未核销单据数
- `recentWriteoffCount`：最近核销批次数
- `核销率`：已核销金额 / 单据原额

## 3. 页面区域口径
- `rows`：对账明细表，按单据展示原额、已核销、未核销、核销状态等信息
- `recentLogs`：最近自动核销日志摘要
- `warnings`：需要重点关注的提示信息

## 4. 跳转口径
- 对账明细可跳转关联凭证
- 最近核销批次可跳转核销日志并带入方案号

## 5. 待确认项
- 核销状态的最终枚举口径是否需要更细说明
- warnings 的正式触发规则是否需要单独固化
