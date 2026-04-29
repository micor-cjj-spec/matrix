# 前端开发提示词

在 `matrix-web` 中实现映射保存后的自动复核：

1. 报表科目映射页在 `mode=resolve` 且新增映射保存成功后，自动返回 `sourcePath`。
2. 返回来源报表时携带 `review=reportMappingResolution`、`resolvedAccountCode`、`resolvedAccountName`、`resolvedTemplateId`。
3. 企业纳税表读取复核参数，在数据加载完成后比对当前 `mappingGaps`。
4. 若目标缺口消失，展示成功提示；若仍存在，展示警告提示。
5. 普通映射新增、编辑、删除不触发自动跳转。
