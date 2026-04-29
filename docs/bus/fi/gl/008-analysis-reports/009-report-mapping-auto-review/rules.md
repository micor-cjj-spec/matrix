# 规则说明

## 复核参数
- `review=reportMappingResolution`：触发映射治理复核提示。
- `resolvedAccountCode`：本次治理的会计科目编码。
- `resolvedAccountName`：本次治理的会计科目名称。
- `resolvedTemplateId`：本次治理的报表模板。

## 复核判断
- 若当前 `mappingGaps` 中不存在同一会计科目和模板，则提示“复核通过”。
- 若当前 `mappingGaps` 中仍存在同一会计科目和模板，则提示“仍待处理”。
- 若缺少会计科目参数，则按当前 `mappingGaps` 是否为空给出通用提示。
