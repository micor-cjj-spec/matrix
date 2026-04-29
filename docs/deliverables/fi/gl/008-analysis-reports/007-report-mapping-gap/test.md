# 测试记录

## 本地测试
- 已执行：`matrix-web` 下 `npm run build`，通过。
- 已执行：相关后端/前端/文档文件 `git diff --check`，通过。
- 说明：本机未检测到 `mvn` 或 `mvnw.cmd`，后端未执行 Maven 编译。

## 线上测试
- 已使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 已验证 `/api/analysis-report/enterprise-tax?period=2026-04&currency=CNY` 返回 `HTTP 200`，`rows=6`，`mappingGaps=1`。
- 已验证首条映射缺口为 `1001 - 库存现金`，`templateId=1002`，`targetRoute=/ledger/report-account-map?accountCode=1001&reportType=PROFIT_STATEMENT&templateId=1002`。
- 已验证企业纳税表页面展示“报表科目映射待处理”面板，并可点击“维护映射”跳转。
- 已验证报表科目映射页能按路由参数自动定位模板“利润表（标准版）”和会计科目 `1001 - 库存现金`。
- 已验证新增映射弹窗会预填报表模板、会计科目和映射类型 `PL/损益映射`。
- 线上验证截图：`matrix-web/output/playwright/report-mapping-gap-final.png`。
