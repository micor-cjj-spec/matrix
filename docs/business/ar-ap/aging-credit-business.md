# AR/AP 账龄分析与信用控制（预警版）

> 更新时间：2026-03-13

## 1. 目标
- 提供 AR/AP 在途单据的账龄分布，辅助催收与付款计划。
- 提供往来方信用预警（额度超限、逾期天数超阈值）。

## 2. 统计口径（MVP）
- 数据范围：`fdoctype` 前缀为 `AR` 或 `AP` 的单据。
- 未结口径：状态为 `SUBMITTED`、`AUDITED`。
- 账龄天数：`asOfDate - fdate`，负值按 0 处理。
- 分桶：`0-30`、`31-60`、`61-90`、`91+`。

## 3. 信用控制口径（预警版）
- 配置维度：`往来方 + AR/AP`。
- 预警规则：
  1) `totalOutstanding > creditLimit` 触发超限预警
  2) `maxOverdueDays > overdueDaysThreshold` 触发逾期预警

## 4. 新增接口
- `GET /arap-doc/aging/summary?docTypeRoot=AR&asOfDate=2026-03-13`
- `POST /arap-doc/credit/config`
- `GET /arap-doc/credit/config/list?docTypeRoot=AR`
- `GET /arap-doc/credit/warnings?docTypeRoot=AR&asOfDate=2026-03-13`

## 5. 新增表
- `bizfi_fi_counterparty_credit`（见 `docs/sql/fi-counterparty-credit.sql`）
