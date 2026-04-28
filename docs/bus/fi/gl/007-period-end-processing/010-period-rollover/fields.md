# 字段说明

## 期间滚动记录

| 字段 | 含义 |
| --- | --- |
| fid | 主键 |
| frolloverNo | 滚动编号 |
| fcloseExecutionId | 关账执行记录 ID |
| fcloseExecutionNo | 关账执行编号 |
| forg | 业务单元 |
| ffromPeriod | 原期间 |
| ftoPeriod | 下一期间 |
| fnextPeriodId | 下一会计期间 ID |
| fconfigId | 组织财务参数 ID |
| fbeforeCurrentPeriod | 滚动前当前期间 |
| fafterCurrentPeriod | 滚动后当前期间 |
| fcreatedNextPeriod | 是否自动创建下一期间 |
| frolloverStatus | 滚动状态 |
| foperator | 操作人 |
| fremark | 备注 |
| frolledTime | 滚动时间 |
| fcreatedTime | 创建时间 |

## 状态

`frolloverStatus` 当前支持 `SUCCESS`。
