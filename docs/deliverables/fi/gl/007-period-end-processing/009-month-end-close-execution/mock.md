# Mock 交付

## 执行成功返回

```json
{
  "execution": {
    "fid": 1,
    "fexecutionNo": "MECLOSE-20260428143000-1234",
    "fbatchId": 10,
    "fbatchNo": "MEC-20260428142000-1234-1-202604",
    "forg": 1,
    "fperiod": "2026-04",
    "fbeforeStatus": "OPEN",
    "fafterStatus": "CLOSED",
    "fexecutionStatus": "SUCCESS",
    "foperator": "WEB",
    "fexecutedTime": "2026-04-28T14:30:00"
  },
  "batch": {
    "fid": 10,
    "fapplicationStatus": "CLOSED",
    "fcloseStatus": "CLOSED"
  },
  "accountingPeriod": {
    "fid": 3,
    "fstatus": "CLOSED"
  },
  "workbench": {
    "canClose": true,
    "closeStatus": "READY"
  }
}
```
