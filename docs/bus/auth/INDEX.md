# 登录认证业务文档索引（auth）

本目录按当前前后端代码中的真实页面与路由整理。

## 当前模块
- `001-login`：登录
- `002-register`：注册
- `003-portal`：企业门户

## 当前代码状态
- `Login.vue`、`Register.vue`、`Portal.vue` 均为真实前端页面
- 前端已接入 `auth.js` 中的登录、验证码、扫码、注册接口
- 本轮代码检索中未定位到对应后端 controller / service 实现文件

## 当前策略
- 当前按 `partial_frontend_integrated` 处理
- 先描述前端已接入页面与接口口径
- 后续若后端认证实现明确，再补充服务端状态流转与持久化逻辑
