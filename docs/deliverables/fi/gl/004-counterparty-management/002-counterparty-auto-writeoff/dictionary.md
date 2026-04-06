# 往来自动核销口径说明

## 1. 查询与执行参数口径
- `docTypeRoot`：往来类型
- `counterparty`：往来方
- `asOfDate`：统计日期
- `operator`：操作人

## 2. 汇总统计口径
- `sourceDocCount`：源单据数
- `targetDocCount`：结算单据数
- `linkCount`：核销链接数
- `totalAmount`：核销总额
- `planCode`：方案号
- `logId`：批次日志ID
- `message`：执行成功提示

## 3. 执行结果口径
- `records` 展示预览/执行后的匹配明细
- 当前执行会将结果落库为核销日志与核销链接
- 当前页面上的预览不等于已落库，只有执行接口成功才代表核销生效

## 4. warnings 与条件带入口径
- warnings 由后端返回
- 页面可从方案页带入查询条件并自动预览

## 5. 待确认项
- 方案号生成规则是否需要更细说明
- message 与 warnings 的正式分工是否需要单独固化
