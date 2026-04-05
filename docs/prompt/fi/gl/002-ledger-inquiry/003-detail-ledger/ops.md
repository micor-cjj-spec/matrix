# 明细分类账运维提示词

## 目标
根据 `docs/bus/fi/gl/002-ledger-inquiry/003-detail-ledger/` 下业务文档，以及当前已确认实现，输出明细分类账场景的发布、运维、监控与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/sql.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/test.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/003-detail-ledger/review.md`

## 输出要求
1. 输出发布依赖检查项（前端/后端/科目选项加载依赖）
2. 输出明细流水查询、summary、warnings 相关监控关注点
3. 输出初始化自动查询与参数兼容性注意项
4. 输出回滚与异常排查建议
5. 对 BUS 未明确项显式标记“待确认”
