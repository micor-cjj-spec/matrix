# 凭证样例提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前前后端已实现代码，输出凭证场景的样例数据与 mock 提示词，用于生成请求样例、响应样例、列表样例、明细样例、异常样例与联调数据。

## 输入上下文
请结合以下文档：
- `fields.md`
- `flow.md`
- `page.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/test.md`

同时结合当前实现特征：列表筛选、分录编辑、批量过账、冲销关联、保存错误透出、CSV/OCR 相关流。

## 输出要求
1. 输出列表样例、详情样例、保存样例、工作流动作样例
2. 输出借贷平衡 / 不平衡、不同状态、冲销关联、批量过账结果等样例
3. 输出保存失败、状态越权、期间不合法、精度异常等错误样例
4. 输出 CSV/OCR 预览或导入相关样例（若当前范围内）
5. 对 BUS 未明确项显式标记“待确认”
