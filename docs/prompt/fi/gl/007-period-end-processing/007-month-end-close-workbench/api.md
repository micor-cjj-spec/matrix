# 月结工作台接口提示词

## 目标

根据 BUS 文档生成关账前检查接口说明，并保证前后端字段一致。

## 接口

`GET /period-process/month-end-workbench`

## 参数

- `forg`: 业务单元 ID，可选
- `period`: 会计期间，可选，格式 `yyyy-MM`

## 返回约束

返回 `ApiResponse<MonthEndWorkbenchResultVO>`，必须包含：

- `closeStatus`
- `readinessScore`
- `canClose`
- `checkItems`
- `steps`
- `warnings`

## 注意

本接口首版只读，禁止在接口中执行正式关账。

