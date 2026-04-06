# 现金流量查询交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/003-cash-flow/002-cash-flow-query/`
- 业务名称：现金流量查询
- 业务定位：现金流量归类追踪页，用于查看每张现金相关凭证如何被分类到现金流量表，以及分类来源和复核原因。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/api.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/test.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/review.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/ops.md`
- `docs/prompt/fi/gl/003-cash-flow/002-cash-flow-query/mock.md`

## 3. 交付物清单
- `dictionary.md`：追踪字段与识别来源口径说明
- `api.md`：现金流量查询接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确识别来源与统计口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现现金流量查询页面与查询接口的场景
- 适用于现金流分类追踪、告警展示与查看凭证验证

## 6. 风险与注意事项
- 当前结果依赖现金相关凭证分析结果
- 直接标记、规则推断、未知编码、多编码复核、现金划转等口径必须与后端实现一致
- 初始化自动查询与查看凭证跳转参数映射必须保持一致
- 业务单元与现金流项目依赖需保持可用

## 7. 回滚与兜底
- 若现金流量查询口径调整失败，优先回滚到已验证的追踪逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若查看凭证跳转参数不兼容，先保留追踪查询能力，降级跳转能力
