# 接口说明

## 结转清单接口
- `GET /voucher/carry-list`

## 查询参数
- `forg`：业务单元ID
- `period`：期间，建议格式 `yyyy-MM`

## 返回结构
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

## 前端关联
- 前端 API：`voucherApi.fetchCarryList`
- 页面：`CarryForwardListView.vue`
- 辅助接口：页面初始化会额外调用 `getBusinessUnitList` 加载业务单元下拉
