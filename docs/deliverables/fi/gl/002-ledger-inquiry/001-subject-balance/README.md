# 科目余额表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/002-ledger-inquiry/001-subject-balance/`
- 业务名称：科目余额表
- 业务定位：总账账表查询中的余额类报表，用于展示期初余额、本期借贷发生和期末余额。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/dictionary.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/api.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/backend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/frontend.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/sql.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/test.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/review.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/ops.md`
- `docs/prompt/fi/gl/002-ledger-inquiry/001-subject-balance/mock.md`

## 3. 交付物清单
- `dictionary.md`：余额字段与统计口径说明
- `api.md`：余额表接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确余额口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现科目余额表页面与查询接口的场景
- 适用于余额查询、告警展示与汇总验证

## 6. 风险与注意事项
- 当前计算直接基于已过账总账分录，不读取期间余额快照表
- 期初 / 本期借贷 / 期末余额口径必须与后端实现一致
- query 回填与实际查询参数映射必须保持一致
- 科目下拉加载依赖需保持可用

## 7. 回滚与兜底
- 若余额口径调整失败，优先回滚到已验证的计算逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若 query 参数不兼容，先保留基础查询能力，降级路径传参能力
