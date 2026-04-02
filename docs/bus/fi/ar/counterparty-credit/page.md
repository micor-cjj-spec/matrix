# 页面说明

## 列表页
- 支持按往来方、AR/AP 根类型、启用状态查询。fileciteturn164file0L100-L102
- 展示信用额度、逾期阈值、硬拦截开关等核心字段。fileciteturn165file0L12-L22

## 详情页
- 展示配置基础信息、最近更新人和更新时间。fileciteturn165file0L20-L22

## 编辑页
- 维护 `counterparty`、`docTypeRoot`、`creditLimit`、`overdueDaysThreshold`、`enabled`、`blockOnOverLimit`、`blockOnOverdue` 等字段。fileciteturn165file0L12-L22
- `docTypeRoot` 建议使用枚举下拉，仅允许 `AR / AP`。

## 按钮与交互行为
- 列表页：新增、编辑、查看
- 编辑页：保存配置
- 分析页：结合信用预警接口查看风险命中结果。fileciteturn164file0L88-L99
