# Prompt 与交付物目录规范

## 1. 文档目标

本文档用于约束 `bus`、`prompt`、正式代码产物与非代码交付物之间的目录关系、职责边界与落点规则，保证业务文档、AI 生成提示词与最终交付物可以一一对应、可追溯、可维护。

适用范围：

- 基于 BUS 文档驱动前后端代码生成的业务场景
- 需要同时沉淀 SQL、测试验证、运维等非代码交付物的业务场景
- 需要通过统一提示词规范驱动 AI 生成工程资产的场景

---

## 2. 总体设计原则

### 2.1 一一对应原则

- `prompt` 目录必须与 `bus` 目录按业务场景一一对应
- 一个业务场景在 `bus` 中存在，则在 `prompt` 中必须存在对应场景目录
- 非代码类正式交付物在 `deliverables` 中按同名业务场景归档

### 2.2 职责分离原则

- `docs/bus/`：定义业务目标、业务规则、业务流程与边界
- `docs/prompt/`：定义如何根据 BUS 生成工程资产的提示词
- `matrix`：承载后端正式代码产物
- `matrix-web`：承载前端正式代码产物
- `docs/deliverables/`：承载非代码类正式交付物

### 2.3 代码与非代码分离原则

- 前端和后端正式代码不落在 `docs/deliverables` 目录中
- SQL、测试验证、运维、接口契约、数据字典、Mock、Review 等非代码资产统一落在 `docs/deliverables/{scene}/` 下

### 2.4 可追溯原则

每个业务场景都应建立如下追溯关系：

`BUS -> Prompt -> 正式代码 / 非代码交付物`

### 2.5 文档统一归档原则

除正式前后端代码外，业务文档、提示词文档、交付说明文档及非代码类交付物文档均统一归档于 `docs/` 目录下。

### 2.6 横切约束统一原则

通用生成规则、通用安全基线、通用输出格式、通用技术栈约束等，不应在每个业务场景内重复定义，应放入通用目录统一管理。

---

## 3. 顶层目录职责

推荐目录结构如下：

```text
docs/
├── bus/
├── prompt/
├── deliverables/
└── specs/
```

扩展说明：

- `docs/bus/`：业务定义层
- `docs/prompt/`：AI 生成规则层
- `docs/deliverables/`：非代码正式交付物层
- `docs/specs/`：规范类与总则类文档
- `matrix`：后端代码正式落点
- `matrix-web`：前端代码正式落点

---

## 4. bus 目录规范

```text
docs/bus/
└── {scene}/
    └── ...
```

说明：

- `{scene}` 表示业务场景目录名
- `docs/bus/{scene}` 为该业务场景的业务定义根目录
- 场景命名应稳定、清晰、语义明确，后续 `docs/prompt/{scene}` 与 `docs/deliverables/{scene}` 必须复用同一名称

建议 `docs/bus/{scene}` 中至少包含以下内容：

- 业务目标
- 业务流程
- 业务规则
- 输入输出对象
- 边界条件
- 异常场景

---

## 5. prompt 目录规范

### 5.1 通用目录

```text
docs/prompt/
├── common/
└── integration/
```

说明：

#### `docs/prompt/common/`
用于沉淀所有业务场景共用的提示词规范，包括但不限于：

- 通用输出格式约束
- 通用命名规范
- 通用代码规范
- 通用日志规范
- 通用异常处理约束
- 通用安全基线
- 通用技术栈约束

#### `docs/prompt/integration/`
用于沉淀跨多类产物的编排型提示词，包括但不限于：

- 端到端生成编排
- 前后端契约对齐
- 后端与 SQL 对齐
- BUS 到多产物的整体生成流程

### 5.2 场景目录

每个业务场景必须在 `docs/prompt/` 中建立同名目录：

```text
docs/prompt/
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

### 5.3 场景类提示词说明

#### `frontend/`
用于根据 BUS 生成前端代码相关提示词。

#### `backend/`
用于根据 BUS 生成后端代码相关提示词。

#### `sql/`
用于根据 BUS 生成 SQL 脚本相关提示词。

#### `test/`
用于根据 BUS 生成测试验证相关提示词。

#### `ops/`
用于根据 BUS 生成运维与上线相关提示词。

#### `api/`
用于根据 BUS 生成接口契约相关提示词。

#### `dictionary/`
用于根据 BUS 生成数据字典、枚举、状态机等统一语义相关提示词。

#### `mock/`
用于根据 BUS 生成联调与验证所需示例数据相关提示词。

#### `review/`
用于根据 BUS 生成评审、验收、一致性检查相关提示词。

### 5.4 安全与权限处理原则

安全与权限当前不单独拆分为独立目录，而作为横切约束纳入以下目录：

- `docs/prompt/common/`：通用安全基线
- `docs/prompt/{scene}/frontend/`：页面访问控制、按钮权限显隐
- `docs/prompt/{scene}/backend/`：鉴权、权限校验、数据权限、审计日志
- `docs/prompt/{scene}/api/`：权限点、未登录与无权限响应约束
- `docs/prompt/{scene}/ops/`：密钥、配置、访问控制、日志安全要求
- `docs/prompt/{scene}/review/`：越权检查、敏感数据检查、安全核对项

后续若权限体系、审计要求或安全交付物显著增多，可再升级为独立 `security/` 目录。

---

## 6. 正式代码产物落点规范

### 6.1 前端正式代码

- 前端代码由 `docs/prompt/{scene}/frontend/` 提示词驱动生成
- 正式前端代码产物落在 `matrix-web` 项目中
- 不在 `docs/deliverables` 中重复保存前端源码

### 6.2 后端正式代码

- 后端代码由 `docs/prompt/{scene}/backend/` 提示词驱动生成
- 正式后端代码产物落在 `matrix` 项目中
- 不在 `docs/deliverables` 中重复保存后端源码

---

## 7. deliverables 目录规范

每个业务场景在 `docs/deliverables/` 下建立同名目录，并使用 `README.md` 作为该场景非代码正式交付物的总说明与索引。

推荐结构如下：

```text
docs/deliverables/
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

