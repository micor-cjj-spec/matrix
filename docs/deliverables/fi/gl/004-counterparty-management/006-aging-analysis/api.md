# 账龄分析表接口说明

## 1. 查询接口
- `GET /arap-manage/aging-analysis`

## 2. 查询参数
- `docTypeRoot`
- `asOfDate`

## 3. 返回结构
返回 `Map<String,Object>`：
- `rows`
- `warnings`
- `counterpartyCount`
- `warningCount`
- `totalOpenAmount`

## 4. 前端关联
- 前端 API：`arapManageApi.fetchAgingAnalysis`
- 前端页面：`CounterpartyAgingAnalysisView.vue`

## 5. 说明
- 当前场景为查询类分析报表，不包含执行类接口
- 返回重点在账龄分析结果、warnings 与汇总统计字段
