# 总账模块业务文档

本目录用于沉淀 `fi/gl` 总账模块下的核心业务文档。

当前已补充的核心业务包括：

- `ledger/`：账簿
- `account/`：会计科目
- `voucher-word/`：凭证字
- `voucher/`：凭证
- `accounting-period/`：会计期间
- `opening-balance/`：期初余额
- `carry-forward-template/`：结转模板
- `period-close/`：期末处理 / 结账
- `cashflow-item/`：现金流量项目

说明：

1. 当前阶段先补充 `business.md`，用于明确业务背景、目标、定位和上下游关系。
2. 后续可继续为每个业务补充 `fields.md`、`flow.md`、`rules.md`、`page.md`、`api.md`。
3. 若后续代码中存在更细分的总账子域，可在此目录下继续扩展。
