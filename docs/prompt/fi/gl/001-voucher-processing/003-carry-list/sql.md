# 结转清单SQL提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前已确认实现，输出结转清单场景的 SQL 与查询设计提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`

同时结合当前已确认实现：
- 需要汇总组织财务参数、期间、默认凭证字、基础资料健康度和相关凭证
- 页面只做检查与导航，不直接执行结转

## 输出要求
1. 输出组织、期间、默认凭证字、相关凭证聚合查询口径
2. 输出 `tasks`、`relatedVouchers` 所需查询与关联建议
3. 输出业务单元、期间、凭证号相关索引建议
4. 输出 warnings 与健康检查相关数据来源建议
5. 对 BUS 未明确项显式标记“待确认”
