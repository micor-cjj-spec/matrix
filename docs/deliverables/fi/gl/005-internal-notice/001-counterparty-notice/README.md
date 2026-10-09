# 往来通知单交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/005-internal-notice/001-counterparty-notice/`
- 业务名称：往来通知单
- 业务定位：把应收/应付账龄与风险扫描结果转化为内部处理单的页面，用于推动团队跟进超额度、超期和长期未清问题。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/test.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/review.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/ops.md`
- `docs/prompt/fi/gl/005-internal-notice/001-counterparty-notice/mock.md`

## 3. 交付物清单
- `dictionary.md`：通知字段与统计口径说明
- `api.md`：往来通知单接口正式说明
- `sql.md`：查询与生成说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确通知状态、紧急程度、通知金额与未清金额口径
2. 再看 `api.md` 与 `sql.md` 对齐查询接口、生成接口与数据逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来通知单页面、查询接口与生成接口的场景
- 适用于通知生成、通知跟踪与后续勾稽处理入口联动

## 6. 风险与注意事项
- 当前结果依赖往来账龄分析结果和内部通知单表
- 通知状态、紧急程度、通知金额、未清金额、生成结果统计口径必须与后端实现一致
- 跳转到对账单与勾稽页面的参数映射必须保持一致
- 账龄风险扫描依赖需保持可用

## 7. 回滚与兜底
- 若通知生成或查询口径调整失败，优先回滚到已验证的逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留通知查询与生成能力，降级外部联动能力
