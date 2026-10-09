# 结转清单评审提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/003-carry-list/` 下业务文档、当前 prompt 文档，以及已确认实现，输出结转清单场景的一致性评审提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/003-carry-list/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查检查项口径、warnings 口径、相关凭证口径是否一致
3. 检查页面展示、接口返回、查看凭证跳转是否一致
4. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
5. 标记后续待补项
