# 财务云业务文档索引（fi）

本目录按当前前后端代码中的真实菜单与业务归属逐步重建，不再仅以历史散列目录为准。

## 当前已建立的新分组入口
- `gl/`：总账
- `ar/`：应收管理
- `ap/`：应付管理
- `finance-base-data/`：财务基础资料

## 当前索引文件
- `gl/INDEX.md`：总账目录入口
- `gl/AUDIT_STATUS.md`：总账模块状态总览
- `gl/CLEANUP_PRIORITY.md`：总账旧路径清理优先级
- `ar/INDEX.md`：应收目录入口
- `ap/INDEX.md`：应付目录入口
- `finance-base-data/INDEX.md`：财务基础资料入口

## 已完成的主要业务线
### 1. 总账（gl）
已按菜单结构重建并完成主要模块文档，包括：
- 凭证处理
- 账表查询
- 现金流量
- 往来管理
- 内部通知单
- 账簿协同管理
- 分析报表（部分）
- 基础设置（部分）

### 2. 应收（ar）
已按真实路由与通用页结构重建，包括：
- `001-receivable`
- `002-estimated-receivable`
- `003-settlement-processing`
- `004-aging-credit-warning`

### 3. 应付（ap）
已按真实路由与通用页结构重建，包括：
- `001-payable`
- `002-estimated-payable`
- `003-payment-application`
- `004-payment-processing`
- `005-aging-credit-warning`

### 4. 财务基础资料（finance-base-data）
当前已补齐：
- `001-account-subject`

## 复用关系说明
财务基础资料首页当前还展示以下模块，但其实复用的是其他分组下已建立的文档：
- 报表项目 → `gl/008-analysis-reports/001-report-item/`
- 现金流量项目 → `gl/010-basic-settings/002-cash-flow-item/`
- 报表科目映射 → `gl/010-basic-settings/003-report-account-map/`

## 待继续补齐的业务域
当前 `fi` 域下仍可继续按相同方法补齐：
- `fund/`：资金管理
- `tax/`：税务管理
- `fa/`：固定资产
- `shared/`：共享与审核
- 其他财务协同与主数据模块

## 当前收口原则
1. 目录结构以真实菜单和代码归属为准。
2. 已实现模块补齐完整业务文档。
3. 未实现模块仅保留状态说明，不虚构细节。
4. 旧路径不再继续扩写，后续只做迁移、废弃或删除收口。
