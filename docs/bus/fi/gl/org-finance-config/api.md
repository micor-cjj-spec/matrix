# 接口说明

## 基础接口
- `GET /org-finance-config/{fid}`：按主键查询详情。fileciteturn237file0L16-L18
- `GET /org-finance-config/org/{forg}`：按组织查询配置。fileciteturn237file0L20-L22
- `POST /org-finance-config`：新增配置。fileciteturn237file0L24-L26
- `PUT /org-finance-config`：修改配置。fileciteturn237file0L28-L30
- `DELETE /org-finance-config/{fid}`：删除配置。fileciteturn237file0L32-L34
- `GET /org-finance-config/list`：分页查询。fileciteturn237file0L35-L44

## 说明
- 当前 Controller 已覆盖配置维护主流程。
- 若后续需要“只查询启用配置”或“切换配置状态”等能力，可在现有 Service 上扩展。
