# 财务月结工作台接口交付

## 接口

`GET /period-process/month-end-workbench`

## 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| forg | Long | 否 | 业务单元 ID |
| period | String | 否 | 会计期间，格式 `yyyy-MM` |

## 返回对象

`ApiResponse<MonthEndWorkbenchResultVO>`

关键字段：

- `closeStatus`：`READY` / `WARNING` / `BLOCKED` / `CLOSED`
- `readinessScore`：准备度 0-100
- `canClose`：是否建议进入关账
- `checkItems`：关账前检查项
- `steps`：月结步骤
- `warnings`：阻塞与预警提示

## 接口性质

只读查询接口，不修改会计期间状态。

