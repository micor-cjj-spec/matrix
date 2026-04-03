# 接口说明

## 查询接口
- `GET /arap-manage/multi-analysis`

## 查询参数
- `docTypeRoot`
- `groupDimension`
- `asOfDate`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `groupDimension`
- `groupCount`
- `totalAmount`
- `writtenOffAmount`
- `openAmount`

## 前端关联
- 前端 API：`arapManageApi.fetchMultiAnalysis`
- 前端页面：`CounterpartyMultiAnalysisView.vue`
