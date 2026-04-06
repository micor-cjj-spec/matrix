# 往来自动核销接口提示词

## 目标
根据 `docs/bus/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/` 下业务文档，输出往来自动核销场景的接口契约提示词，为前后端联调、测试与评审提供统一接口口径。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/004-counterparty-management/002-counterparty-auto-writeoff/dictionary.md`

同时结合当前已确认实现：
- 预览接口：`GET /arap-manage/plan`
- 执行接口：`POST /arap-manage/auto-writeoff`
- 返回 `Map<String,Object>`
- 前端 API：`arapManageApi.executeAutoWriteoff`
- 页面：`CounterpartyAutoWriteoffView.vue`

## 输出要求
1. 输出预览接口与执行接口契约
2. 输出执行参数、返回结构、warnings 与统计字段口径
3. 输出 `records` 的结构与执行结果统计口径说明
4. 输出从方案页带入参数与执行成功提示区字段映射说明
5. 对 BUS 未明确项显式标记“待确认”
