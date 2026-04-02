# AI 输出结果

## 初步设计建议

- Service 层统一收口 `submit / audit / reject`
- 增加 `submitByNumber / auditByNumber / rejectByNumber`
- 状态校验放在 Service 内，不要下放到 Controller
- 驳回后允许再次编辑并重新提交

## 接口建议

- `POST /arap-doc/submit/{fid}`
- `POST /arap-doc/submit/by-number`
- `POST /arap-doc/audit/{fid}`
- `POST /arap-doc/audit/by-number`
- `POST /arap-doc/reject/{fid}`
- `POST /arap-doc/reject/by-number`
