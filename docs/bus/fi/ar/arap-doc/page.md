# 页面说明

## 列表页
- 支持按 `docType`、单据号、状态、往来方、日期区间、金额区间分页查询。fileciteturn164file0L58-L68
- 支持按凭证 ID 或凭证号反查相关单据。fileciteturn164file0L75-L79

## 详情页
- 展示基础字段、差异化业务字段、审核信息、凭证关联信息。字段来源于 `BizfiFiArapDoc` 单表模型。fileciteturn162file0L1-L36

## 编辑页
- 草稿和驳回状态允许编辑。fileciteturn167file0L54-L61
- 已提交、已审核通常仅支持查看；驳回后允许修改并再次提交。fileciteturn167file0L349-L370

## 按钮与交互行为
- 草稿态：保存、提交、删除
- 已提交：审核、驳回、查看
- 已驳回：编辑、再次提交
- 已审核：查看、按需查看凭证关联结果

## 分析页/配置页
- 账龄汇总页：按 AR/AP 根类型输出账龄桶统计。fileciteturn164file0L81-L86
- 信用预警页：展示命中额度/逾期规则的往来方预警。fileciteturn164file0L88-L93
- 信用配置页：维护往来方信用参数。fileciteturn164file0L95-L103
