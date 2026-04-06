# 现金流量查询口径说明

## 1. 查询条件口径
- `orgId`：业务单元
- `period`：期间
- `currency`：币种
- `cashflowItemCode`：现金流项目
- `categoryCode`：活动分类
- `sourceType`：识别方式
- `accountCode`：科目编码关键字
- `keyword`：关键字

## 2. 汇总统计口径
- `postedVoucherCount`：已过账凭证数
- `cashVoucherCount`：现金相关凭证数
- `directCount`：直接标记数
- `heuristicCount`：规则推断数
- `unknownCount`：未知编码数
- `mixedCount`：多编码复核数
- `transferCount`：现金划转数
- `cashInAmount`：现金流入
- `cashOutAmount`：现金流出
- `netAmount`：净额

## 3. 识别来源口径
- 直接标记
- 规则推断
- 未知编码
- 多编码复核
- 现金划转

## 4. warnings 与跳转口径
- warnings 由后端返回
- “查看凭证”跳转到凭证页面并带凭证号

## 5. 待确认项
- 多编码复核的最终判定边界是否需要更细说明
- warnings 的正式触发规则是否需要单独固化
