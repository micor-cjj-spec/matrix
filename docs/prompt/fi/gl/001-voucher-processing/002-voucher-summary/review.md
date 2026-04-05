# 凭证汇总表评审提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/002-voucher-summary/` 下业务文档、当前 prompt 文档，以及已确认实现，输出凭证汇总表场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/002-voucher-summary/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查聚合口径、状态口径、warnings 口径是否一致
3. 检查接口返回、页面展示、查看凭证跳转是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
