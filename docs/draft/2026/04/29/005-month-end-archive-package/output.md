# 月结归档包草稿输出

## 功能范围

1. 新增月结归档包查询接口。
2. 汇总最新检查批次、关账执行记录、期间滚动记录。
3. 汇总准备度、阻塞项、预警项、凭证数量等关键指标。
4. 形成归档结论。
5. 输出月结里程碑。
6. 前端月结工作台新增归档包区域。

## 归档状态

- `NOT_STARTED`：未开始。
- `CHECKED`：已生成检查批次。
- `APPROVED_PENDING_CLOSE`：已批准待关账。
- `CLOSED`：已关账。
- `ROLLED`：已关账并启用下一期间。
- `BLOCKED`：存在阻塞项。

## 交付目录

- BUS：`docs/bus/fi/gl/007-period-end-processing/011-month-end-archive-package`
- Prompt：`docs/prompt/fi/gl/007-period-end-processing/011-month-end-archive-package`
- Deliverables：`docs/deliverables/fi/gl/007-period-end-processing/011-month-end-archive-package`
