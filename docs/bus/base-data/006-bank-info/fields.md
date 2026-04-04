# 字段设计

## 主体字段
| 字段 | 含义 |
|---|---|
| fid | 主键 |
| fname | 名称 |
| fcode | 编码 |
| fstatus | 状态：`DRAFT / SUBMITTED / AUDITED / REJECTED` |

## 页面状态字段
| 字段 | 含义 |
|---|---|
| selectedItem | 当前选中行 |
| loading | 列表加载状态 |
| dialog.mode | 创建或编辑 |
| dialog.valid | 表单校验状态 |
