# 结转清单数据字典提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，输出结转清单场景的数据字典、检查口径、状态口径、告警口径与跳转口径，作为接口、前后端、测试的统一语义基础。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

同时结合当前已确认实现：
- 后端服务：`BizfiFiVoucherAnalysisServiceImpl.carryList`
- 前端页面：`CarryForwardListView.vue`
- 接口：`GET /voucher/carry-list`
- 页面初始化会调用 `getBusinessUnitList`

## 输出要求
1. 输出查询条件字段字典
2. 输出元信息区字段字典
3. 输出汇总卡片字段字典
4. 输出检查清单表与相关凭证表字段口径
5. 输出 warnings 与跳转参数口径
6. 对 BUS 未明确项显式标记“待确认”
