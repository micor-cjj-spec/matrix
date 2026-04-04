# 总账业务文档索引（gl）

本目录已按前端总账菜单重新整理为 10 个分组目录，并以 `dev` 分支当前前后端代码作为文档对齐基准。

## 使用说明
- 模块状态总览：见 `AUDIT_STATUS.md`
- 历史旧路径清理建议：见 `LEGACY_PATHS.md`
- 目录结构以当前菜单分组为准，不再以旧的 `voucher / cashflow-item / opening-balance` 等散列目录为准

## 分组目录

### 001-voucher-processing（凭证处理）
- `001-voucher`
- `002-voucher-summary`
- `003-carry-list`

### 002-ledger-inquiry（账表查询）
- `001-subject-balance`
- `002-general-ledger`
- `003-detail-ledger`
- `004-daily-report`
- `005-dimension-balance`
- `006-aux-dimension-balance`
- `007-aux-general-ledger`
- `008-aux-detail-ledger`

### 003-cash-flow（现金流量）
- `001-cash-flow-statement`
- `002-cash-flow-query`
- `003-cash-flow-supplement`

### 004-counterparty-management（往来管理）
- `001-counterparty-writeoff-plan`
- `002-counterparty-auto-writeoff`
- `003-counterparty-statement`
- `004-counterparty-account-query`
- `005-counterparty-writeoff-log`
- `006-aging-analysis`
- `007-counterparty-multi-analysis`

### 005-internal-notice（内部通知单）
- `001-counterparty-notice`
- `002-counterparty-notice-check`
- `003-cashflow-notice`
- `004-cashflow-notice-check`

### 006-ledger-collaboration（账簿协同管理）
- `001-voucher-conversion-rule`
- `002-offset-voucher`
- `003-voucher-collaboration-check`
- `004-subject-compare`

### 007-period-end-processing（期末处理）
- `001-profit-loss-transfer`
- `002-auto-transfer`
- `003-fx-revaluation`
- `004-voucher-amortization`
- `005-period-close`
- `006-monitor-center`

### 008-analysis-reports（分析报表）
- `001-report-item`
- `002-financial-indicator`
- `003-balance-sheet`
- `004-profit-statement`
- `005-enterprise-tax`
- `006-cash-flow-statement`

### 009-initialization（初始化）
- `001-subject-opening-balance`
- `002-cash-flow-opening`
- `003-counterparty-opening-balance`

### 010-basic-settings（基础设置）
- `001-voucher-type`
- `002-cash-flow-item`
- `003-report-account-map`
- `004-dimension-relation-setting`
- `005-dimension-value-range-setting`
- `006-equity-change-type`
- `007-impairment-nature`
- `008-license-plate-item`
- `009-cost-nature`

## 当前收口原则
1. 新结构优先：所有新增或修订文档都以新分组目录为准。
2. 代码优先：只描述当前前后端代码中能确认的能力。
3. 未实现模块不虚构细节：仅保留状态说明，等代码落地后再扩展。
4. 复用入口不重复维护：如分析报表下的现金流量表，直接复用已有文档。
