# 凭证交付物说明

## 1. 对应 BUS 场景
- BUS 路径：`docs/bus/fi/gl/001-voucher-processing/001-voucher/`
- 业务名称：凭证
- 业务定位：总账核心核算单据，支持新增、查看、提交、审核、过账、冲销等主流程。

## 2. 对应 Prompt 来源
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/frontend.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/sql.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/test.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/review.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/ops.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/mock.md`

## 3. 交付物清单
- `dictionary.md`：字段、状态、校验与统计口径说明
- `api.md`：接口契约说明
- `sql.md`：表结构、索引与迁移说明
- `test.md`：测试验证说明
- `ops.md`：发布运维说明
- `review.md`：一致性评审清单
- `mock.md`：样例与联调数据说明

## 4. 使用顺序
1. 先阅读 `dictionary.md` 明确语义与状态口径
2. 再看 `api.md`、`sql.md` 对齐接口与数据结构
3. 开发或联调前参考 `mock.md`
4. 提测前执行 `test.md` 验证
5. 发布前检查 `review.md` 和 `ops.md`

## 5. 适用环境
- 当前 `dev` 分支已实现凭证主流程的场景
- 适用于列表、明细、分录编辑、提交流转、过账冲销与批量过账相关验证

## 6. 风险与注意事项
- 借贷平衡、期间校验、状态越权、精度规则需以后端口径为准
- 前端按钮显隐与禁用规则必须和后端状态机保持一致
- 冲销关联展示与后端反向凭证生成口径必须保持一致
- CSV/OCR 相关流若作为扩展能力，不能反向污染主流程定义

## 7. 回滚与兜底
- 若状态流转规则调整失败，优先回滚到已验证的状态机口径
- 若精度或过账逻辑异常，优先停用相关扩展能力，保留核心查询和查看能力
- 若冲销关联异常，先保留查看能力并暂停关联跳转或扩展展示
