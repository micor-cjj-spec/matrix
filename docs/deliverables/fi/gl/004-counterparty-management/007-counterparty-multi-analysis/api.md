# 往来多维分析表接口说明

## 1. 查询接口
- `GET /arap-manage/multi-analysis`

## 2. 查询参数
- `docTypeRoot`
- `groupDimension`
- `asOfDate`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `groupDimension`
- `groupCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`

## 4. 前端关联
- 前端 API：`arapManageApi.fetchMultiAnalysis`
- 前端页面：`CounterpartyMultiAnalysisView.vue`

## 5. 说明
- 当前场景为查询类汇总分析页，不包含执行类接口
- 返回重点在多维聚合结果、warnings 与汇总统计字段
