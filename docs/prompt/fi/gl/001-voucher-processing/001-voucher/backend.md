# 凭证后端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前 `matrix` 已实现代码，输出凭证场景的后端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/business.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/fields.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/flow.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/rules.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/api.md`

同时结合当前已实现能力：
- 凭证明细维护
- 借贷平衡校验
- 过账生成总账分录
- 同步批量过账
- 币别精度与舍入规则
- 审核流与驳回后可编辑边界
- 列表日期区间筛选

## 输出要求
1. 说明当前后端已实现能力与待补齐项
2. 输出 controller / service / repository / domain 的职责划分建议
3. 说明保存、提交、审核、过账、冲销、删除等动作的处理边界
4. 说明借贷平衡、期间校验、状态校验、权限校验、精度校验的落点
5. 说明事务边界、幂等、批量过账处理建议
6. 对 BUS 未明确项显式标记“待确认”
