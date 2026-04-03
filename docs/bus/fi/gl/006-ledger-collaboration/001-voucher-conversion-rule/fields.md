# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| docTypeRoot | 往来类型 | `ALL / AR / AP` |
| keyword | 关键字 | 匹配规则名、单据类型、科目 |

## 返回结构
返回 `Map<String,Object>`，核心字段包括：
- `docTypeRoot`
- `ruleCount`
- `auditedCount`
- `generatedCount`
- `pendingCount`
- `rows`
- `warnings`

## 规则行字段
| 字段 | 含义 |
|---|---|
| docTypeRoot | 往来类型 |
| docType | 单据类型 |
| ruleName | 规则名称 |
| summaryPrefix | 摘要前缀 |
| source | 规则来源，当前固定 `BUILTIN` |
| enabled | 是否启用 |
| debitAccountCode | 借方科目编码 |
| debitAccountName | 借方科目名称 |
| creditAccountCode | 贷方科目编码 |
| creditAccountName | 贷方科目名称 |
| docCount | 单据数 |
| auditedCount | 已审核及以上单据数 |
| generatedCount | 已关联凭证单据数 |
| pendingCount | 待生成单据数 |
| coverageRate | 覆盖率 |
| lastDocDate | 最近单据日期 |
| note | 说明 |
