# 月结工作台后端提示词

## BUS 来源

`docs/bus/fi/gl/007-period-end-processing/007-month-end-close-workbench/`

## 生成目标

在 `fi-service` 的期末处理服务中新增关账前检查接口。

## 输入上下文

- Controller：`BizfiFiPeriodProcessController`
- Service：`BizfiFiPeriodProcessService`
- ServiceImpl：`BizfiFiPeriodProcessServiceImpl`
- 现有期末模块方法：`profitLoss`、`autoTransfer`、`fxRevalue`、`voucherAmortization`、`closeBooks`
- 现有基础资料健康检查：`BizfiFiDataHealthCheckService`
- 现有报表服务：资产负债表、利润表、现金流量表

## 生成约束

1. 新增 `monthEndWorkbench(Long forg, String period)`。
2. 新增只读接口 `GET /period-process/month-end-workbench`。
3. 新增 VO：`MonthEndWorkbenchResultVO`、`MonthEndCheckItemVO`、`MonthEndStepVO`。
4. 不修改会计期间状态，不新增写接口。
5. 检查项必须包含基础资料、会计期间、凭证过账、总账平衡、期末模块、报表生成、关账判断。

## 验收标准

- 接口返回关账准备状态、准备度分数、检查项和步骤列表。
- 当存在未过账凭证或期间缺失时，返回阻塞项。
- 当前期间已关闭时，返回 `CLOSED` 复核状态。

