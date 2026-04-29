# 测试记录

## 本地测试
- 已执行：`matrix-web` 下 `npm run build`，通过。
- 已执行：相关文档和前端文件 `git diff --check`，通过。
- 说明：本轮无后端运行时代码变更，未执行 Maven 编译。

## 线上测试
- 已使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 已验证企业纳税表点击 `1001 - 库存现金` 的“维护映射”后，跳转到 `/ledger/report-account-map`。
- 已验证跳转 URL 携带 `accountCode=1001`、`reportType=PROFIT_STATEMENT`、`templateId=1002`、`mode=resolve`、`sourcePath=/ledger/enterprise-tax`、`sourcePeriod=2026-04`、`sourceCurrency=CNY`。
- 已验证报表科目映射页展示“缺口治理上下文”面板。
- 已验证新增报表科目映射弹窗自动打开。
- 已验证弹窗预填“利润表（标准版）”、`1001 - 库存现金`、`PL/损益映射`。
- 已验证点击“返回来源报表复核”可返回企业纳税表，并保留 `period=2026-04&currency=CNY`。
- 线上验证截图：`matrix-web/output/playwright/report-mapping-resolution-final.png`。
