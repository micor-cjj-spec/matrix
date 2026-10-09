# 财务域 Prompt 目录

本目录用于沉淀基于现有代码结构整理出的开发提示词。

当前优先补充已确认有后端代码落点的对象：

- `fi/gl/voucher/`：凭证
- `fi/ar/arap-doc/`：往来单
- `fi/ar/counterparty-credit/`：往来方信用配置

说明：

1. Prompt 以现有 `fi-service` 中真实存在的实体、Controller、Service 为基础编写。
2. 当前阶段暂不补 `sql.md`。
3. 后续如果在 `matrix-web` 中确认有对应页面代码，可继续细化前端 Prompt。
