# 对冲凭证交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/006-ledger-collaboration/002-offset-voucher/`
- 业务名称：对冲凭证
- 业务定位：按冲销链路展示原凭证与对冲凭证的配对关系，识别缺原凭证、缺对冲凭证或金额不一致的孤儿记录。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/dictionary.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/api.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/backend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/frontend.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/sql.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/test.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/review.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/ops.md`
- `docs/prompt/fi/gl/006-ledger-collaboration/002-offset-voucher/mock.md`

## 3. 交付物清单
- `dictionary.md`：配对字段与统计口径说明
- `api.md`：对冲凭证接口正式说明
- `sql.md`：查询与配对说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确已配对、孤儿记录、原凭证金额、对冲金额、金额差异口径
2. 再看 `api.md` 与 `sql.md` 对齐接口与查询逻辑
3. 然后执行 `test.md` 验证
4. 发布前检查 `review.md` 和 `ops.md`
5. 联调或演示时使用 `mock.md`

## 5. 适用环境
- 当前 `dev` 分支前后端已实现对冲凭证页面与查询接口的场景
- 适用于冲销链路追踪、孤儿记录排查与金额差异检查

## 6. 风险与注意事项
- 当前结果依赖凭证表中的冲销编号与备注关联信息
- 已配对、孤儿记录、原凭证金额、对冲金额、金额差异口径必须与后端实现一致
- 跳转查看原凭证和对冲凭证的参数映射必须保持一致
- 凭证链路依赖需保持可用

## 7. 回滚与兜底
- 若配对逻辑或金额差异口径调整失败，优先回滚到已验证的查询逻辑
- 若 warnings 规则存在争议，先保留页面展示但标记说明口径待确认
- 若跳转不兼容，先保留配对查询能力，降级外部联动能力
