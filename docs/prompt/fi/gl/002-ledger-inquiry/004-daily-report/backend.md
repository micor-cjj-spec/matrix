# 日报表后端提示词

## 目标
结合 `docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/` 下业务文档，以及当前后端已确认实现，输出日报表场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/api.md`

同时结合当前已确认实现：
- 查询服务：`BizfiFiLedgerQueryServiceImpl.dailyReport`
- 基于已过账总账分录 `BizfiFiGlEntry` 按日聚合
- 返回 `records`、`summary`、`warnings`

## 输出要求
1. 说明当前后端查询能力与待补齐项
2. 输出按日借方、贷方、凭证数、滚动余额的计算逻辑建议
3. 说明 accountCode 过滤、日期区间过滤、warnings 生成逻辑
4. 说明性能、空结果与异常结果处理建议
5. 对 BUS 未明确项显式标记“待确认”
