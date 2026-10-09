# 月结检查批次测试交付

## 建议验证

1. 执行 `sql/bizfi_fi_month_end_check_batch_v1.sql`。
2. 进入 `/ledger/month-end-close-workbench`。
3. 点击“生成检查批次”，确认列表新增批次。
4. 无阻塞批次点击“提交”，状态变为 `SUBMITTED`。
5. 已提交批次点击“批准”，状态变为 `APPROVED`。
6. 有阻塞项批次提交时应返回业务错误。

## 自动验证

- `git diff --check`：后端、前端均通过；仅保留已有文件的 LF/CRLF 提示。
- `node --check src/views/login/ledger/period-process/periodProcessShared.js`：通过。
- `node --check src/api/periodProcess.js`：通过。
- `@vue/compiler-sfc` 解析并编译 `MonthEndCloseWorkbenchView.vue`：通过。
- `npm.cmd run build`：当前本机环境在 Vite/esbuild 阶段报 `cannot allocate memory` / `write ENOMEM`，未得到完整构建结果。
- 后端 Maven 编译：当前环境未安装 `mvn`，需在具备 Maven 的环境执行。
