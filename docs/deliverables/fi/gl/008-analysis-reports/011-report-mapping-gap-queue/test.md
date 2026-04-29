# 测试记录

## 本地测试
- 通过：`matrix-web` 执行 `npm run build`，生产构建成功。
- 通过：本轮前端文件执行 `git diff --check`，仅提示 Windows CRLF 转换警告，无空白错误。
- 未执行：本轮无后端运行时代码变更，且本地未提供 `mvn` / `mvnw.cmd`，未运行 Maven 测试。

## 线上测试
- 通过：2026-04-29 使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 通过：企业纳税表 `2026-04` / `CNY` 页面展示 `待治理队列：共 1 项`，单条缺口展示 `第 1 / 1 项`，并保留 `开始治理` / `维护映射` 入口。
- 通过：从真实缺口点击 `维护映射` 后，报表科目映射页 URL 携带 `gapQueue`、`gapIndex=0`、`accountName`、来源报表参数和 `recommendationReason`。
- 通过：真实缺口映射页和新增映射弹窗均展示 `队列进度：第 1 / 1 项` 与来源建议；当前真实缺口无可靠推荐项目，未自动预填报表项目。
- 通过：使用只读路由模拟 2 项缺口队列，映射页展示 `队列进度：第 1 / 2 项`，提供 `下一条缺口`；点击后跳转为 `gapIndex=1` 并展示 `队列进度：第 2 / 2 项`。
- 通过：模拟第 2 项携带 `recommendedItemCode=PL_REVENUE` 时，治理上下文展示 `已推荐报表项目：营业收入 (PL_REVENUE)`，新增映射弹窗自动预填 `营业收入 (PL_REVENUE)`。
- 通过：本次线上验证未点击 `创建`，未写入生产映射数据。
- 截图：`matrix-web/output/playwright/report-mapping-gap-queue-final.png`。
