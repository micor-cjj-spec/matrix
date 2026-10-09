# 往来核销日志交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/`
- 业务名称：往来核销日志
- 业务定位：自动核销批次和落库配对明细的审计追踪页面，用于查看批次级结果并按方案号下钻明细。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/dictionary.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/api.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/backend.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/frontend.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/sql.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/test.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/review.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/ops.md`
- `docs/prompt/fi/gl/004-counterparty-management/005-counterparty-writeoff-log/mock.md`

## 3. 交付物清单
- `dictionary.md`：日志字段与审计口径说明
- `api.md`：核销日志接口正式说明
- `sql.md`：查询与下钻说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确批次数、链接数、核销金额与明细查看口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现往来核销日志页面与查询接口的场景
- 适用于自动核销结果追溯、批次摘要查看与按方案号下钻明细

## 6. 风险与注意事项
- 当前结果依赖 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`
- 批次数、链接数、核销金额、操作时间、warnings 口径必须与后端实现一致
- 按方案号下钻与批次/明细联动口径必须保持一致
- 日志与链接数据依赖需保持可用

## 7. 回滚与兜底
- 若日志口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若按方案号下钻不兼容，先保留批次查询能力，降级明细联动能力
