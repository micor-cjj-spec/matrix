# 测试记录

## 本地测试
- 通过：`matrix-web` 执行 `npm run build`，前端生产构建成功。
- 通过：本轮前端文件与文档执行 `git diff --check`，仅提示 Windows CRLF 转换警告，无空白错误。
- 未执行：本轮无后端运行时代码变更，且本地未提供 `mvn` / `mvnw.cmd`，未运行 Maven 测试。

## 线上测试
- 通过：使用测试账号 `19106026235` 登录 `https://micor.top/`。
- 通过：访问企业纳税表回跳复核地址：
  `https://micor.top/ledger/enterprise-tax?period=2026-04&currency=CNY&review=reportMappingResolution&resolvedAccountCode=1001&resolvedAccountName=库存现金&resolvedTemplateId=1002`
- 通过：页面顶部展示自动复核警告 `已返回企业纳税表自动复核：1001 - 库存现金 仍在映射缺口中，请确认报表项目是否选择正确后重新保存。`
- 通过：页面仍展示 `1001 - 库存现金` 的报表科目映射待处理卡片，说明缺口未被误判为已消失。
- 通过：企业纳税表税种拆分行不少于 5 条。
- 说明：为避免污染生产数据，本次线上验证未真实新增报表科目映射，仅验证回跳参数识别与复核提示展示。
- 截图留证：`matrix-web/output/playwright/report-mapping-auto-review-final.png`。
