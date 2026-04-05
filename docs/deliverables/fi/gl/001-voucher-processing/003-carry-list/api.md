# 结转清单接口说明

## 1. 结转清单接口
- `GET /voucher/carry-list`

## 2. 查询参数
- `forg`：业务单元ID
- `period`：期间，建议格式 `yyyy-MM`

## 3. 返回结构
返回 `VoucherCarryListResultVO`，核心字段包括：
- `period`
- `periodSource`
- `baseCurrency`
- `currentPeriod`
- `periodStatus`
- `defaultVoucherType`
- `foundationHealthy`
- `periodVoucherCount`
- `carryVoucherCount`
- `periodVoucherAmount`
- `tasks`
- `relatedVouchers`
- `warnings`

## 4. 前端关联
- 前端 API：`voucherApi.fetchCarryList`
- 页面：`CarryForwardListView.vue`
- 辅助接口：`getBusinessUnitList`
- 查看凭证跳转：`/ledger/voucher`

## 5. 说明
- 当前场景只做检查与导航，不包含执行结转、结账或修改基础资料的接口
