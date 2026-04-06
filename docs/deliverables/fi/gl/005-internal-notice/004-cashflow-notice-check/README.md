# 现金流通知单勾稽交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/005-internal-notice/004-cashflow-notice-check/`
- 业务名称：现金流通知单勾稽
- 业务定位：用于把历史现金流通知与当前最新现金流补充资料扫描结果做对照，识别仍需补录/复核的问题和已自然解除的问题。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/dictionary.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/api.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/backend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/frontend.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/sql.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/test.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/review.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/ops.md`
- `docs/prompt/fi/gl/005-internal-notice/004-cashflow-notice-check/mock.md`

## 3. 交付物清单
- `dictionary.md`：勾稽字段与统计口径说明
- `api.md`：现金流通知单勾稽接口正式说明
- `sql.md`：查询与比对说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确仍需处理、已自然解除、快照金额与当前金额口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现现金流通知单勾稽页面与查询接口的场景
- 适用于复核历史现金流通知是否仍有效，并为关闭历史通知提供依据

## 6. 风险与注意事项
- 当前结果依赖内部通知单表与最新现金流候选结果
- 仍需处理、已自然解除、快照金额、当前金额、warnings 口径必须与后端实现一致
- 跳转到凭证页的规则必须保持一致
- 现金流候选结果依赖需保持可用

## 7. 回滚与兜底
- 若勾稽口径调整失败，优先回滚到已验证的比对逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留勾稽查询能力，降级外部联动能力
