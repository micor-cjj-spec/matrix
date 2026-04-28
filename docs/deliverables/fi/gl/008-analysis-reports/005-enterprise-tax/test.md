# 测试记录

## 本地测试
- 已执行：`matrix-web` 下 `npm run build`，通过。
- 已执行：相关后端/文档文件 `git diff --check`，通过。
- 说明：本机未检测到 `mvn` 或 `mvnw.cmd`，后端未执行 Maven 编译。

## 线上测试
- 待部署后使用测试账号 `19106026235` 登录 micor.top 验证。
- 重点检查 `/ledger/enterprise-tax` 是否返回不少于 5 条税种拆分行。
