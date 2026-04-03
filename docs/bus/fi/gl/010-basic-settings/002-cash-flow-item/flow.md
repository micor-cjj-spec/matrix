# 流程设计

## 查询流程
1. 前端调用 `GET /cashflow-item/list`。
2. 后端按编码、名称、分类、状态过滤。
3. 按排序、主键升序返回分页数据。
4. 前端展示列表并同步一份全量数据作为上级项目选择来源。

## 维护流程
1. 新增/编辑时维护编码、名称、分类、方向、上级项目、排序、状态、备注。
2. 前端调用 `POST /cashflow-item` 或 `PUT /cashflow-item`。
3. 删除时调用 `DELETE /cashflow-item/{fid}`。
