# Matrix 鉴权白名单与接口访问说明

> 更新时间：2026-03-08

## 1. 认证机制

- 登录成功后由 `auth-service` 颁发 JWT。
- 业务服务（`base-service`、`share-service`）从请求头读取：
  - `Authorization: Bearer <token>`
- 服务端会校验：
  1. JWT 格式与签名；
  2. Redis 中 `token:<jwt>` 是否存在。

若校验失败，接口会返回 401/403（由 Spring Security 拦截）。

---

## 2. 服务级白名单

## 2.1 auth-service

默认白名单：
- `/auth/**`
- `/actuator/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`

说明：登录、注册、验证码、短信发送、二维码状态查询等都在白名单范围。

## 2.2 base-service

白名单：
- `/user/account/**`（供 auth-service 拉取用户）
- `/user`（注册时写入用户）
- `/oss/**`
- `/actuator/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`

其余接口都要求 JWT。

## 2.3 share-service

白名单：
- `/actuator/**`
- `/swagger-ui/**`
- `/v3/api-docs/**`

其余接口都要求 JWT。

---

## 3. 前端联调要求

- 所有业务接口自动携带 `Authorization`（已在前端 axios 拦截器中实现）。
- 网关统一前缀：`/api`。
- 登录后保存 token，登出时清理本地 token。

---

## 4. 常见问题

1) 登录成功但访问业务接口 401
- 检查请求头是否有 `Authorization: Bearer ...`
- 检查 Redis 是否存在 `token:<jwt>`

2) 某接口本应放行却被拦截
- 把路径加入对应服务 `SecurityConfig` 的 `requestMatchers(...)`
- 修改后重启对应服务

3) token 刚发下来就失效
- 检查 auth-service 与业务服务的 JWT 密钥是否一致
- 检查 Redis 是否可用
