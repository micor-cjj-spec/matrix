# 资产负债表样例提示词

## 目标
根据 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档，以及当前已确认前端接入事实，输出资产负债表场景的样例数据与 mock 提示词。

## 输入上下文
请结合以下文档：
- `fields.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/frontend.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/test.md`

## 输出要求
1. 输出默认查询样例、汇总卡片样例、warnings/checks 样例、主表样例、下钻样例
2. 输出不同 showZero、空结果、异常结果等样例
3. 输出主表和下钻接口参数样例
4. 明确标注“后端内部实现文件本轮未定位”并对未明确项标记“待确认”
