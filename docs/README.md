# docs

新的 docs 体系按三层组织：

- `draft/`：记录原始需求、AI 输出与人工修订
- `bus/`：沉淀正式业务文档
- `prompt/`：沉淀可复用的开发提示词
- `template/`：新业务复制模板

## 目录约定

### draft
按“年/月/日/序号-业务名”组织，例如：

`docs/draft/2026/04/03/001-expense-reimbursement/`

目录内通常包含：

- `input.md`
- `output.md`
- `notes.md`

### bus
按“领域/子领域/业务名”组织，例如：

- `docs/bus/fi/er/expense-reimbursement/`
- `docs/bus/fi/gl/voucher/`

### prompt
按“领域/子领域/业务名”组织，例如：

- `docs/prompt/fi/er/expense-reimbursement/`
- `docs/prompt/fi/gl/voucher/`

## 说明

当前仓库中旧的 docs 内容后续可逐步废弃；新业务请优先按本目录规范新增。
