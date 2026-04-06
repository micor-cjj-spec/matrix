# 往来对账单交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/003-counterparty-statement/`
- 业务名称：往来对账单
- 业务定位：往来管理主对账报表，用于按单据维度展示原额、已核销金额、未核销金额、核销状态和最近核销批次。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/003-counterparty-statement/mock.md`

## 3. 交付物清单
- `dictionary.md`：对账字段与统计口径说明
- `api.md`：往来对账单接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确原额、已核销、未核销、核销率与最近批次口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来对账单页面与查询接口的场景
- 适用于单据级核销进度查看、最近批次反查与告警展示

## 6. 风险与注意事项
- 当前结果依赖往来单据和已落库核销日志/链接
- 原额、已核销金额、未核销金额、核销率、最近批次口径必须与后端实现一致
- 关联凭证跳转与核销日志跳转参数映射必须保持一致
- 往来方与日志依赖需保持可用

## 7. 回滚与兜底
- 若对账口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转参数不兼容，先保留对账查询能力，降级跳转能力
