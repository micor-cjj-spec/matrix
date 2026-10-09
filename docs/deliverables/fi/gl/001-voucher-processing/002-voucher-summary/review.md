# 凭证汇总表评审清单

## 1. 一致性检查
- BUS 中“只读统计分析”定位是否与 prompt 一致
- 汇总字段口径是否与接口返回一致
- warnings 口径是否在前后端保持一致
- 查看凭证跳转参数是否与凭证列表页过滤参数一致

## 2. 已对齐项
- 页面定位为统计分析，不直接维护凭证
- 汇总接口为 `GET /voucher/summary`
- 前端页面为 `VoucherSummaryView.vue`
- 反查跳转为 `/ledger/voucher`

## 3. 待确认项
- `totalAmount` 与 `postedAmount` 的最终业务口径是否已完全固化
- warnings 的正式触发规则是否需要独立文档化

## 4. 后续建议
- 若后续增加导出或更多统计维度，应同步补充 BUS、prompt 与交付物
- 若跳转参数扩展，应优先补充接口说明与测试说明
