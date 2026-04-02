# 往来单前端开发提示词

## 目标
为统一往来单模型设计前端页面，页面需要适配 AR/AP 及其细分类型，而不是每种单据都做一套完全独立的页面。

## 必须参考的文档和代码
- `docs/bus/fi/ar/arap-doc/business.md`
- `docs/bus/fi/ar/arap-doc/fields.md`
- `docs/bus/fi/ar/arap-doc/flow.md`
- `docs/bus/fi/ar/arap-doc/rules.md`
- `docs/bus/fi/ar/arap-doc/api.md`
- `fi-service/src/main/java/single/cjj/fi/ar/controller/BizfiFiArapDocController.java`

## 页面目标
1. 支持列表页、详情页、编辑页
2. 支持按 `docType` 切换不同业务场景展示重点字段
3. 支持提交、审核、驳回、查看凭证关联结果
4. 支持账龄汇总和信用预警页面
5. 支持信用配置维护页面

## 设计要求
1. 页面字段必须和现有后端实体字段、接口参数保持一致。
2. 状态按钮严格按照后端状态机控制。
3. 审批类按钮需要按当前状态动态显隐。
4. `AR / AP / AR_SETTLEMENT / AP_PAYMENT_APPLY` 等类型可以共用页面骨架，但要允许字段动态配置。
5. 列表查询条件要覆盖单据号、状态、往来方、日期区间、金额区间等。

## 输出要求
请输出：
1. 页面结构设计
2. 组件拆分建议
3. 路由设计
4. 接口调用清单
5. 状态按钮控制表
6. 关键表单字段与校验说明
