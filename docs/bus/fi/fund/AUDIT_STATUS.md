# 资金管理业务文档审计状态（fund）

> 审计基准：以 `dev` 分支当前前后端代码为准。

## 当前模块状态
- `001-fund-dispatch`：`not_implemented`
- `002-fund-settlement`：`not_implemented`
- `003-bill-management`：`not_implemented`

## 代码依据
前端路由当前仅存在 3 个占位入口：
- `/funds`：资金调度
- `/settlement`：资金结算
- `/bills`：票据管理

本轮代码检索中：
- 未发现对应前端独立实现页面
- 未发现对应后端 controller / service 实现

## 说明
旧 `fi/fund/README.md` 中保留了历史规划模块，但当前不作为新结构的代码对齐依据。
