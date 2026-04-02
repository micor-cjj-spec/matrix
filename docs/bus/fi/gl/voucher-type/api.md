# 接口说明

## 基础接口
- `GET /voucher-type/{fid}`：查询详情。fileciteturn236file0L16-L18
- `POST /voucher-type`：新增凭证字。fileciteturn236file0L20-L22
- `PUT /voucher-type`：修改凭证字。fileciteturn236file0L24-L26
- `DELETE /voucher-type/{fid}`：删除凭证字。fileciteturn236file0L28-L30
- `GET /voucher-type/list`：分页查询。fileciteturn236file0L42-L55

## 状态接口
- `POST /voucher-type/{fid}/enable`：启用凭证字。fileciteturn236file0L32-L34
- `POST /voucher-type/{fid}/disable`：停用凭证字。fileciteturn236file0L36-L38

## 说明
- 当前 Controller 已覆盖配置维护主流程。
- 若后续需要增加“按组织查默认可用凭证字”之类能力，可在现有 Service 上继续扩展。
