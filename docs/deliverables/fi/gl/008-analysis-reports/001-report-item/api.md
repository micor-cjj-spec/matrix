# 报表项目接口说明

## 1. 查询接口
- `GET /report-item/list`
- `GET /report-item/tree`
- `GET /report-item/{fid}`

## 2. 写接口
- `POST /report-item`
- `PUT /report-item`
- `DELETE /report-item/{fid}`

## 3. 前端关联
- 前端 API：`reportItemApi`
- 前端 API：`reportTemplateApi`
- 前端页面：`ReportItemView.vue`

## 4. 当前页面接入情况
- 已接入：模板加载、分页列表查询、查询重置、刷新
- 未接入：新增、编辑、删除、树查询显式展示、详情显式展示

## 5. 说明
- 后端已具备基础维护接口能力
- 当前前端页面定位仍以查询展示为主
