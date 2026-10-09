# 资产负债表前端提示词

## 目标
结合 `docs/bus/fi/gl/008-analysis-reports/003-balance-sheet/` 下业务文档，以及当前前端已确认实现，输出资产负债表场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `page.md`
- `fields.md`
- `flow.md`
- `api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/dictionary.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/api.md`
- `docs/prompt/fi/gl/008-analysis-reports/003-balance-sheet/backend.md`

同时结合当前已确认实现：
- 页面：`BalanceSheetView.vue`
- 查询区：业务单元、期间、币种、报表模板、显示零值行
- 元信息条、汇总卡片、告警区、校验区、主表区、下钻弹窗
- 初始化时加载业务单元和模板并自动查询
- 点击“查询/重置”刷新报表
- 点击可下钻项目打开下钻弹窗

## 输出要求
1. 输出查询区、元信息条、汇总卡片、告警区、校验区、主表区、下钻弹窗交互建议
2. 说明初始化自动查询、查询、重置、showZero 展示行为
3. 说明 warnings、checks、平衡状态与下钻展示逻辑
4. 明确标注“前端已接入，后端内部实现文件本轮未定位”
5. 对 BUS 未明确项显式标记“待确认”
