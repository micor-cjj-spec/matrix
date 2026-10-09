# 结转清单口径说明

## 1. 查询条件口径
- `forg`：业务单元 ID
- `period`：期间，建议格式 `yyyy-MM`

## 2. 元信息区口径
- `baseCurrency`：本位币
- `currentPeriod`：当前期间
- `periodSource`：期间来源
- `periodStatus`：期间状态
- `defaultVoucherType`：默认凭证字
- `foundationHealthy`：基础资料健康度

## 3. 汇总卡片口径
- `periodVoucherCount`：本期凭证数
- `carryVoucherCount`：结转相关凭证数
- `periodVoucherAmount`：本期凭证金额
- 检查完成度：根据后端检查结果汇总展示

## 4. 检查清单与相关凭证口径
- `tasks`：期末处理前置检查项
- `relatedVouchers`：当前期间识别到的结转相关凭证
- `warnings`：需要额外关注的提示信息

## 5. 待确认项
- 基础资料健康度的最终计算规则
- 检查完成度的展示口径是否需要单独固化
