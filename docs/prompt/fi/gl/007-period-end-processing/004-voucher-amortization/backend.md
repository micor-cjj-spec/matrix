# 凭证摊销后端提示词

## 目标
根据 `docs/bus/fi/gl/007-period-end-processing/004-voucher-amortization/` 下现有业务文档，输出凭证摊销场景的占位版后端提示词。

## 输入上下文
请仅结合以下已存在文档：
- `business.md`
- `manifest.json`

并严格遵守当前代码事实：
- 未发现后端 controller / service 实现
- 未发现任务编排或批处理实现
- 当前仅为菜单占位

## 输出要求
1. 明确写明“当前无正式后端实现可对齐”
2. 不虚构 service、controller、mapper、表结构或任务流
3. 输出后续后端落地时必须先明确的内容：摊销计划来源、摊销周期、剩余待摊余额、重复执行控制、批次记录、回滚策略
4. 所有未落地内容统一标记为“待实现 / 待确认”
