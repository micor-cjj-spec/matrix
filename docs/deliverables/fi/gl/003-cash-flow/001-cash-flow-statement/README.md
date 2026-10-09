# 现金流量表交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/003-cash-flow/001-cash-flow-statement/`
- 业务名称：现金流量表
- 业务定位：总账分析报表之一，用于按经营、投资、筹资三大活动分类展示本期现金净流量，并计算现金及现金等价物净增加额。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/test.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/review.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/ops.md`
- `docs/prompt/fi/gl/003-cash-flow/001-cash-flow-statement/mock.md`

## 3. 交付物清单
- `dictionary.md`：现金流量表字段与统计口径说明
- `api.md`：现金流量表接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确活动分类、checks、warnings 口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现现金流量表页面与查询接口的场景
- 适用于现金流主表展示、校验提示与跳转验证

## 6. 风险与注意事项
- 当前结果依赖已过账总账分录、现金流项目主数据、报表模板和报表项目
- 经营/投资/筹资活动、净增加额、checks、warnings 口径必须与后端实现一致
- 初始化自动查询与跳转参数映射必须保持一致
- 业务单元、模板、币种依赖需保持可用

## 7. 回滚与兜底
- 若现金流量表口径调整失败，优先回滚到已验证的计算逻辑
- 若 checks / warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转参数不兼容，先保留主表查询能力，降级跳转能力
