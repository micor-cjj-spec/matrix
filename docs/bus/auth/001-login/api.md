# 接口说明

## 认证接口
- `POST /auth/login/account`
- `POST /auth/login/phone`
- `GET /auth/captcha`
- `POST /auth/sms/send`
- `GET /auth/login/qrcode/generate`
- `GET /auth/login/qrcode/status`

## 前端关联
- 前端页面：`Login.vue`
- 组合逻辑：`useLogin.js`
- 前端 API：`auth.js`

## 代码边界
- 前端接口调用已接入。
- 本轮未检到对应后端实现文件。
