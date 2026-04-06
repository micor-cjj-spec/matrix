# 往来核销方案交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/`
- 业务名称：往来核销方案
- 业务定位：自动核销前的方案规划页，用于在不落库前提下预览未核销往来单据的匹配建议。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/001-counterparty-writeoff-plan/mock.md`

## 3. 交付物清单
- `dictionary.md`：方案字段与统计口径说明
- `api.md`：核销方案接口正式说明
- `sql.md`：查询与匹配说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确建议核销与剩余金额口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来核销方案页面与查询接口的场景
- 适用于自动核销前方案预览、告警展示与跳转验证

## 6. 风险与注意事项
- 当前结果基于 AR/AP 单据与已落库核销链接生成，仅预览不落库
- 建议核销金额、剩余金额、warnings 口径必须与后端实现一致
- “去自动核销”跳转参数映射必须保持一致
- 往来方依赖需保持可用

## 7. 回滚与兜底
- 若方案口径调整失败，优先回滚到已验证的匹配逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转参数不兼容，先保留方案预览能力，降级跳转能力
