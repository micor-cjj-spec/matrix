# 字段设计

## 查询参数
| 参数 | 含义 | 说明 |
|---|---|---|
| page | 页码 | 默认 1 |
| size | 每页数量 | 默认 10 |
| fcode | 项目编码 | 模糊匹配 |
| fname | 项目名称 | 模糊匹配 |
| fcategory | 分类 | `OPERATING / INVESTING / FINANCING` |
| fstatus | 状态 | `ENABLED / DISABLED` |

## 主体字段（BizfiFiCashflowItem）
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fcode | 项目编码 |
| fname | 项目名称 |
| fparentId | 上级项目ID |
| fcategory | 分类 |
| fdirection | 方向：`BOTH / IN / OUT` |
| fsort | 排序 |
| fstatus | 状态：`ENABLED / DISABLED` |
| fremark | 备注 |
| fcreatetime | 创建时间 |
| fupdatetime | 更新时间 |
