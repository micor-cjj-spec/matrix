# 往来自动核销数据字典提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，输出往来自动核销场景的数据字典、执行口径、统计口径、告警口径与返回口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiArapManageServiceImpl.autoWriteoff`
- 落库对象：`BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 前端页面：`CounterpartyAutoWriteoffView.vue`
- 接口：`GET /arap-manage/plan`、`POST /arap-manage/auto-writeoff`

## 输出要求
1. 输出查询条件与执行参数字段字典
2. 输出汇总卡片、成功提示区字段字典
3. 输出执行结果记录字段字典
4. 输出方案号、日志号、核销链接数、核销总额等口径
5. 输出 warnings 与从方案页带入参数口径
6. 对 BUS 未明确项显式标记“待确认”
