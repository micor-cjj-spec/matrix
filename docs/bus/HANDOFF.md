# 业务文档阶段性交接说明（bus）

## 一、当前交付范围
本轮已围绕 `docs/bus` 逐步建立新的业务文档总结构，并在多个一级分组下按当前前后端代码持续补齐业务文档。

## 二、当前已建立的一级分组
### 1. `fi/` 财务云
当前状态：`done / partial / planned` 混合

说明：
- 已建立 `INDEX.md`、`AUDIT_STATUS.md`、`HANDOFF.md`
- 已较完整补齐：`gl / ar / ap`
- 已建立并持续扩写：`finance-base-data / fund / tax / fa / shared`

### 2. `auth/` 登录认证
当前状态：`partial_frontend_integrated`

说明：
- 已建立 `INDEX.md`、`AUDIT_STATUS.md`
- 已补齐或开始补齐：
  - `001-login`
  - `002-register`
  - `003-portal`
- 当前前端页面与接口已确认，后端认证实现本轮未完整检出

### 3. `ai/` AI 功能
当前状态：`partial_frontend_integrated`

说明：
- 已建立 `INDEX.md`、`AUDIT_STATUS.md`
- 已建立模块：`001-ai-assistant`
- 当前前端会话、配置状态、聊天调用已接入，后端 AI 实现本轮未完整检出

### 4. `base-data/` 基础资料
当前状态：`partial`

说明：
- 已建立 `INDEX.md`、`AUDIT_STATUS.md`、`HANDOFF.md`
- 当前已建立或正在收口的模块包括：
  - `001-material`
  - `002-customer`
  - `003-supplier`
  - `004-currency`
  - `005-exchange-rate`
  - `006-bank-info`
  - `007-country`
  - `008-region`
  - `009-unit`

## 三、当前统一工作原则
1. 目录结构以当前前后端代码中的真实菜单、路由和模块归属为准。
2. 已实现模块优先补齐完整业务文档。
3. 未实现模块仅保留状态说明，不虚构细节。
4. 前端已接入但后端未确认的模块，只描述前端可确认行为与接口口径。
5. 优先通过新增文件稳步推进，再在条件成熟时对旧文件和已存在文件做覆盖、替换与收口。

## 四、当前已知边界
1. 当前 GitHub 可稳定新建文件，但对已存在文件的覆盖需要 `sha`，因此部分已存在目标文件未直接通过高层接口完成原地更新。
2. 当前多个分组已形成新结构入口，但不代表所有旧口径文件都已完全替换。
3. `fi` 收口深度目前最高；`auth / ai / base-data` 仍处于持续扩写阶段。

## 五、建议的后续动作
1. 继续优先收口 `base-data` 中尚未完全落齐的模块。
2. 在具备可稳定更新现有文件能力后，统一处理：
   - 旧 README 替换
   - 已存在文件的补全覆盖
   - 历史散列目录的废弃或删除
3. 条件成熟时，在 `docs/bus` 层补统一状态总览文件，进一步提升跨分组可读性。

## 六、当前推荐阅读路径
- 总入口：`docs/bus/INDEX.md`
- 财务云：`docs/bus/fi/HANDOFF.md`
- 基础资料：`docs/bus/base-data/HANDOFF.md`
