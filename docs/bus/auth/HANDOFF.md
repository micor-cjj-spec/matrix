# 登录认证业务文档阶段性交接说明（auth）

## 一、当前交付范围
本轮已围绕 `docs/bus/auth` 建立登录认证分组入口，并按当前前端页面与接口口径补齐主要模块文档。

## 二、当前已建立的模块
### 1. `001-login`
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
- 前端已接入 `Login.vue`
- 支持账号登录、手机号登录、扫码登录
- 已接入认证接口，但本轮未检到对应后端实现文件

### 2. `002-register`
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
- 前端已接入 `Register.vue`
- 支持邮箱验证码注册
- 已接入注册接口，但本轮未检到对应后端实现文件

### 3. `003-portal`
状态：`partial_frontend_integrated`

当前已建立：
- `manifest.json`
- `business.md`
- `fields.md`
- `flow.md`
- `page.md`
- `api.md`

说明：
- 前端已接入 `Portal.vue`
- 页面按云产品分组展示模块导航，并支持退出登录
- 当前未检到门户专属后端接口实现

## 三、顶层索引文件
当前 `auth/` 下已补充：
- `INDEX.md`：登录认证总入口
- `AUDIT_STATUS.md`：模块状态总览

## 四、当前统一原则
1. 以前端真实页面、路由和接口调用为准。
2. 已接入前端模块优先补齐页面行为、字段、流程与接口口径。
3. 后端未确认时，不虚构服务端认证流程和持久化细节。

## 五、当前已知边界
1. 当前 `auth` 分组主要依据前端页面与 `auth.js` 接口调用整理。
2. 本轮代码检索中未完整定位到对应后端 controller / service 实现文件。
3. 因此当前模块状态统一按 `partial_frontend_integrated` 处理。

## 六、建议的后续动作
1. 后续若确认后端认证实现类，再回填：
   - 登录态校验
   - 验证码与短信发送实现
   - 注册入库与用户状态流转
2. 条件成熟时，再补 `auth` 分组更细的统一状态说明或 README 过渡说明。

## 七、推荐阅读路径
- 总入口：`docs/bus/auth/INDEX.md`
- 状态总览：`docs/bus/auth/AUDIT_STATUS.md`
