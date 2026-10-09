# 接口说明

## 关账前检查接口

### 请求

`GET /period-process/month-end-workbench`

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| forg | Long | 否 | 业务单元 ID |
| period | String | 否 | 会计期间，格式 `yyyy-MM` |

### 返回

返回 `ApiResponse<MonthEndWorkbenchResultVO>`。

核心结构：

- 主信息：`forg`、`period`、`periodSource`、`baseCurrency`、`currentPeriod`、`periodStatus`
- 准备度：`closeStatus`、`readinessScore`、`canClose`
- 统计：`blockingCount`、`warningCount`、`passedCount`、`totalCheckCount`
- 检查项：`checkItems`
- 月结步骤：`steps`
- 提示信息：`warnings`

## 接口边界

- 本接口只做查询和判断，不修改会计期间状态。
- 本接口不保存检查批次。
- 正式关账、反关账、审批流另行建模。

