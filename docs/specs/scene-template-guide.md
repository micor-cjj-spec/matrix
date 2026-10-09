# 业务场景目录模板说明

## 1. 目标

本文档用于定义单个业务场景在 `docs/bus/`、`docs/prompt/` 与 `docs/deliverables/` 下的推荐目录模板，便于后续快速新增场景并保持结构统一。

---

## 2. 推荐目录模板

```text
docs/
├── bus/
│   └── {scene}/
│       ├── README.md
│       ├── flow.md
│       ├── rules.md
│       └── objects.md
├── prompt/
│   └── {scene}/
│       ├── frontend/
│       │   └── README.md
│       ├── backend/
│       │   └── README.md
│       ├── sql/
│       │   └── README.md
│       ├── test/
│       │   └── README.md
│       ├── ops/
│       │   └── README.md
│       ├── api/
│       │   └── README.md
│       ├── dictionary/
│       │   └── README.md
│       ├── mock/
│       │   └── README.md
│       └── review/
│           └── README.md
└── deliverables/
    └── {scene}/
        ├── README.md
        ├── sql/
        ├── test/
        ├── ops/
        ├── api/
        ├── dictionary/
        ├── mock/
        └── review/
```

---

## 3. bus 模板说明

### 3.1 `docs/bus/{scene}/README.md`

建议作为场景总说明，至少包含：

- 场景名称
- 业务目标
- 适用范围
- 关键流程
- 关键规则
- 输入输出对象
- 关联系统
- 风险与边界

### 3.2 `flow.md`

用于说明主要业务流程、状态流转、关键节点。

### 3.3 `rules.md`

用于沉淀业务规则、校验规则、约束条件、异常分支。

### 3.4 `objects.md`

用于说明业务对象、核心字段、关键关系、主从结构。

---

## 4. prompt 模板说明

每个 `docs/prompt/{scene}/{category}/README.md` 建议固定包含：

1. 对应 BUS 来源
2. 生成目标
3. 输入上下文
4. 生成约束
5. 输出要求
6. 示例
7. 验收标准

### 4.1 frontend

面向前端页面、组件、交互与接口接入代码生成。

### 4.2 backend

面向后端控制层、服务层、领域对象、持久层与流程编排代码生成。

### 4.3 sql

面向数据库表结构、变更脚本、初始化数据与修复脚本生成。

### 4.4 test

面向测试用例、接口验证、回归检查、UAT 验收内容生成。

### 4.5 ops

面向部署、配置、监控、回滚、应急处理文档生成。

### 4.6 api

面向接口清单、请求响应、错误码与契约说明生成。

### 4.7 dictionary

面向字段字典、枚举、状态机、校验口径说明生成。

### 4.8 mock

面向示例请求、示例响应、联调数据与边界样例生成。

### 4.9 review

面向代码评审、测试评审、上线检查与一致性检查生成。

---

## 5. deliverables 模板说明

### 5.1 `docs/deliverables/{scene}/README.md`

建议至少包含：

1. 对应 BUS 场景
2. 对应 Prompt 来源
3. 交付物清单
4. 使用顺序
5. 适用环境
6. 风险与注意事项
7. 回滚与兜底

### 5.2 各子目录用途

- `sql/`：正式 SQL 脚本
- `test/`：正式测试与验证资料
- `ops/`：正式运维与上线资料
- `api/`：正式接口契约资料
- `dictionary/`：正式数据语义资料
- `mock/`：正式样例与联调资料
- `review/`：正式检查与验收资料

---

## 6. 命名要求

- `{scene}` 在 `docs/bus/`、`docs/prompt/`、`docs/deliverables/` 下必须完全一致
- 目录命名使用稳定业务语义，避免临时缩写
- 同一场景的标题名称、文件名称、交付物说明应保持一致口径

---

## 7. 落地建议

新增一个业务场景时，建议按以下顺序进行：

1. 先建立 `docs/bus/{scene}`
2. 补齐核心 BUS 内容
3. 再建立 `docs/prompt/{scene}`
4. 从 `api`、`dictionary`、`backend`、`frontend` 开始补提示词
5. 最后建立 `docs/deliverables/{scene}` 并沉淀正式非代码交付物

这样可以保证从业务定义到工程实现的链路始终可追溯。
