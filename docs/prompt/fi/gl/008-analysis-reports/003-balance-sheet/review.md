# 资产负债表评审提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档、当前 prompt 文档，以及已确认前端接入事实，输出资产负债表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/backend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/sql.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查资产总计、负债和所有者权益、差额、平衡状态、warnings、checks 口径是否一致
3. 检查初始化自动查询、查询、重置、下钻展示是否一致
4. 明确标注“前端已接入，后端内部实现文件本轮未定位”
5. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
