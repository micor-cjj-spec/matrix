# 字段设计

## 主表字段

| 字段 | 含义 | 类型 | 是否必填 | 备注 |
|---|---|---|---|---|
| fid | 主键 | Long | 是 | |
| forg | 组织ID | Long | 是 | 同组织唯一 |
| fbaseCurrency | 本位币 | string | 否 | 默认 `CNY`fileciteturn240file0L99-L101 |
| fcurrentPeriod | 当前期间 | string | 否 | 格式 `yyyy-MM`fileciteturn240file0L102-L109 |
| fperiodControlMode | 期间控制模式 | string | 否 | STRICT / FLEXIBLEfileciteturn240file0L111-L118 |
| fdefaultVoucherType | 默认凭证字编码 | string | 否 | |
| fstatus | 状态 | string | 否 | ENABLED / DISABLEDfileciteturn240file0L120-L125 |
| fremark | 备注 | string | 否 | |
| fcreatetime | 创建时间 | datetime | 否 | |
| fupdatetime | 更新时间 | datetime | 否 | |

## 明细字段

当前代码模型为单表配置，无独立明细表。fileciteturn227file0L1-L27
