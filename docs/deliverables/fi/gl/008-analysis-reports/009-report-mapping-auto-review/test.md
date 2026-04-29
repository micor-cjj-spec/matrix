# 测试记录

## 本地测试
- 通过：`matrix-web` 执行 `npm run build`，前端生产构建成功。
- 通过：本轮前端文件与文档执行 `git diff --check`，仅提示 Windows CRLF 转换警告，无空白错误。
- 未执行：本轮无后端运行时代码变更，且本地未提供 `mvn` / `mvnw.cmd`，未运行 Maven 测试。

## 线上测试
- 待部署后使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 待验证：企业纳税表带 `review=reportMappingResolution` 回跳参数时展示自动复核提示。
