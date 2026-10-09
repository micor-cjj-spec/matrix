# 财务基础资料业务文档索引（finance-base-data）

本目录按当前前后端代码中的真实菜单与路由整理。

## 当前模块
- `001-account-subject`：会计科目

## 代码对齐说明
前端 `FinanceBaseDataView.vue` 当前展示 4 个“已可使用”模块：
- 会计科目
- 报表项目
- 现金流量项目
- 报表科目映射

其中，只有“会计科目”真正属于本分组并在本目录下单独维护；其余 3 个为复用入口：
- 报表项目 → `docs/bus/fi/gl/008-analysis-reports/001-report-item/`
- 现金流量项目 → `docs/bus/fi/gl/010-basic-settings/002-cash-flow-item/`
- 报表科目映射 → `docs/bus/fi/gl/010-basic-settings/003-report-account-map/`

## 主要路由
- `/finance/base-data`
- `/finance/base-data/account-subject`
- `/finance/base-data/account-subject/form/:fid?`
