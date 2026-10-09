# 往来核销日志评审清单

## 1. 一致性检查
- BUS 与 prompt 是否一致
- 批次数、链接数、核销金额、操作时间、warnings 口径是否一致
- 查询、批次表、明细表、查看明细联动是否一致
- 往来类型、往来方、方案号、开始日期、结束日期过滤口径是否一致

## 2. 已对齐项
- 页面定位为自动核销结果的审计追踪页面
- 接口为 `GET /arap-manage/writeoff-log`
- 页面为 `CounterpartyWriteoffLogView.vue`
- 数据来源为 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`

## 3. 待确认项
- 明细查看状态规则是否需要独立文档化
- warnings 的正式触发规则是否需要更细说明

## 4. 后续建议
- 若后续增加日志导出或撤销能力，应同步补接口与测试说明
- 若引入缓存或预汇总摘要，应同步更新业务口径与 SQL 说明
