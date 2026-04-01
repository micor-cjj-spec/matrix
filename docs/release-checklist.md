# Matrix 上线检查清单（安全链路版）

> 适用分支：dev / release
> 最后更新：2026-03-08

## A. 鉴权链路

- [ ] 网关全局鉴权开启（`GatewayAuthFilter` 生效）
- [ ] 网关白名单仅包含：
  - `/api/auth/**`
  - `/api/actuator/**`
  - `/api/swagger-ui/**`
  - `/api/v3/api-docs/**`
- [ ] 非白名单接口未带 token 时返回 401
- [ ] 带无效 token 时返回 401
- [ ] 带有效 token 时可访问受保护业务接口

## B. 服务端鉴权

- [ ] `base-service` 已启用 JWT 过滤器
- [ ] `share-service` 已启用 JWT 过滤器
- [ ] `auth-service` 仅放行 `/auth/**` 等登录相关接口
- [ ] 服务内白名单最小化（仅保留必要跨服务调用接口）

## C. 登录与验证码

- [ ] 账号登录：失败 3 次后才要求图形验证码
- [ ] 短信验证码接口可用（`/auth/sms/send`）
- [ ] 邮箱登录接口可用（`/auth/login/email`）
- [ ] 二维码接口存在（`generate/status`，当前为占位实现）

## D. 运行与部署

- [ ] 后端 jar 已同步到：`/root/docker/server/jars`
- [ ] compose 中已移除 `common` 伪服务
- [ ] 仅重启 Spring 容器（不要重启整机）
- [ ] `docker compose ps` 显示关键服务 running

## E. 监控与告警

- [ ] `auth/base/share/gateway` 暴露 `actuator/health`
- [ ] 配置 `ALERT_WEBHOOK_URL`（可选，未配置则不发告警）
- [ ] 500 异常可触发 webhook（日志可见）

## F. 回归建议（上线前）

- [ ] 登录成功 -> 跳转门户
- [ ] 不带 token 调业务接口 -> 401
- [ ] 带 token 调业务接口 -> 200
- [ ] 登录失败 3 次后验证码显隐逻辑正确
- [ ] 前端主要页面可加载（无接口 401 连锁报错）

---

## 快速验证命令

```bash
# 1) 网关白名单接口
curl -i http://127.0.0.1:10000/api/auth/captcha

# 2) 受保护接口（无 token，应 401）
curl -i "http://127.0.0.1:10000/api/user/list?page=1&size=1"

# 3) 服务状态
docker compose -f /root/docker/docker-compose.yml ps
```
