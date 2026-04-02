# 流程设计

## 状态定义
- 已启用
- 已停用

## 状态流转
已启用 <-> 已停用

## 业务流程
1. 维护往来方信用配置，按 `counterparty + docTypeRoot` 唯一定位。fileciteturn167file0L231-L254
2. 保存时校验额度、根类型、逾期阈值等字段；未传默认值时由服务层补齐。fileciteturn167file0L215-L229
3. 后续往来单提交、审核前读取配置并执行风险判断。fileciteturn167file0L349-L360 fileciteturn167file0L372-L413
4. 若只命中预警，则保留预警结果；若命中硬拦截，则直接阻断业务操作。fileciteturn167file0L196-L211 fileciteturn167file0L390-L413

## 特殊说明
- 当前实现中没有独立 Controller，配置保存和查询入口挂在 `arap-doc` 控制器下。fileciteturn164file0L95-L103
- 当前“启用/停用”更多通过 `fenabled` 字段表达，而不是独立状态机字段。fileciteturn165file0L16-L22
