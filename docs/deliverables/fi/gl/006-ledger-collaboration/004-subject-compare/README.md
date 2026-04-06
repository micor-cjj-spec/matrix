# 科目余额对照交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/006-ledger-collaboration/004-subject-compare/`
- 业务名称：科目余额对照
- 业务定位：对比同一期间内“凭证分录汇总口径”和“总账分录汇总口径”在科目层面的期初、发生和期末差异，用于定位过账、冲销或历史修正引起的科目差异。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/review.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/ops.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/004-subject-compare/mock.md`

## 3. 交付物清单
- `dictionary.md`：对照字段与统计口径说明
- `api.md`：科目余额对照接口正式说明
- `sql.md`：查询与差异计算说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确科目数、差异科目数、凭证借贷合计、总账借贷合计、借贷差值、余额差额口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现科目余额对照页面与查询接口的场景
- 适用于余额表复核、协同修正定位与差异追踪

## 6. 风险与注意事项
- 当前结果依赖凭证分录汇总口径与总账分录汇总口径
- 科目数、差异科目数、凭证借贷合计、总账借贷合计、借贷差值、余额差额、warnings 口径必须与后端实现一致
- 跳转到科目余额表页面的参数映射必须保持一致
- 科目选项依赖需保持可用

## 7. 回滚与兜底
- 若对照逻辑或差异口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留对照查询能力，降级外部联动能力
