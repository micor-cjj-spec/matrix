# AI 业务文档阶段性交接说明（ai）

## 一、当前交付范围
本轮已围绕 `docs/bus/ai` 建立 AI 功能分组入口，并按当前前端页面与接口口径补齐 AI 助手模块文档。

## 二、当前已建立的模块
### 1. `001-ai-assistant`
状态：`partial_frontend_integrated`

已补齐：
- `manifest.json`
- `business.md`
- `fields.md`
- `flow.md`
- `rules.md`
- `page.md`
- `api.md`

说明：
- 前端已接入 `AiAssistantView.vue`
- 支持读取模型配置状态、创建会话、加载历史消息、发送问题、提交反馈
- 前端已接入 `ai.js` 中的会话、聊天、配置状态与反馈接口
- 本轮未检到对应后端 AI 实现文件

## 三、顶层索引文件
当前 `ai/` 下已补充：
- `INDEX.md`：AI 功能总入口
- `AUDIT_STATUS.md`：模块状态总览

## 四、当前统一原则
1. 以前端真实页面、路由和接口调用为准。
2. 已接入前端模块优先补齐页面行为、会话流程和接口口径。
3. 后端未确认时，不虚构模型服务、流式处理或反馈落库细节。

## 五、当前已知边界
1. 当前 `ai` 分组主要依据 `AiAssistantView.vue` 和 `ai.js` 整理。
2. 本轮代码检索中未完整定位到对应后端 controller / service 实现文件。
3. 因此当前模块状态按 `partial_frontend_integrated` 处理。

## 六、建议的后续动作
1. 后续若确认后端 AI 实现类，再回填：
   - 会话持久化逻辑
   - 模型配置读取逻辑
   - 聊天请求的真实处理链路
   - 反馈入库或评价逻辑
2. 条件成熟时，可继续扩展 AI 相关子模块，而不仅限于助手页。

## 七、推荐阅读路径
- 总入口：`docs/bus/ai/INDEX.md`
- 状态总览：`docs/bus/ai/AUDIT_STATUS.md`
