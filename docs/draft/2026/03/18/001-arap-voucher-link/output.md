# AI 输出结果

## 初步设计建议

- 在往来单审核通过后直接调用 `generateVoucher`
- 用代码内置映射维护 `fdoctype -> 借贷科目`
- 统一生成两条分录：借方一条、贷方一条
- 若单据已关联凭证，则直接幂等返回

## 关键设计点

- 往来单和凭证通过 `fvoucherId / fvoucherNumber` 建立关联
- 自动转凭证逻辑放在 Service 内，不放在 Controller
- 审核通过和生成凭证保持同一条业务链路
