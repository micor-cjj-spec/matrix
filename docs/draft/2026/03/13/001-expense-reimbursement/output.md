# AI 输出结果

## 初步业务判断

费用报销单建议归属 `fi/er` 领域，而不是直接归属 `fi/gl`。

## 初步设计建议

- 业务文档沉淀到 `docs/bus/fi/er/expense-reimbursement/`
- 开发提示词沉淀到 `docs/prompt/fi/er/expense-reimbursement/`
- 审批通过后触发凭证生成，凭证业务可与 `fi/gl/voucher` 建立联动关系
