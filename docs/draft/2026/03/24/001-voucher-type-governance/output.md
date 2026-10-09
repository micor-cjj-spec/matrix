# AI 输出结果

## 初步设计建议

- 凭证字按 `forg + fcode` 唯一维护
- 保存时编码统一转大写
- 状态先只做 `ENABLED / DISABLED`
- 默认凭证字不要直接写死在代码里，放到组织财务配置里引用

## 接口建议

- `POST /voucher-type`
- `PUT /voucher-type`
- `DELETE /voucher-type/{fid}`
- `POST /voucher-type/{fid}/enable`
- `POST /voucher-type/{fid}/disable`
- `GET /voucher-type/list`
