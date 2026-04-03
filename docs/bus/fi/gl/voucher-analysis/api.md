# 接口说明

## 汇总分析接口
- `GET /voucher/summary`
- 输入：`startDate`、`endDate`、`status`、`summaryKeyword`
- 输出：凭证汇总结果、分日期汇总行、警告信息

## 结转清单接口
- `GET /voucher/carry-list`
- 输入：`forg`、`period`
- 输出：任务清单、相关凭证、警告信息、本位币、当前期间、默认凭证字等

## 说明
- 这两个接口都挂在现有凭证控制器下
- 分析接口不直接修改业务数据
