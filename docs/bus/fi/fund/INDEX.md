# 资金管理业务文档索引（fund）

本目录按当前前后端代码中的真实菜单与路由整理。

## 当前代码中可确认的资金入口
- `001-fund-dispatch`：资金调度
- `002-fund-settlement`：资金结算
- `003-bill-management`：票据管理

## 当前代码状态
前端路由中，这 3 个入口当前都仍为占位页：
- `/funds`
- `/settlement`
- `/bills`

本轮代码检索中，未发现对应的前端独立实现页面，也未检到后端 controller / service 实现。

## 与旧目录的关系
旧 `fi/fund/README.md` 中提到的：
- `payment-order`
- `receipt-order`
- `bank-account`
- `fund-plan`

本轮未在当前代码中检到与之对应的真实页面与接口，因此不直接沿用为新结构主入口。
