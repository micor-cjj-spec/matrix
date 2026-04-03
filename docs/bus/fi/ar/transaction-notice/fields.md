# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| noticeNo | 通知单编号 | string | 是 | 系统生成 |
| counterpartyType | 往来方类型 | string | 是 | 客户/供应商 |
| counterpartyCode | 往来方编码 | string | 是 | |
| counterpartyName | 往来方名称 | string | 是 | |
| noticeDate | 通知日期 | date | 是 | |
| totalBalance | 往来余额 | decimal | 否 | |
| dueAmount | 到期金额 | decimal | 否 | |
| overdueAmount | 逾期金额 | decimal | 否 | |
| status | 状态 | string | 是 | 草稿、已发送、已确认、已归档 |
| remark | 备注 | string | 否 | |

## 明细字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| itemType | 项目类型 | string | 否 | 余额/到期/逾期 |
| itemDate | 业务日期 | date | 否 | |
| itemAmount | 金额 | decimal | 否 | |
| itemDesc | 说明 | string | 否 | |
