# API

## 查询月结归档包

`GET /month-end-archive/package?forg=1&period=2026-04`

返回：

```json
{
  "forg": 1,
  "period": "2026-04",
  "archiveStatus": "ROLLED",
  "conclusion": "本期已完成关账并启用下一期间 2026-05。",
  "readinessScore": 100,
  "closeExecuted": true,
  "periodRolled": true,
  "milestones": []
}
```
