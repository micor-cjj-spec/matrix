# 凭证SQL提示词

## 目标
根据 `docs/bus/fi/gl/001-voucher-processing/001-voucher/` 下业务文档，以及当前已实现代码，输出凭证场景的 SQL 设计与交付提示词，用于生成表结构、变更脚本、初始化数据与修复脚本。

## 输入上下文
请结合以下文档：
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `api.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/dictionary.md`
- `docs/prompt/fi/gl/001-voucher-processing/001-voucher/backend.md`

同时结合当前已实现能力：凭证头/分录、借贷平衡、过账生成总账分录、批量过账、精度规则、冲销关联展示、日期区间筛选。

## 输出要求
1. 输出凭证头、凭证明细、过账关联、冲销关联等核心表设计建议
2. 输出状态字段、审计字段、精度字段、来源追溯字段建议
3. 输出索引与查询优化建议
4. 输出迁移、回滚、初始化数据依赖说明
5. 对 BUS 未明确项显式标记“待确认”
