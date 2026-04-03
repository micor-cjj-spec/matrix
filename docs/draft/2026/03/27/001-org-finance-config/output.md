# AI 输出结果

## 初步设计建议

- 组织财务配置按 `forg` 唯一维护
- 本位币默认 `CNY`
- 当前期间格式统一为 `yyyy-MM`
- 期间控制模式先支持 `STRICT / FLEXIBLE`
- 默认凭证字只存编码，具体配置到凭证字表查询

## 接口建议

- `GET /org-finance-config/org/{forg}`
- `POST /org-finance-config`
- `PUT /org-finance-config`
- `DELETE /org-finance-config/{fid}`
- `GET /org-finance-config/list`
