# 往来自动核销评审清单

## 1. 一致性检查
- BUS 与 prompt 是否一致
- 方案号、日志号、核销链接数、核销总额、warnings 口径是否一致
- 条件带入、自动预览、执行自动核销、成功提示区展示是否一致
- 往来类型、往来方、统计日期与操作人参数口径是否一致

## 2. 已对齐项
- 页面定位为核销方案预览基础上的落库执行页
- 预览接口为 `GET /arap-manage/plan`
- 执行接口为 `POST /arap-manage/auto-writeoff`
- 页面为 `CounterpartyAutoWriteoffView.vue`
- 执行会写入 `BizfiFiArapWriteoffLog / BizfiFiArapWriteoffLink`

## 3. 待确认项
- 方案号生成规则是否需要独立文档化
- 成功提示区 `message` 与 `warnings` 的分工是否需要更细说明

## 4. 后续建议
- 若后续增加撤销核销或人工调整能力，应同步补接口与测试说明
- 若引入批量执行优化或缓存，应同步更新业务口径与 SQL 说明
