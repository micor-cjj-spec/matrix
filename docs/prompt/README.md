# Prompt 目录说明

## 1. 目录目标

`docs/prompt/` 用于沉淀基于 BUS 文档生成工程资产的提示词文档。

其职责不是保存正式代码或最终非代码交付物，而是定义如何根据 `docs/bus/` 中的业务场景文档，生成前端、后端、SQL、测试、运维等工程资产。

---

## 2. 与其他目录的关系

- `docs/bus/`：定义业务目标、业务规则、流程和边界
- `docs/prompt/`：定义生成各类工程资产的提示词
- `matrix`：后端正式代码落点
- `matrix-web`：前端正式代码落点
- `docs/deliverables/`：非代码类正式交付物落点

推荐追溯链路：

`docs/bus/{scene} -> docs/prompt/{scene} -> matrix / matrix-web / docs/deliverables/{scene}`

---

## 3. 目录结构

```text
docs/prompt/
├── common/
├── integration/
└── {scene}/
    ├── frontend/
    ├── backend/
    ├── sql/
    ├── test/
    ├── ops/
    ├── api/
    ├── dictionary/
    ├── mock/
    └── review/
```

---

## 4. 通用目录说明

### 4.1 common

用于沉淀跨场景共用提示词，包括：

- 通用输出格式约束
- 通用命名规范
- 通用代码规范
- 通用异常处理要求
- 通用日志规范
- 通用安全基线
- 通用技术栈要求

### 4.2 integration

用于沉淀跨多类产物的编排型提示词，包括：

- BUS 到多产物的端到端生成
- 前后端契约对齐
- 后端与 SQL 对齐
- 生成结果一致性校验

---

## 5. 场景目录说明

每个业务场景在 `docs/prompt/` 下建立同名目录，且必须与 `docs/bus/{scene}`、`docs/deliverables/{scene}` 保持同名。

场景目录下固定包含以下 9 类提示词：

1. `frontend`
2. `backend`
3. `sql`
4. `test`
5. `ops`
6. `api`
7. `dictionary`
8. `mock`
9. `review`

---

## 6. 提示词文件建议结构

每个提示词文件建议至少包含：

1. 对应 BUS 来源
2. 生成目标
3. 输入上下文
4. 生成约束
5. 输出要求
6. 示例
7. 验收标准

---

## 7. 安全与权限说明

安全与权限当前不单独拆分目录，而作为横切要求纳入：

- `common`
- `frontend`
- `backend`
- `api`
- `ops`
- `review`

后续若权限模型复杂度明显提高，再独立扩展 `security/` 目录。
