# AI 输出结果

## 初步设计建议

- 凭证明细独立表维护，按 `voucherId` 关联
- 明细字段保留科目、借贷金额、币种、汇率、原币金额、现金流项目
- 凭证详情页支持同时展示来源往来单
- 通过 `voucherId / voucherNumber` 反查 `ArapDoc`

## 接口建议

- `GET /voucher/{fid}/lines`
- `PUT /voucher/{fid}/lines`
- `GET /voucher/{fid}/source-docs`
