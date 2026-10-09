# 凭证测试提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前前后端已实现代码，输出凭证场景的测试与验证提示词，用于生成测试用例、接口验证脚本、回归清单与 UAT 检查项。

## 输入上下文
请结合以下文档：
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

同时结合当前已实现能力：新增、编辑、提交、审核、过账、批量过账、驳回后编辑、借贷平衡校验、精度规则、冲销关联展示、保存错误透出、CSV/OCR 相关流。

## 输出要求
1. 覆盖正常新增、编辑、提交、审核、过账、冲销、删除流程
2. 覆盖借贷不平、期间不合法、状态越权、精度异常等负向场景
3. 覆盖批量过账、驳回后再编辑、冲销关联、保存错误提示等实现特征
4. 输出 API/UI/Data/E2E 分层测试建议
5. 对 BUS 未明确项显式标记“待确认”
