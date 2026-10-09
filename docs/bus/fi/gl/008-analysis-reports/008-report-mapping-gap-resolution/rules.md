# 规则说明

## 路由参数
- `mode=resolve`：表示从缺口治理入口进入。
- `accountCode`：待治理会计科目编码。
- `reportType`：目标报表类型。
- `templateId`：目标报表模板。
- `sourcePath`：来源报表路径。
- `sourcePeriod`：来源期间。
- `sourceCurrency`：来源币种。
- `sourceOrgId`：来源业务单元。

## 推荐报表项目
- 若会计科目维护了 `freportItem`，且该项目属于当前模板，则优先使用。
- 利润表按收入/成本/费用属性尝试推荐 `PL_REVENUE` 或 `PL_COST`。
- 资产负债表按现金类、资产类、负债/权益类尝试推荐对应项目。
- 无可靠结果时不推荐，要求用户人工选择。
