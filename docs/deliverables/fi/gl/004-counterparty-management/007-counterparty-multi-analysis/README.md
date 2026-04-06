# 往来多维分析表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/`
- 业务名称：往来多维分析表
- 业务定位：往来管理的汇总分析页，用于按往来方、单据类型、状态、角色等维度汇总往来金额和未核销余额。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/007-counterparty-multi-analysis/mock.md`

## 3. 交付物清单
- `dictionary.md`：多维分析字段与统计口径说明
- `api.md`：往来多维分析接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确 groupDimension、原额、已核销、未核销口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来多维分析表页面与查询接口的场景
- 适用于汇总分析、清账优先级判断与风险盘点

## 6. 风险与注意事项
- 当前结果依赖往来单据和已落库核销链接按分组维度聚合
- groupDimension、原额、已核销、未核销、warnings 口径必须与后端实现一致
- 页面只做汇总分析，不直接下钻到执行动作
- 分组维度依赖需保持可用

## 7. 回滚与兜底
- 若分析口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若分组维度展示不兼容，先保留基础汇总能力，降级高级维度展示
