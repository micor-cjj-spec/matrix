# 测试交付

## 手工验证

1. 执行 `sql/bizfi_fi_month_end_check_batch_v1.sql`。
2. 执行 `sql/bizfi_fi_month_end_close_execution_v1.sql`。
3. 进入 `/ledger/month-end-close-workbench`。
4. 生成检查批次。
5. 提交并批准无阻塞批次。
6. 点击“执行关账”。
7. 确认会计期间状态变为 `CLOSED`。
8. 确认页面出现关账执行记录。

## 自动验证

- `git diff --check`：后端、前端均通过；仅保留已有 LF/CRLF 提示。
- `node --check src/api/periodProcess.js`：通过。
- `node --check src/views/login/ledger/period-process/periodProcessShared.js`：通过。
- `@vue/compiler-sfc` 解析并编译 `MonthEndCloseWorkbenchView.vue`：通过。
- 后端 Maven 编译：当前环境未安装 `mvn`，需在具备 Maven 的环境执行。
