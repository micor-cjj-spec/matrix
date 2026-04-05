# 凭证运维提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前前后端已实现代码，输出凭证场景的发布、运维、监控、回滚与问题处理提示词。

## 输入上下文
请结合以下文档：
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/review.md`

同时结合当前实现特征：过账、批量过账、精度规则、保存报错、冲销关联、CSV/OCR 流程。

## 输出要求
1. 输出发布依赖检查项（前端/后端/SQL）
2. 输出配置与兼容性注意项
3. 输出监控项与告警关注点
4. 输出批量过账、导入、精度相关问题的运维关注点
5. 输出回滚与应急处理建议
6. 对 BUS 未明确项显式标记“待确认”
