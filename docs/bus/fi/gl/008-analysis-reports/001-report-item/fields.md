# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| page | 页码 | 默认 1 |
| size | 每页数量 | 默认 10 |
| ftemplateId | 报表模板ID | 精确匹配 |
| fcode | 项目编码 | 模糊匹配 |
| fname | 项目名称 | 模糊匹配 |

## 主体字段（BizfiFiReportItem）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| ftemplateId | 模板ID |
| fparentId | 上级项目ID |
| fcode | 项目编码 |
| fname | 项目名称 |
| frowNo | 行次 |
| flevel | 层级 |
| flineType | 行类型 |
| fperiodMode | 期间模式 |
| fsignRule | 正负号规则 |
| fdrillable | 可下钻 |
| feditableAdjustment | 是否可调整 |
| fsort | 排序 |
| fremark | 备注 |
| fcreatetime | 创建时间 |
| fupdatetime | 更新时间 |