### 7.1 必要说明

- `docs/deliverables/{scene}/README.md` 为方案 A 的总说明文档
- 前后端源码不进入 `docs/deliverables/{scene}/`
- 非代码类正式交付物统一在该目录下归档

### 7.2 各类交付物说明

#### `sql/`
存放正式 SQL 交付物，例如：

- 建表脚本
- 变更脚本
- 初始化数据脚本
- 数据修复脚本

#### `test/`
存放正式测试验证交付物，例如：

- 测试用例
- 接口验证脚本
- 回归检查清单
- 测试报告
- UAT 验收清单

#### `ops/`
存放正式运维交付物，例如：

- 部署文档
- 环境配置说明
- 回滚预案
- 监控检查清单
- 应急处置手册

#### `api/`
存放正式接口契约交付物，例如：

- 接口清单
- 请求响应说明
- 错误码说明
- OpenAPI 草稿

#### `dictionary/`
存放正式数据语义交付物，例如：

- 字段字典
- 枚举定义
- 状态机说明
- 校验规则说明

#### `mock/`
存放正式样例交付物，例如：

- 请求样例
- 响应样例
- 演示数据
- 边界场景样例

#### `review/`
存放正式检查类交付物，例如：

- 代码评审清单
- 测试评审清单
- 上线检查清单
- 一致性检查清单

---

## 8. deliverables README 规范

`docs/deliverables/{scene}/README.md` 建议至少包含以下内容：

1. 对应 BUS 场景
2. 对应 Prompt 来源
3. 交付物清单
4. 使用顺序
5. 适用环境
6. 风险与注意事项
7. 回滚或兜底说明

推荐结构如下：

```md
# 交付物说明

## 1. 对应 BUS 场景
## 2. 对应 Prompt 来源
## 3. 交付物清单
## 4. 使用顺序
## 5. 适用环境
## 6. 风险与注意事项
## 7. 回滚与兜底
```

---

## 9. 提示词文件编写规范

每个提示词文件建议固定包含以下内容：

1. 对应 BUS 来源
2. 生成目标
3. 输入上下文
4. 生成约束
5. 输出要求
6. 示例
7. 验收标准

推荐结构如下：

```md
# 1. 对应 BUS 来源
# 2. 生成目标
# 3. 输入上下文
# 4. 生成约束
# 5. 输出要求
# 6. 示例
# 7. 验收标准
```

---

## 10. 一致性要求

### 10.1 命名一致性

- `docs/bus/{scene}`、`docs/prompt/{scene}`、`docs/deliverables/{scene}` 的 `{scene}` 必须完全一致
- 场景名称不得随意缩写、改写或同义替换

### 10.2 内容一致性

- BUS 中定义的业务规则，应在 Prompt 中被显式消费
- Prompt 中驱动生成的关键产物，应在正式代码或交付物中落地
- Review 类内容应覆盖 BUS、Prompt 与交付物之间的一致性检查

### 10.3 目录一致性

- 所有规范类文档统一存放在 `docs/specs/`
- 所有业务场景文档统一在 `docs/` 内闭环管理
- 前后端代码仅作为正式实现产物，落在对应代码仓库或代码目录中

---

## 11. 当前推荐目录总览

```text
docs/
├── bus/
│   └── {scene}/
├── prompt/
│   ├── common/
│   ├── integration/
│   └── {scene}/
│       ├── frontend/
│       ├── backend/
│       ├── sql/
│       ├── test/
│       ├── ops/
│       ├── api/
│       ├── dictionary/
│       ├── mock/
│       └── review/
├── deliverables/
│   └── {scene}/
│       ├── README.md
│       ├── sql/
│       ├── test/
│       ├── ops/
│       ├── api/
│       ├── dictionary/
│       ├── mock/
│       └── review/
└── specs/
    └── prompt-deliverables-spec.md
```

---

## 12. 结论

本规范明确以下原则：

- 所有业务文档、提示词文档、交付说明文档统一放在 `docs/` 下
- `docs/prompt/` 与 `docs/bus/` 按业务场景一一对应
- 前端正式代码落在 `matrix-web`
- 后端正式代码落在 `matrix`
- 除代码外的正式交付物统一落在 `docs/deliverables/{scene}/`
- 安全与权限当前不单独拆分目录，而作为横切要求纳入现有分类

后续若场景复杂度提升，可在不破坏当前结构的前提下平滑扩展。