# 业务规则

## 查询规则
- `docTypeRoot` 只支持 `ALL / AR / AP`。
- `keyword` 对规则名、单据类型、借贷科目等字段做模糊匹配。

## 规则规则
- 当前规则页展示的是系统内置映射，不支持在线维护。
- 规则来源固定为 `BUILTIN`。
- 当前内置映射覆盖应收、暂估应收、应收结算、应付、暂估应付、付款申请、付款处理等典型单据。

## 覆盖率规则
- `coverageRate = generatedCount / auditedCount`。
- 当 `auditedCount = 0` 时，覆盖率显示 `0.00%`。
