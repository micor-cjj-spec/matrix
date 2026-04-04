# 流程设计

## 注册流程
1. 用户进入 `/register`。
2. 输入邮箱并调用 `/auth/email/send` 获取邮箱验证码。
3. 输入验证码、密码和确认密码。
4. 前端调用 `/auth/register` 完成注册。
5. 注册成功后跳转 `/login`。
