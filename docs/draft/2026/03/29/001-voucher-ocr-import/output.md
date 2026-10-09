# AI 输出结果

## 初步设计建议

- 拆成两步：`ocr/parse` 和 `ocr/confirm`
- `parse` 只返回结构化识别结果，不直接保存正式数据
- `confirm` 再把识别结果落成凭证草稿和凭证明细
- 保证识别错误时用户还能人工修正再确认

## 接口建议

- `POST /voucher/import/ocr/parse`
- `POST /voucher/import/ocr/confirm`
