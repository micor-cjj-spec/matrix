# 业务流程

## 财务任务执行

```text
Quartz触发
→ 创建execution和Outbox
→ RabbitMQ投递到fi-service
→ scheduler-client按executionNo幂等
→ Handler上报RUNNING和进度
→ 执行真实财务检查或报表汇总
→ 回调SUCCESS/FAILED
→ 前端展示结果、进度和错误
```

## 人工补偿

```text
管理员选择异常执行
→ 选择补偿动作
→ 填写必填原因
→ 服务端按当前状态做条件更新
→ 写入operation_log
→ 必要时创建新的重试execution
→ 刷新运行中心
```

## 告警处理

```text
定时扫描最终失败/超时/DEAD/执行器离线
→ 按dedupeKey防重复
→ 写入PENDING告警
→ 运行中心展示
→ 管理员确认
→ 状态改为ACKED并记录确认人
```

## 进度上报

进度只能单调增加。调度中心拒绝旧进度覆盖新进度；终态实例不再接受进度回调。
