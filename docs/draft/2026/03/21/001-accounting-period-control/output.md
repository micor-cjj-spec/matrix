# AI 输出结果

## 初步设计建议

- 会计期间按 `forg + fperiod` 唯一维护
- `fperiod` 统一做 `yyyy-MM` 格式校验
- 新增时自动回填年度和月份
- 关账和反开走独立接口
- 期间日期范围默认取当月起止日

## 接口建议

- `POST /accounting-period`
- `PUT /accounting-period`
- `DELETE /accounting-period/{fid}`
- `POST /accounting-period/{fid}/close`
- `POST /accounting-period/{fid}/reopen`
- `GET /accounting-period/list`
