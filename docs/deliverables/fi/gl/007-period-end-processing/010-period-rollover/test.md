# 测试交付

## 手工验证

1. 执行 `sql/bizfi_fi_period_rollover_v1.sql`。
2. 完成一次月结检查批次审批。
3. 执行关账并生成关账执行记录。
4. 在月结工作台点击“启用下一期间”。
5. 确认下一期间会计期间档案存在且为 `OPEN`。
6. 确认组织财务参数 `fcurrentPeriod` 更新为下一期间。
7. 确认页面出现期间滚动记录。

## 自动验证

- `git diff --check`：后端、前端均通过；仅有已有 LF/CRLF 提示。
- `node --check src/api/periodProcess.js`：通过。
- `node --check src/views/login/ledger/period-process/periodProcessShared.js`：通过。
- `@vue/compiler-sfc` 解析并编译 `MonthEndCloseWorkbenchView.vue`：通过。
- `npm.cmd run build`：通过。
- 后端 Maven 编译：当前环境未安装 `mvn`，需在具备 Maven 的环境执行。
