# 流程设计

## 查询流程
1. 前端在 `/receivable/aging-credit` 打开应收账龄与信用预警页。
2. 页面按 `docTypeRoot=AR` 调用 `GET /arap-doc/aging/summary` 获取账龄汇总。
3. 同时调用 `GET /arap-doc/credit/config/list` 加载信用配置。
4. 再调用 `GET /arap-doc/credit/warnings` 生成预警列表。
5. 页面展示账龄汇总、信用配置和预警结果。

## 配置流程
1. 用户填写往来方、信用额度、逾期阈值、超限/超期拦截标志。
2. 前端调用 `POST /arap-doc/credit/config` 保存配置。
3. 保存成功后刷新配置列表和预警列表。

## 联动逻辑
- 信用配置不仅用于当前预警页展示，也用于 AR/AP 单据提交、审核前的硬拦截检查。
