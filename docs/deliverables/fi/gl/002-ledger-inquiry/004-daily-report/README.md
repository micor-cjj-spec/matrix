# 日报表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/002-ledger-inquiry/004-daily-report/`
- 业务名称：日报表
- 业务定位：总账账表查询中的日级统计报表，用于按日期查看期间内凭证发生和滚动余额变化。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/sql.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/test.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/review.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/ops.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/004-daily-report/mock.md`

## 3. 交付物清单
- `dictionary.md`：日报表字段与统计口径说明
- `api.md`：日报表接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确日级统计口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现日报表页面与查询接口的场景
- 适用于日级统计查询、告警展示与滚动余额验证

## 6. 风险与注意事项
- 当前计算直接基于已过账总账分录按日聚合
- 借方、贷方、凭证数、滚动余额口径必须与后端实现一致
- 初始化自动查询与实际查询参数映射必须保持一致
- 科目选项加载依赖需保持可用

## 7. 回滚与兜底
- 若日级统计口径调整失败，优先回滚到已验证的计算逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若初始化自动查询行为不兼容，先保留手动查询能力并降级自动触发
