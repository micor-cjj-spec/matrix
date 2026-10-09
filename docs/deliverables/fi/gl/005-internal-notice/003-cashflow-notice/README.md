# 现金流通知单交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/005-internal-notice/003-cashflow-notice/`
- 业务名称：现金流通知单
- 业务定位：把现金流补充资料中的待复核凭证转化为内部处理单，用于推动团队补录现金流项目、处理多编码和确认现金划转事项。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/api.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/test.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/review.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/ops.md`
- `docs/prompt/fi/gl/005-internal-notice/003-cashflow-notice/mock.md`

## 3. 交付物清单
- `dictionary.md`：通知字段与统计口径说明
- `api.md`：现金流通知单接口正式说明
- `sql.md`：查询与生成说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确通知状态、紧急程度、问题来源、问题金额与生成结果口径
2. 再看 `api.md` 与 `sql.md` 对齐查询接口、生成接口与数据逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现现金流通知单页面、查询接口与生成接口的场景
- 适用于现金流问题通知生成、通知跟踪与后续勾稽处理入口联动

## 6. 风险与注意事项
- 当前结果依赖现金流补充资料待处理凭证与内部通知单表
- 通知状态、紧急程度、问题来源、问题金额、生成结果统计口径必须与后端实现一致
- 跳转到凭证页与现金流通知单勾稽页面的参数映射必须保持一致
- 现金流补充资料依赖需保持可用

## 7. 回滚与兜底
- 若通知生成或查询口径调整失败，优先回滚到已验证的逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留通知查询与生成能力，降级外部联动能力
