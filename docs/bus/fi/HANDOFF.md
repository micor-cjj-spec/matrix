# 财务云业务文档阶段性交接说明（fi）

## 一、当前交付范围
本轮已围绕 `docs/bus/fi` 完成以下工作：

1. 以 `dev` 分支当前前后端代码为基准，逐步重建财务云业务文档目录。
2. 对已实现或可确认前端行为的模块，按代码补齐或落定以下文档：
   - `manifest.json`
   - `business.md`
   - `fields.md`
   - `flow.md`
   - `rules.md`
   - `page.md`
   - `api.md`
3. 对菜单占位或当前未检到实现的模块，按真实状态标记为 `not_implemented`。
4. 对前端已接入但后端实现文件本轮未检出的模块，标记为 `partial_frontend_integrated`。
5. 对各业务分组补充 `INDEX.md` 与状态总览，形成可继续扩展的目录骨架。

## 二、当前已建立的业务分组
### 1. `gl/` 总账
状态：`done`

说明：
- 已按总账前端菜单重建新目录结构
- 已完成主要业务模块文档
- 已完成旧路径审计、废弃标记、清理优先级与阶段性交接说明

关键文件：
- `gl/INDEX.md`
- `gl/AUDIT_STATUS.md`
- `gl/CLEANUP_PRIORITY.md`
- `gl/HANDOFF.md`

### 2. `ar/` 应收管理
状态：`done`

已完成模块：
- `001-receivable`
- `002-estimated-receivable`
- `003-settlement-processing`
- `004-aging-credit-warning`

关键文件：
- `ar/INDEX.md`

### 3. `ap/` 应付管理
状态：`done`

已完成模块：
- `001-payable`
- `002-estimated-payable`
- `003-payment-application`
- `004-payment-processing`
- `005-aging-credit-warning`

关键文件：
- `ap/INDEX.md`

### 4. `finance-base-data/` 财务基础资料
状态：`partial`

已完成模块：
- `001-account-subject`

说明：
- 财务基础资料首页当前可用模块包括：会计科目、报表项目、现金流量项目、报表科目映射
- 其中只有“会计科目”真正属于本分组
- 其余 3 个在当前代码里为复用入口，分别指向总账分组下已有文档

关键文件：
- `finance-base-data/INDEX.md`

### 5. `fund/` 资金管理
状态：`planned / not_implemented`

当前已建立目录：
- `001-fund-dispatch`
- `002-fund-settlement`
- `003-bill-management`

说明：
- 当前前端路由仅存在 `/funds`、`/settlement`、`/bills` 3 个占位入口
- 本轮未检到对应前后端实现

关键文件：
- `fund/INDEX.md`
- `fund/AUDIT_STATUS.md`

### 6. `tax/` 税务管理
状态：`planned / not_implemented`

当前已建立目录：
- `001-tax-connect`
- `002-invoice-management`
- `003-receipt-management`

说明：
- 当前前端路由仅存在 `/tax-connect`、`/invoice`、`/receipt` 3 个占位入口
- 本轮未检到对应前后端实现

关键文件：
- `tax/INDEX.md`
- `tax/AUDIT_STATUS.md`

### 7. `fa/` 固定资产
状态：`planned`

说明：
- 当前仅建立顶层索引与状态总览
- 本轮未检到固定资产独立前端路由与后端实现
- 未继续硬拆“卡片/折旧/变动/清理”等子模块目录

关键文件：
- `fa/INDEX.md`
- `fa/AUDIT_STATUS.md`

### 8. `shared/` 共享与审核
状态：`partial_frontend_integrated`

当前已建立模块：
- `001-shared-ops`

说明：
- 前端路由 `/shared/operations` 已接入真实页面 `SharedOperationsView.vue`
- 本轮未检到对应后端 controller / service 实现
- 文档当前仅按前端页面行为描述，不扩写后端接口与持久化逻辑

关键文件：
- `shared/INDEX.md`
- `shared/AUDIT_STATUS.md`

## 三、顶层索引文件
当前 `fi/` 下已补充：
- `INDEX.md`：财务云业务文档总入口
- `AUDIT_STATUS.md`：财务云分组状态总览

## 四、当前状态定义
- `done`：已按代码补齐完整业务文档
- `partial`：仅完成部分模块或部分目录
- `partial_frontend_integrated`：前端已接入页面，但本轮未检到对应后端实现
- `not_implemented`：菜单占位，前端未接入独立页面，后端未检到实现
- `planned`：当前仅建立分组入口与状态说明，待代码落地后再细分模块

## 五、当前已知边界
1. `fi/README.md` 仍为旧口径描述，当前新结构主入口应以 `fi/INDEX.md` 为准。
2. `gl` 分组已经完成较深度的目录重构与旧路径治理；其他分组当前更多处于“新结构已建立、后续继续扩写”的状态。
3. `fund / tax / fa` 当前仍以状态说明为主，不应虚构业务细节。
4. `shared` 当前仅能确认前端页面行为，后端接口与落库逻辑尚未在本轮代码检索中确认。

## 六、建议的后续动作
1. 后续优先继续补能和总账、AR/AP 形成闭环的业务域，如：
   - `fund`（若相关代码落地）
   - `tax`（若相关代码落地）
2. 若 `shared` 后端实现后续补齐，可回填：
   - 真实接口
   - 数据结构
   - 持久化与状态流转逻辑
3. 条件成熟时，统一替换 `fi/README.md` 旧内容，明确指向 `fi/INDEX.md`。
4. 每新增一个新分组，都同步补：
   - `INDEX.md`
   - `AUDIT_STATUS.md`
   - 必要时补 `HANDOFF.md`

## 七、总原则
- 目录结构以当前前后端代码中的真实菜单和路由归属为准。
- 已实现模块补齐完整业务文档。
- 未实现模块不虚构。
- 前端已接入但后端未确认的模块，只描述可确认的前端行为。
- 旧口径文档不再继续扩写，后续只做替换、收口或废弃处理。
