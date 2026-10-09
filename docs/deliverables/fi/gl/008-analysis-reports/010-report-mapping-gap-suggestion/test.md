# 测试记录

## 本地测试
- 通过：`matrix-web` 执行 `npm run build`，生产构建成功。
- 通过：本轮文档和前端文件执行 `git diff --check`，仅提示 Windows CRLF 转换警告，无空白错误。
- 未执行：本轮无后端运行时代码变更，且本地未提供 `mvn` / `mvnw.cmd`，未运行 Maven 测试。

## 线上测试
- 通过：2026-04-29 使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 通过：企业纳税表 `2026-04` / `CNY` 页面展示不少于 5 条税种拆分行。
- 通过：企业纳税表缺口卡片展示 `1001 - 库存现金` 的治理建议，并提供 `开始治理` / `维护映射` 入口。
- 通过：点击 `维护映射` 后跳转到报表科目映射页，URL 保留 `mode=resolve`、`accountCode=1001`、`reportType=PROFIT_STATEMENT`、`templateId=1002`、来源参数和 `recommendationReason`。
- 通过：映射页治理上下文和新增映射弹窗均展示来源建议；当前真实缺口为资产类科目，未自动带入利润表项目，符合“不可靠推荐不预填”的规则。
- 通过：只读路由验证 `recommendedItemCode=PL_REVENUE` 时，映射页显示 `已推荐报表项目：营业收入 (PL_REVENUE)`，新增映射弹窗自动预填 `营业收入 (PL_REVENUE)`。
- 通过：本次线上验证未点击 `创建`，未写入生产映射数据。
- 截图：`matrix-web/output/playwright/report-mapping-gap-suggestion-final.png`。
