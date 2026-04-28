# 财务月结工作台测试交付

## 已执行

- 前端构建：`npm.cmd run build`，通过。
- Git 空白检查：`git diff --check`，通过，仅提示已有 CRLF/LF 转换警告。
- 前端开发服务：已启动并通过 `http://127.0.0.1:5173` 返回 200。

## 未执行

- 后端 Maven 编译未执行成功：当前环境未找到 `mvn`，项目也没有 Maven Wrapper。

## 建议补测

1. 调用 `GET /period-process/month-end-workbench?forg=1&period=2026-04`。
2. 构造未过账凭证，确认返回 `VOUCHER_POSTING` 阻塞项。
3. 构造总账借贷不平，确认返回 `GL_BALANCE` 阻塞项。
4. 访问 `/ledger/month-end-close-workbench`，确认页面可查询和下钻。
