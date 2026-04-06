# 现金流量补充资料交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/003-cash-flow/003-cash-flow-supplement/`
- 业务名称：现金流量补充资料
- 业务定位：现金流主表之外的质量检查与补录页面，用于集中展示当前期间需要补录、复核和说明的现金流事项。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/dictionary.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/api.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/backend.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/frontend.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/sql.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/test.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/review.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/ops.md`
- `docs/prompt/fi/gl/003-cash-flow/003-cash-flow-supplement/mock.md`

## 3. 交付物清单
- `dictionary.md`：补充资料字段与统计口径说明
- `api.md`：补充资料接口正式说明
- `sql.md`：查询与聚合说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确任务清单、分类分布、待补录/复核凭证口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现现金流量补充资料页面与查询接口的场景
- 适用于补录复核、分类分布分析与查看凭证验证

## 6. 风险与注意事项
- 当前结果依赖现金相关凭证分析结果
- 任务清单、分类分布、待补录/复核凭证、warnings 口径必须与后端实现一致
- 初始化自动查询与查看凭证跳转参数映射必须保持一致
- 业务单元依赖需保持可用

## 7. 回滚与兜底
- 若补充资料口径调整失败，优先回滚到已验证的追踪逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若查看凭证跳转参数不兼容，先保留补充资料查询能力，降级跳转能力
