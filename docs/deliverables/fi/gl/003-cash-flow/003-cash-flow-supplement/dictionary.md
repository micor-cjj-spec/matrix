# 现金流量补充资料口径说明

## 1. 查询条件口径
- `orgId`：业务单元
- `period`：期间
- `currency`：币种

## 2. 汇总统计口径
- `postedVoucherCount`：已过账凭证数
- `cashVoucherCount`：现金相关凭证数
- `cashAccountCount`：现金类科目数
- `cashflowItemCount`：现金流项目数
- `directCount`：直接标记数
- `heuristicCount`：规则推断数
- `unknownCount`：未知编码数
- `mixedCount`：多编码数
- `transferCount`：现金划转数
- `cashInAmount`：现金流入
- `cashOutAmount`：现金流出
- `netAmount`：净额

## 3. 页面区域口径
- `tasks`：检查项/待处理任务清单
- `categories`：分类分布桶
- `pendingVouchers`：待补录/复核凭证
- `warnings`：需要重点关注的提示信息

## 4. 跳转口径
- “查看凭证”跳转到凭证页面并带凭证号

## 5. 待确认项
- 多编码与待补录边界是否需要更细业务定义
- warnings 的正式触发规则是否需要单独固化
