# 总账业务文档阶段性交接说明（gl）

## 一、当前交付范围
本轮已围绕 `docs/bus/fi/gl` 完成以下工作：

1. 按总账前端菜单重建新目录结构，形成 10 个一级分组。
2. 对已实现模块，按代码补齐：
   - `manifest.json`
   - `business.md`
   - `fields.md`
   - `flow.md`
   - `rules.md`
   - `page.md`
   - `api.md`
3. 对未实现模块，按代码现状标记为 `not_implemented`，仅保留状态说明，不虚构业务细节。
4. 对“前端已接入、后端实现文件本轮未检出”的模块，标记为 `partial_frontend_integrated`。
5. 对分析报表分组下复用已有页面的入口，标记为 `reused_entry`。
6. 对旧路径历史目录补充废弃说明，降低新旧混用风险。

## 二、关键索引文件
- `INDEX.md`：新目录结构总入口
- `AUDIT_STATUS.md`：模块状态总览
- `LEGACY_PATHS.md`：旧路径与新路径映射清单
- `CLEANUP_PRIORITY.md`：旧路径清理优先级

## 三、当前状态定义
- `done`：已按代码补齐完整业务文档
- `partial_frontend_integrated`：前端已接入页面/接口，但本轮未检出后端实现文件
- `not_implemented`：菜单占位，前端未接入独立页面，后端未检出实现
- `reused_entry`：菜单复用已有业务入口，不单独维护一套文档

## 四、已完成的主要分组
### 1. 已完整补齐（done）
- `001-voucher-processing`
- `002-ledger-inquiry`
- `003-cash-flow`
- `004-counterparty-management`
- `005-internal-notice`
- `006-ledger-collaboration`
- `010-basic-settings` 中：
  - `002-cash-flow-item`
  - `003-report-account-map`
- `008-analysis-reports` 中：
  - `001-report-item`

### 2. 前端已接入但后端实现文件本轮未检出
- `008-analysis-reports/003-balance-sheet`
- `008-analysis-reports/004-profit-statement`

### 3. 未实现（菜单占位）
- `007-period-end-processing` 全组
- `009-initialization` 全组
- `008-analysis-reports/002-financial-indicator`
- `008-analysis-reports/005-enterprise-tax`
- `010-basic-settings` 中除 `现金流量项目`、`报表科目映射` 外的大部分模块

### 4. 复用入口
- `008-analysis-reports/006-cash-flow-statement`
  - 复用：`003-cash-flow/001-cash-flow-statement`

## 五、旧路径处理情况
### 1. 已补废弃说明（DEPRECATED.md）的旧目录
- `voucher/`
- `cashflow-item/`
- `opening-balance/`
- `account/`
- `ledger/`
- `voucher-word/`
- `accounting-period/`
- `carry-forward-template/`
- `period-close/`

### 2. 清理优先级
#### P0：有明确新路径承接，可优先删除
- `docs/bus/fi/gl/voucher/`
- `docs/bus/fi/gl/cashflow-item/`

#### P1：有新分组承接，但新分组当前未实现
- `docs/bus/fi/gl/opening-balance/`
- `docs/bus/fi/gl/period-close/`

#### P2：暂未明确新实现承接
- `docs/bus/fi/gl/account/`
- `docs/bus/fi/gl/ledger/`
- `docs/bus/fi/gl/voucher-word/`
- `docs/bus/fi/gl/accounting-period/`
- `docs/bus/fi/gl/carry-forward-template/`

## 六、当前已知边界
1. 当前 GitHub 连接可稳定新建文件，但未直接暴露“删除文件 / 覆盖现有文件”的高层接口。
2. 虽然存在底层 `create_blob / create_tree / create_commit / update_ref` 能力，但本轮未成功拿到当前 commit 的 `tree_sha`，因此未完成旧目录的物理删除。
3. 顶层 `docs/bus/fi/gl/README.md` 仍为旧内容；当前已通过新增 `INDEX.md` 提供新入口，但 README 本体尚未正式替换。

## 七、建议的后续动作
1. 优先替换 `docs/bus/fi/gl/README.md`，使其指向新结构。
2. 在具备删除能力后，先按 `CLEANUP_PRIORITY.md` 删除 P0 旧目录。
3. 继续追查：
   - `资产负债表`
   - `利润表`
   对应后端实现文件或接口来源。
4. 等期末处理、初始化、基础设置其余模块代码落地后，再把 `not_implemented` 目录扩写为正式业务文档。

## 八、总原则
- 文档目录以新菜单分组结构为准。
- 业务描述以当前代码现状为准。
- 未实现模块不虚构。
- 旧路径不再继续扩写，只做废弃、迁移或删除收口。
