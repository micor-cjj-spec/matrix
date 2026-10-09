# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| page | 页码 | 默认 1 |
| size | 每页数量 | 默认 10 |
| fcode | 科目编码 | 模糊匹配 |
| fname | 科目名称 | 模糊匹配 |
| forg | 业务单元ID | 精确匹配 |
| ftype | 科目类型 | 精确匹配 |
| fpltype | 损益类型 | 精确匹配 |

## 主体字段（BizfiFiAccount）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fcode | 科目编码 |
| fname | 科目名称 |
| forg | 业务单元ID |
| flongName | 科目全称 |
| ftype | 科目类型 |
| fparent | 上级科目ID |
| fpltype | 损益类型 |
| fdirection | 余额方向 |
| fisDetail | 是否明细科目 |
| freportItem | 关联报表项目ID |
| flevel1 | 一级科目ID |
| fentryControl | 分录控制 |
| fcontrolLevel | 控制级别 |
| fallowChild | 是否允许子科目 |
| fmanualEntry | 是否手工录入 |
| fcash | 是否现金科目 |
| fbank | 是否银行科目 |
| fequivalent | 是否现金等价物 |
| fisEntry | 是否允许凭证分录使用 |
| fnotice | 是否往来通知 |
| fexchange | 是否外币科目 |
| fqtyAccounting | 是否数量核算 |
| fcreatetime | 创建时间 |
| fupdatetime | 更新时间 |
