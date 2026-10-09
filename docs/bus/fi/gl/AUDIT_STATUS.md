# 总账业务文档审计状态（gl）

> 审计基准：以 `dev` 分支当前前后端代码为准，文档状态分为：
>
> - `done`：已按代码补齐 `manifest + business/fields/flow/rules/page/api`
> - `partial_frontend_integrated`：前端已接入页面/接口，但本轮未检出后端实现文件
> - `not_implemented`：菜单占位，前端未接入独立页面，后端未检出实现
> - `reused_entry`：菜单复用已有业务入口，不单独维护一套文档

## 1. 凭证处理
- `001-voucher`：`done`
- `002-voucher-summary`：`done`
- `003-carry-list`：`done`

## 2. 账表查询
- `001-subject-balance`：`done`
- `002-general-ledger`：`done`
- `003-detail-ledger`：`done`
- `004-daily-report`：`done`
- `005-dimension-balance`：`done`
- `006-aux-dimension-balance`：`done`
- `007-aux-general-ledger`：`done`
- `008-aux-detail-ledger`：`done`

## 3. 现金流量
- `001-cash-flow-statement`：`done`
- `002-cash-flow-query`：`done`
- `003-cash-flow-supplement`：`done`

## 4. 往来管理
- `001-counterparty-writeoff-plan`：`done`
- `002-counterparty-auto-writeoff`：`done`
- `003-counterparty-statement`：`done`
- `004-counterparty-account-query`：`done`
- `005-counterparty-writeoff-log`：`done`
- `006-aging-analysis`：`done`
- `007-counterparty-multi-analysis`：`done`

## 5. 内部通知单
- `001-counterparty-notice`：`done`
- `002-counterparty-notice-check`：`done`
- `003-cashflow-notice`：`done`
- `004-cashflow-notice-check`：`done`

## 6. 账簿协同管理
- `001-voucher-conversion-rule`：`done`
- `002-offset-voucher`：`done`
- `003-voucher-collaboration-check`：`done`
- `004-subject-compare`：`done`

## 7. 期末处理
- `001-profit-loss-transfer`：`not_implemented`
- `002-auto-transfer`：`not_implemented`
- `003-fx-revaluation`：`not_implemented`
- `004-voucher-amortization`：`not_implemented`
- `005-period-close`：`not_implemented`
- `006-monitor-center`：`not_implemented`

## 8. 分析报表
- `001-report-item`：`done`
- `002-financial-indicator`：`not_implemented`
- `003-balance-sheet`：`partial_frontend_integrated`
- `004-profit-statement`：`partial_frontend_integrated`
- `005-enterprise-tax`：`not_implemented`
- `006-cash-flow-statement`：`reused_entry`（复用 `003-cash-flow/001-cash-flow-statement`）

## 9. 初始化
- `001-subject-opening-balance`：`not_implemented`
- `002-cash-flow-opening`：`not_implemented`
- `003-counterparty-opening-balance`：`not_implemented`

## 10. 基础设置
- `001-voucher-type`：`not_implemented`
- `002-cash-flow-item`：`done`
- `003-report-account-map`：`done`
- `004-dimension-relation-setting`：`not_implemented`
- `005-dimension-value-range-setting`：`not_implemented`
- `006-equity-change-type`：`not_implemented`
- `007-impairment-nature`：`not_implemented`
- `008-license-plate-item`：`not_implemented`
- `009-cost-nature`：`not_implemented`

## 当前收口建议
1. 把旧目录历史文档统一清理或迁移，避免新旧并存。
2. 补写 `README.md` 的新结构索引，替换旧的 `voucher/account/ledger` 旧口径描述。
3. 对 `partial_frontend_integrated` 模块继续追查后端实现或接口来源。
4. 对 `not_implemented` 模块维持“菜单占位”说明，等代码落地后再展开详细业务文档。
