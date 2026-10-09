# 接口说明：报表映射缺口连续治理队列

## 后端接口
本轮不新增后端接口。

## 前端路由参数
- `mode=resolve`：进入缺口治理模式。
- `accountCode`：当前治理科目编码。
- `reportType`：当前报表类型。
- `templateId`：当前报表模板 ID。
- `recommendedItemCode`：可靠推荐报表项目编码，可为空。
- `recommendationReason`：来源建议文案。
- `gapQueue`：精简缺口队列 JSON 字符串。
- `gapIndex`：当前缺口在队列中的 0 基下标。

## 精简队列字段
- `accountCode`
- `accountName`
- `reportType`
- `templateId`
- `templateName`
- `mappingType`
- `recommendedItemCode`
- `recommendationReason`
