# 字段说明

## 执行记录

| 字段 | 含义 |
| --- | --- |
| fid | 主键 |
| fexecutionNo | 关账执行号 |
| fbatchId | 月结检查批次 ID |
| fbatchNo | 月结检查批次号 |
| forg | 业务单元 |
| fperiod | 会计期间 |
| fperiodId | 会计期间 ID |
| fbeforeStatus | 执行前期间状态 |
| fafterStatus | 执行后期间状态 |
| fexecutionStatus | 执行状态，当前为 `SUCCESS` |
| fcheckSnapshotJson | 执行前实时检查快照 |
| foperator | 执行人 |
| fremark | 备注 |
| fexecutedTime | 执行时间 |
| fcreatedTime | 记录创建时间 |

## 批次状态补充

`fapplicationStatus` 新增 `CLOSED`，表示该检查批次已完成正式关账。
