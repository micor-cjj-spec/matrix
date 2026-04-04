# 业务文档索引（bus）

本目录按当前前后端代码中的真实业务分组逐步重建，不再仅依赖历史散列目录。

## 当前已建立的一级分组
- `fi/`：财务云
- `auth/`：登录认证
- `ai/`：AI 功能
- `base-data/`：基础资料

## 当前推荐入口
### 1. 财务云
- `fi/INDEX.md`
- `fi/AUDIT_STATUS.md`
- `fi/HANDOFF.md`

### 2. 登录认证
- `auth/INDEX.md`
- `auth/AUDIT_STATUS.md`

### 3. AI 功能
- `ai/INDEX.md`
- `ai/AUDIT_STATUS.md`

### 4. 基础资料
- `base-data/INDEX.md`
- `base-data/AUDIT_STATUS.md`
- `base-data/HANDOFF.md`

## 当前进度概览
### 财务云（fi）
已建立并持续扩写的分组包括：
- `gl/`
- `ar/`
- `ap/`
- `finance-base-data/`
- `fund/`
- `tax/`
- `fa/`
- `shared/`

### 登录认证（auth）
已建立模块包括：
- `001-login`
- `002-register`
- `003-portal`

### AI 功能（ai）
已建立模块包括：
- `001-ai-assistant`

### 基础资料（base-data）
当前已建立或正在收口的模块包括：
- `001-material`
- `002-customer`
- `003-supplier`
- `004-currency`
- `005-exchange-rate`
- `006-bank-info`
- `007-country`
- `008-region`
- `009-unit`

## 当前收口原则
1. 目录结构以真实菜单、路由和代码归属为准。
2. 已实现模块优先补齐业务文档。
3. 未实现模块只保留状态说明，不虚构细节。
4. 前端已接入但后端未确认的模块，只描述前端可确认行为。
5. 后续优先通过新增文件稳步推进，再在条件成熟时做覆盖和替换收口。
