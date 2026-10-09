# 往来域 Prompt 目录

本目录用于沉淀 `fi/ar` 域下基于真实代码整理的开发提示词。

当前已补充：

- `arap-doc/`：统一往来单模型
- `counterparty-credit/`：往来方信用配置

说明：

- `arap-doc` 当前同时承接 AR/AP 以及暂估、结算、付款申请、付款处理等细分类型。
- Prompt 设计时应优先复用现有实体 `BizfiFiArapDoc` 与 `BizfiFiCounterpartyCredit`，不要先拆成多套完全独立的模型。
