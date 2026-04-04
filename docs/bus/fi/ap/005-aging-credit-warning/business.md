# 账龄与信用预警业务说明

## 1. 业务名称
账龄与信用预警

## 2. 业务定位
账龄与信用预警用于对应付单据做账龄汇总、维护往来方信用配置，并输出超额度/超逾期预警结果。

## 3. 业务目标
- 查看应付账龄区间分布
- 维护供应商信用额度与逾期阈值
- 输出信用风险预警并为提交/审核拦截提供依据

## 4. 数据来源
前端 `AgingCreditView.vue` 以 `/payable/aging-credit` 路由承载；后端通过 `/arap-doc/aging/summary`、`/arap-doc/credit/warnings`、`/arap-doc/credit/config*` 接口实现。
