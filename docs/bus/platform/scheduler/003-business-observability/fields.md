# 字段设计

## 执行进度字段

| 字段 | 含义 |
|---|---|
| fprogress | 0-100 的执行进度 |
| fcurrent_stage | 当前执行阶段编码 |
| fprogress_message | 当前阶段说明 |
| flast_progress_time | 最近一次进度上报时间 |

## 人工操作日志

| 字段 | 含义 |
|---|---|
| fexecution_no | 被操作的执行编号 |
| faction | RETRY_NOW / STOP_RETRY / CANCEL / SKIP / MARK_SUCCESS |
| foperator_id | 操作人 |
| freason | 必填操作原因 |
| ffrom_status | 操作前状态 |
| fto_status | 操作后状态 |
| fcreate_time | 操作时间 |

## 告警记录

| 字段 | 含义 |
|---|---|
| fdedupe_key | 告警去重键 |
| fexecution_no | 关联执行编号，可空 |
| fjob_id | 关联任务ID，可空 |
| fexecutor_code | 关联执行器 |
| falert_type | 告警类型 |
| flevel | WARN / ERROR / CRITICAL |
| fstatus | PENDING / ACKED |
| fack_by / fack_time | 确认人和确认时间 |

## 财务报表快照

`matrix_fi_scheduler_report_snapshot` 保存执行编号、报表类型、期间、账簿、状态和汇总 JSON；执行编号唯一，重复回调不会产生重复快照。
