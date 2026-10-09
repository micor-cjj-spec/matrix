# 凭证前端提示词

## 目标
结合 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前 `matrix-web` 已实现代码，输出凭证场景的前端实现建议与补齐提示词。

## 输入上下文
请结合以下文档：
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/page.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/flow.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/fields.md`
- `docs/bus/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`

同时结合当前已实现能力：
- 凭证 CRUD 页面与顶部工具栏流程
- 分录编辑器
- 凭证号 / 状态 / 开始结束日期筛选
- 冲销关联展示
- 保存失败时后端错误透出
- 打印入口
- CSV / OCR 相关导入流

## 输出要求
1. 说明当前前端已实现能力与待补齐项
2. 输出列表页、详情/编辑页、分录编辑区的交互建议
3. 说明创建、编辑、提交、审核、过账、冲销、删除、打印等按钮的显隐与禁用规则
4. 说明前端即时校验与后端业务校验的边界
5. 说明驳回后重新编辑、已过账只读、冲销关联展示等状态驱动逻辑
6. 对 BUS 未明确项显式标记“待确认”
