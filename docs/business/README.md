# 业务分析文档目录（matrix）

这个目录用于沉淀 matrix 项目的业务分析与规则，按模块归档，便于后续持续补充。

## 目录结构

- `00-overview/`：整体业务蓝图、术语、跨模块流程
- `gl/`：总账（General Ledger）
- `ar/`：应收（Accounts Receivable）
- `ap/`：应付（Accounts Payable）
- `ar-ap/`：应收应付联合业务主题
- `fund/`：资金管理（Fund/Cash）
- `payroll/`：薪酬
- `asset/`：资产管理
- `shared/`：共享能力（审批流、权限、状态机、字典等）
- `templates/`：分析模板

## 使用约定

1. 每个业务主题单独建文件（例如：`gl/voucher-business.md`）。
2. 每次分析尽量包含：
   - 业务目标
   - 状态流转
   - 核心对象与字段
   - 关键规则
   - 异常与边界
   - 待确认问题
3. 文件命名建议：`<topic>-business.md` 或 `<topic>-design.md`。
4. 如果分析会影响开发，附上“接口/表结构建议”小节。

## 当前已记录

- `gl/voucher-business.md`：总账-凭证业务路径（初版）
- `gl/balance-business.md`：总账-余额表业务设计（初版）
- `gl/report-platform-business.md`：总账-报表基础平台业务设计（初版）
- `gl/balance-profit-report-business.md`：总账-资产负债表与利润表业务设计（初版）
- `gl/cash-flow-report-business.md`：总账-现金流量表业务设计（初版）
- `gl/report-dev-spec-v1.md`：总账-三大报表开发规格建议（V1）
- `ar-ap/ar-ap-seven-docs-business.md`：应收应付七类单据业务设计（MVP）
- `ar-ap/aging-credit-business.md`：应收应付账龄分析与信用控制（预警版）
- `00-overview/code-doc-alignment-2026-03-12.md`：代码与业务文档对齐检查
