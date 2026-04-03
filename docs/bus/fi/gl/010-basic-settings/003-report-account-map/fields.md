# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| page | 页码 | 默认 1 |
| size | 每页数量 | 默认 10 |
| ftemplateId | 报表模板ID | 精确匹配 |
| fitemId | 报表项目ID | 精确匹配 |
| faccountId | 会计科目ID | 精确匹配 |

## 主体字段（BizfiFiReportAccountMap）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| ftemplateId | 报表模板ID |
| fitemId | 报表项目ID |
| faccountId | 会计科目ID |
| fmappingType | 映射类型：`DIRECT / PL / CASHFLOW` |
| feffectiveFrom | 生效开始期间 |
| feffectiveTo | 生效结束期间 |
| fremark | 备注 |
| fcreatetime | 创建时间 |
| fupdatetime | 更新时间 |
