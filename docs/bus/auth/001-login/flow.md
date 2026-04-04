# 流程设计

## 账号登录流程
1. 用户选择“账号登录”。
2. 输入用户名和密码。
3. 若触发验证码要求，则先获取并填写图形验证码。
4. 前端调用 `/auth/login/account`。
5. 返回 token 后保存本地 token 并跳转 `/portal`。

## 手机号登录流程
1. 用户选择“手机号登录”。
2. 输入手机号并调用 `/auth/sms/send` 获取短信验证码。
3. 输入验证码后调用 `/auth/login/phone`。
4. 返回 token 后保存本地 token 并跳转 `/portal`。

## 扫码登录流程
1. 用户选择“扫码登录”。
2. 前端调用 `/auth/login/qrcode/generate` 获取二维码。
3. 页面轮询 `/auth/login/qrcode/status`。
4. 状态为成功时保存 token 并跳转 `/portal`。
