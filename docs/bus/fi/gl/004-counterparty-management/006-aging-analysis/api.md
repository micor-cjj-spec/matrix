# 接口说明

## 查询接口
- `GET /arap-manage/aging-analysis`

## 查询参数
- `docTypeRoot`
- `asOfDate`

## 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `counterpartyCount`
- `warningCount`
- `totalOpenAmount`

## 前端关联
- 前端 API：`arapManageApi.fetchAgingAnalysis`
- 前端页面：`CounterpartyAgingAnalysisView.vue`
