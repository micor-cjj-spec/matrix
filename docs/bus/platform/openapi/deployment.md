# OpenAPI 部署配置

## 1. 初始化数据库

执行：

```text
sql/matrix_open_api_v1.sql
```

## 2. OpenAPI Service Nacos 配置

`openapi-service.yaml` 至少需要提供数据源和 Redis：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/matrix?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: matrix
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ${REDIS_PASSWORD:}
```

## 3. Gateway Nacos 路由

在 `gateway.yaml` 中增加路由，确保 OpenAPI 路径只进入 `openapi-service`：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: openapi-service
          uri: lb://openapi-service
          predicates:
            - Path=/api/open-api/**,/api/openapi/admin/**
```

不要把 `/api/internal/openapi/**` 配置成公网 Gateway 路由。

## 4. 主密钥

生成 32 字节 AES 密钥并进行 Base64 编码，将结果通过环境变量注入：

```text
MATRIX_OPENAPI_MASTER_KEY=<base64-key>
```

主密钥不能写入 Git、Nacos 明文公共配置或日志。主密钥丢失后，已有 AppSecret 密文无法恢复，需要执行密钥轮换。

## 5. 启动顺序

1. MySQL、Redis、Nacos。
2. `fi-service`。
3. `openapi-service`。
4. `gateway`。

## 6. 接入顺序

1. 内部管理员调用 `/api/openapi/admin/apps` 创建应用并保存一次性返回的 AppSecret。
2. 查询 `/api/openapi/admin/definitions` 获取三个 API 定义 ID。
3. 调用 `/api/openapi/admin/grants` 授权。
4. 外部系统按签名规则调用 `/api/open-api/v1/fi/vouchers`。
