# 凭证折算规则交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/`
- 业务名称：凭证折算规则
- 业务定位：可视化展示系统当前内置的往来单据转凭证映射规则，并检查单据生成覆盖率与待生成量。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/review.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/ops.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/001-voucher-conversion-rule/mock.md`

## 3. 交付物清单
- `dictionary.md`：规则字段与统计口径说明
- `api.md`：凭证折算规则接口正式说明
- `sql.md`：查询与覆盖率说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确规则数、已审核单据、已生成凭证、待生成、覆盖率口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现凭证折算规则页面与查询接口的场景
- 适用于规则展示、覆盖率分析与待生成积压排查

## 6. 风险与注意事项
- 当前结果依赖内置 `RuleSpec` 与 AR/AP 单据实际生成情况
- 规则数、已审核单据、已生成凭证、待生成、覆盖率、最近单据日期口径必须与后端实现一致
- 跳转到应收/应付业务单据页面的参数映射必须保持一致
- 规则配置依赖需保持可用

## 7. 回滚与兜底
- 若规则展示或覆盖率口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留规则查询能力，降级外部联动能力
