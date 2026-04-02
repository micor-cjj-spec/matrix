# AI 输出结果

## 初步设计建议

建议先做统一往来单 `BizfiFiArapDoc`：
- 用 `fdoctype` 区分 AR / AP 及后续细分类型
- 主表先承载基础字段和差异化字段
- 状态机先做 `DRAFT / SUBMITTED / AUDITED / REJECTED`
- Controller 先提供创建、更新、删除、提交、审核、驳回、详情、列表

## 预期收益

- 模型先统一，后续再分化
- 可以更快联动总账凭证
- 后续分页查询和分析能力更容易统一实现
