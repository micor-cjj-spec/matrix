# 接口说明

## 配置维护接口
- `POST /arap-doc/credit/config`：保存信用配置。当前实现支持新增或按 `counterparty + docTypeRoot` 更新已有配置。fileciteturn164file0L95-L99 fileciteturn167file0L215-L254
- `GET /arap-doc/credit/config/list`：查询指定根类型下的信用配置列表。fileciteturn164file0L100-L102

## 预警分析接口
- `GET /arap-doc/credit/warnings`：返回命中额度/逾期规则的预警结果列表。fileciteturn164file0L88-L93

## 隐式联动接口
- `POST /arap-doc/submit/{fid}` / `POST /arap-doc/submit/by-number`：提交前执行信用拦截校验。fileciteturn164file0L24-L30 fileciteturn167file0L349-L354
- `POST /arap-doc/audit/{fid}` / `POST /arap-doc/audit/by-number`：审核前执行信用拦截校验。fileciteturn164file0L32-L40 fileciteturn167file0L356-L363
