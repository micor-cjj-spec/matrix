# 财务云业务文档审计状态（fi）

> 审计基准：以 `dev` 分支当前前后端代码为准。
>
> 状态说明：
> - `done`：已按代码补齐完整业务文档
> - `partial`：仅完成部分模块或部分目录
> - `planned`：当前仅建立索引或规划，尚未展开具体业务文档

## 1. 总账（gl）
状态：`done`

说明：
- 已建立新目录结构与索引入口
- 已完成主要业务模块文档
- 已完成旧路径审计、废弃标记与清理优先级整理

参考文件：
- `gl/INDEX.md`
- `gl/AUDIT_STATUS.md`
- `gl/CLEANUP_PRIORITY.md`
- `gl/HANDOFF.md`

## 2. 应收（ar）
状态：`done`

已完成模块：
- `001-receivable`
- `002-estimated-receivable`
- `003-settlement-processing`
- `004-aging-credit-warning`

参考文件：
- `ar/INDEX.md`

## 3. 应付（ap）
状态：`done`

已完成模块：
- `001-payable`
- `002-estimated-payable`
- `003-payment-application`
- `004-payment-processing`
- `005-aging-credit-warning`

参考文件：
- `ap/INDEX.md`

## 4. 财务基础资料（finance-base-data）
状态：`partial`

已完成模块：
- `001-account-subject`

复用入口说明：
- 报表项目 → 复用 `gl/008-analysis-reports/001-report-item/`
- 现金流量项目 → 复用 `gl/010-basic-settings/002-cash-flow-item/`
- 报表科目映射 → 复用 `gl/010-basic-settings/003-report-account-map/`

参考文件：
- `finance-base-data/INDEX.md`

## 5. 资金（fund）
状态：`planned`

说明：
- 当前尚未建立新目录结构
- 后续可按真实菜单与代码实现继续补齐

## 6. 税务（tax）
状态：`planned`

说明：
- 当前尚未建立新目录结构
- 后续可按真实菜单与代码实现继续补齐

## 7. 固定资产（fa）
状态：`planned`

说明：
- 当前尚未建立新目录结构
- 后续可按真实菜单与代码实现继续补齐

## 8. 共享与审核（shared）
状态：`planned`

说明：
- 当前尚未建立新目录结构
- 后续可按真实菜单与代码实现继续补齐

## 当前建议
1. 后续优先补 `fund / tax / fa`。
2. 每补完一个分组，都同步补 `INDEX.md` 与状态说明。
3. `fi/README.md` 当前仍为旧口径，后续应替换为新结构入口或明确指向 `fi/INDEX.md`。
