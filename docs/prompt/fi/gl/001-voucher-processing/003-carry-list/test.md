# 结转清单测试提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档，以及当前前后端已确认实现，输出结转清单场景的测试与验证提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/sql.md`

## 输出要求
1. 覆盖默认期间查询、业务单元切换、查询、重置场景
2. 覆盖元信息区、汇总卡片、检查清单、相关凭证展示场景
3. 覆盖 warnings 展示与查看凭证跳转场景
4. 覆盖空结果、异常结果、业务单元下拉加载场景
5. 对 BUS 未明确项显式标记“待确认”
