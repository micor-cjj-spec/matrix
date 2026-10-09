# 账龄分析表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/006-aging-analysis/`
- 业务名称：账龄分析表
- 业务定位：按往来方汇总剩余未核销余额并按账龄区间分桶的风险分析报表，用于催收、清账和信用控制。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/006-aging-analysis/mock.md`

## 3. 交付物清单
- `dictionary.md`：账龄字段与风险口径说明
- `api.md`：账龄分析接口正式说明
- `sql.md`：查询与分桶说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确未核销余额、账龄分桶、信用额度与风险状态口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现账龄分析表页面与查询接口的场景
- 适用于往来风险识别、超额度分析与逾期预警

## 6. 风险与注意事项
- 当前结果依赖往来单据、核销链接和往来方信用额度主数据
- 未核销余额、账龄分桶、信用额度、逾期阈值、风险状态口径必须与后端实现一致
- 页面只做分析展示，不直接执行核销
- 信用额度主数据依赖需保持可用

## 7. 回滚与兜底
- 若分析口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若风险状态展示不兼容，先保留余额与分桶展示能力，降级风险标识能力
