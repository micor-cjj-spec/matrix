# 测试交付

## 手工验证

1. 进入 `/ledger/month-end-close-workbench`。
2. 选择组织与期间。
3. 查看“月结归档包”区域。
4. 确认归档状态与检查批次、关账执行、期间滚动记录一致。
5. 确认里程碑按实际进度显示。

## 自动验证

- `git diff --check`：后端、前端均通过；仅有已有 LF/CRLF 提示。
- `node --check src/api/periodProcess.js`：通过。
- `node --check src/views/login/ledger/period-process/periodProcessShared.js`：通过。
- `@vue/compiler-sfc` 解析并编译 `MonthEndCloseWorkbenchView.vue`：通过。
- `npm.cmd run build`：通过。
- 后端 Maven 编译：当前环境未安装 `mvn`，需在具备 Maven 的环境执行。
