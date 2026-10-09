# 往来多维分析表评审清单

## 1. 一致性检查
- BUS 与 prompt 是否一致
- groupDimension、原额、已核销、未核销、warnings 口径是否一致
- 查询、表格展示、只读分析边界是否一致
- 往来类型、汇总维度、统计日期过滤口径是否一致

## 2. 已对齐项
- 页面定位为往来管理汇总分析页
- 接口为 `GET /arap-manage/multi-analysis`
- 页面为 `CounterpartyMultiAnalysisView.vue`
- 数据来源为往来单据和已落库核销链接

## 3. 待确认项
- groupDimension 枚举是否需要独立文档化
- warnings 的正式触发规则是否需要更细说明

## 4. 后续建议
- 若后续增加导出或进一步下钻能力，应同步补接口与测试说明
- 若引入预计算分析结果，应同步更新业务口径与 SQL 说明
