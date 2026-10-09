# 凭证协同检查交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/`
- 业务名称：凭证协同检查
- 业务定位：校验凭证表头、凭证分录和总账分录三者之间的一致性，优先暴露借贷不平、缺少分录、缺少总账分录和重复凭证号等问题。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/review.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/ops.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/003-voucher-collaboration-check/mock.md`

## 3. 交付物清单
- `dictionary.md`：检查字段与统计口径说明
- `api.md`：凭证协同检查接口正式说明
- `sql.md`：查询与问题识别说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确扫描凭证数、异常条数、高风险问题、健康凭证、问题类型口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现凭证协同检查页面与查询接口的场景
- 适用于凭证链路健康检查、异常定位与历史修复依据分析

## 6. 风险与注意事项
- 当前结果依赖凭证、凭证分录、总账分录联合检查
- 扫描凭证数、异常条数、高风险问题、健康凭证、问题类型、warnings 口径必须与后端实现一致
- 跳转查看凭证的参数映射必须保持一致
- 凭证链路依赖需保持可用

## 7. 回滚与兜底
- 若检查逻辑或问题口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留检查查询能力，降级外部联动能力
