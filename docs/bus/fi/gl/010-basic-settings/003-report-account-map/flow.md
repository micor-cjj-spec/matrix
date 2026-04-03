# 流程设计

## 查询流程
1. 前端先加载报表模板、报表项目、会计科目三类下拉。
2. 用户选择模板/项目/科目后调用 `GET /report-account-map/list`。
3. 后端按模板、项目、科目过滤，并按更新时间倒序返回分页数据。
4. 前端展示映射列表。

## 维护流程
1. 新增/编辑时维护模板、报表项目、会计科目、映射类型、生效期间、备注。
2. 前端调用 `POST /report-account-map` 或 `PUT /report-account-map`。
3. 删除时调用 `DELETE /report-account-map/{fid}`。
