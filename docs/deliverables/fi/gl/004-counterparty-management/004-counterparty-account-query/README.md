# 往来账查询交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/004-counterparty-account-query/`
- 业务名称：往来账查询
- 业务定位：单据级追踪页，用于按单据类型、状态、往来方、关键字等条件查询单张往来单据的余额、核销状态、账龄和关联凭证。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/004-counterparty-account-query/mock.md`

## 3. 交付物清单
- `dictionary.md`：单据级查询字段与统计口径说明
- `api.md`：往来账查询接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确原额、已核销、未核销、核销状态与账龄口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来账查询页面与查询接口的场景
- 适用于单据级核对、未核销问题排查与关联凭证查看

## 6. 风险与注意事项
- 当前结果依赖往来单据与已落库核销链接
- 原额、已核销、未核销、核销状态、账龄口径必须与后端实现一致
- 关联凭证查看口径必须保持一致
- 往来方依赖需保持可用

## 7. 回滚与兜底
- 若查询口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若关联凭证查看不兼容，先保留查询能力，降级查看能力
