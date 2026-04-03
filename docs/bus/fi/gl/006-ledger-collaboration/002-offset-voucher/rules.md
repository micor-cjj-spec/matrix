# 业务规则

## 查询规则
- 日期格式必须为 `yyyy-MM-dd`。
- `startDate` 为空默认当月第一天，`endDate` 为空默认当天。
- `matchStatus` 只支持 `ALL / PAIRED / ORPHAN`。

## 配对规则
- 优先依据备注中的原凭证ID匹配。
- 其次依据 `RV-原凭证号` 规则匹配。
- 再次依据原凭证备注中的“已冲销到凭证:xxx”匹配。
- 任一侧缺失都视为孤儿记录。

## 差额规则
- `amountDiff = originalAmount - reverseAmount`。
- 已配对但差额不为 0 时，仍需人工核查冲销金额是否正确。
