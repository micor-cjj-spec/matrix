# 接口说明

接口前缀：`/voucher`

## 前端已接入接口
- `GET /voucher/list`：列表查询
- `GET /voucher/summary`：凭证汇总表统计
- `GET /voucher/carry-list`：结转清单
- `POST /voucher`：新增草稿凭证
- `PUT /voucher`：编辑草稿/驳回凭证
- `DELETE /voucher/{fid}`：删除草稿凭证
- `POST /voucher/submit/{fid}`：提交
- `POST /voucher/audit/{fid}`：审核
- `POST /voucher/post/{fid}`：过账
- `POST /voucher/reject/{fid}`：驳回
- `POST /voucher/reverse/{fid}`：冲销
- `GET /voucher/{fid}/lines`：查询分录
- `PUT /voucher/{fid}/lines`：保存分录
- `POST /voucher/import/ocr/parse`：OCR解析
- `POST /voucher/import/ocr/confirm`：OCR确认导入

## 后端已提供但前端当前未接入接口
- `POST /voucher/save`：保存草稿
- `POST /voucher/post/batch`：批量过账
- `GET /voucher/{fid}`：详情查询
- `GET /voucher/{fid}/source-docs`：来源单据查询

## 主要查询参数
### `GET /voucher/list`
- `page`
- `size`
- `number`
- `summary`
- `status`
- `startDate`
- `endDate`

### `GET /voucher/summary`
- `startDate`
- `endDate`
- `status`
- `summaryKeyword`

### `GET /voucher/carry-list`
- `forg`
- `period`

## 主要请求体
### 新增/编辑凭证
请求体为 `BizfiFiVoucher`，前端当前主要传：
- `fid`（编辑时）
- `fnumber`
- `fdate`
- `fsummary`
- `famount`
- `fremark`

### 保存分录
请求体为 `List<BizfiFiVoucherLine>`，前端当前主要传：
- `flineNo`
- `faccountCode`
- `fsummary`
- `fdebitAmount`
- `fcreditAmount`

### OCR确认导入
请求体为 `BizfiFiVoucherOcrImportRequest`：
- `voucher`
- `lines`
