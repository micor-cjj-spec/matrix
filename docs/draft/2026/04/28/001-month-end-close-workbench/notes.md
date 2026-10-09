# 人工修订说明

- 本次需求确认必须遵守 `draft -> bus -> prompt -> code + deliverables` 的执行链路。
- 首版工作台先做关账前检查与月结准备度，不直接执行关账，避免缺少审批、审计和反结账规则时过早引入写操作。
- 场景落点归入总账域 `fi/gl/007-period-end-processing/007-month-end-close-workbench`。
- 前端落点归入 `matrix-web` 总账期末处理菜单；后端落点归入 `matrix` 的 `fi-service` 期末处理服务。

