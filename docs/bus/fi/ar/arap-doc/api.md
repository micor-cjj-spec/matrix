# 接口说明

## 基础单据接口
- `POST /arap-doc`：创建单据草稿。fileciteturn164file0L16-L19
- `PUT /arap-doc`：更新草稿或驳回单据。fileciteturn164file0L20-L21
- `DELETE /arap-doc/{fid}`：删除草稿。fileciteturn164file0L22-L23
- `GET /arap-doc/{fid}`：查询详情。fileciteturn164file0L56-L57
- `GET /arap-doc/list`：分页查询。fileciteturn164file0L58-L68

## 提交流程接口
- `POST /arap-doc/submit/{fid}`：按主键提交。fileciteturn164file0L24-L25
- `POST /arap-doc/submit/by-number`：按单据号提交。fileciteturn164file0L27-L30
- `POST /arap-doc/audit/{fid}`：按主键审核。fileciteturn164file0L32-L34
- `POST /arap-doc/audit/by-number`：按单据号审核。fileciteturn164file0L36-L40
- `POST /arap-doc/reject/{fid}`：按主键驳回。fileciteturn164file0L42-L44
- `POST /arap-doc/reject/by-number`：按单据号驳回。fileciteturn164file0L46-L50

## 凭证联动接口
- `POST /arap-doc/voucher/{fid}`：按主键生成凭证。fileciteturn164file0L70-L73
- `POST /arap-doc/voucher/by-number`：按单据号生成凭证。fileciteturn164file0L75-L79
- `GET /arap-doc/by-voucher`：按凭证反查关联单据。fileciteturn164file0L75-L79

## 分析接口
- `GET /arap-doc/aging/summary`：账龄汇总分析。fileciteturn164file0L81-L86
- `GET /arap-doc/credit/warnings`：信用预警分析。fileciteturn164file0L88-L93

## 信用配置接口
- `POST /arap-doc/credit/config`：保存往来方信用配置。fileciteturn164file0L95-L99
- `GET /arap-doc/credit/config/list`：查询信用配置列表。fileciteturn164file0L100-L102
