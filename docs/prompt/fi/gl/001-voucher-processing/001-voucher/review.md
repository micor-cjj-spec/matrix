# 凭证评审提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档、当前 prompt 文档，以及 `matrix` / `matrix-web` 已实现代码，输出凭证场景的一致性评审与检查提示词。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/test.md`

## 输出要求
1. 检查 BUS 与 prompt 是否一致
2. 检查 prompt 与后端/前端实现是否一致
3. 检查状态命名、字段口径、接口动作、按钮行为是否一致
4. 检查批量过账、精度规则、驳回后编辑、冲销关联等关键点是否覆盖
5. 输出“已对齐 / 部分对齐 / 缺失 / 待确认”的评审清单
