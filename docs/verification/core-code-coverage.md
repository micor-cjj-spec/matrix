# 核心代码对象覆盖核验清单

本文件用于核验：已确认存在真实代码落点的核心对象，是否已经具备对应的业务文档、提示词和草稿记录。

## 1. fi/ar/arap-doc
- 代码对象：`BizfiFiArapDoc`、`BizfiFiArapDocController`、`BizfiFiArapDocServiceImpl`
- 业务文档：`docs/bus/fi/ar/arap-doc/`
- 提示词：`docs/prompt/fi/ar/arap-doc/`
- 草稿记录：
  - `docs/draft/2026/03/14/001-arap-doc-foundation/`
  - `docs/draft/2026/03/16/001-arap-submit-audit/`
  - `docs/draft/2026/03/18/001-arap-voucher-link/`
- 说明：核心链路已覆盖，后续可将这些路径回填到 `manifest.json` 中。

## 2. fi/ar/counterparty-credit
- 代码对象：`BizfiFiCounterpartyCredit`
- 业务文档：`docs/bus/fi/ar/counterparty-credit/`
- 提示词：`docs/prompt/fi/ar/counterparty-credit/`
- 草稿记录：
  - `docs/draft/2026/03/20/001-credit-control/`
- 说明：信用配置与往来单拦截逻辑已形成闭环。

## 3. fi/gl/voucher
- 代码对象：`BizfiFiVoucher`、`BizfiFiVoucherLine`、`BizfiFiVoucherController`、`BizfiFiVoucherServiceImpl`
- 业务文档：`docs/bus/fi/gl/voucher/`
- 提示词：`docs/prompt/fi/gl/voucher/`
- 草稿记录：
  - `docs/draft/2026/03/23/001-voucher-core-flow/`
  - `docs/draft/2026/03/26/001-voucher-lines-and-source-docs/`
- 说明：主流程、明细、来源单据、批量过账、冲销、OCR、分析已通过周边对象扩展覆盖。

## 4. fi/gl/gl-entry
- 代码对象：`BizfiFiGlEntry`
- 业务文档：`docs/bus/fi/gl/gl-entry/`
- 提示词：`docs/prompt/fi/gl/gl-entry/`
- 草稿记录：
  - `docs/draft/2026/03/23/001-voucher-core-flow/`
  - `docs/draft/2026/03/26/001-voucher-lines-and-source-docs/`
- 说明：总账分录作为过账结果对象，已沉淀为独立业务对象。

## 5. fi/gl/accounting-period
- 代码对象：`BizfiFiAccountingPeriod`、`BizfiFiAccountingPeriodController`、`BizfiFiAccountingPeriodServiceImpl`
- 业务文档：`docs/bus/fi/gl/accounting-period/`
- 提示词：`docs/prompt/fi/gl/accounting-period/`
- 草稿记录：
  - `docs/draft/2026/03/21/001-accounting-period-control/`
- 说明：期间控制、关账、反开能力已成体系。

## 6. fi/gl/voucher-type
- 代码对象：`BizfiFiVoucherType`、`BizfiFiVoucherTypeController`、`BizfiFiVoucherTypeServiceImpl`
- 业务文档：`docs/bus/fi/gl/voucher-type/`
- 提示词：`docs/prompt/fi/gl/voucher-type/`
- 草稿记录：
  - `docs/draft/2026/03/24/001-voucher-type-governance/`
- 说明：凭证字治理能力已成体系。

## 7. fi/gl/org-finance-config
- 代码对象：`BizfiFiOrgFinanceConfig`、`BizfiFiOrgFinanceConfigController`、`BizfiFiOrgFinanceConfigServiceImpl`
- 业务文档：`docs/bus/fi/gl/org-finance-config/`
- 提示词：`docs/prompt/fi/gl/org-finance-config/`
- 草稿记录：
  - `docs/draft/2026/03/27/001-org-finance-config/`
- 说明：组织级财务参数已成体系。

## 8. fi/gl/voucher-ocr-import
- 代码对象：`BizfiFiVoucherOcrServiceImpl` + `BizfiFiVoucherController` OCR 接口
- 业务文档：`docs/bus/fi/gl/voucher-ocr-import/`
- 提示词：`docs/prompt/fi/gl/voucher-ocr-import/`
- 草稿记录：
  - `docs/draft/2026/03/29/001-voucher-ocr-import/`
- 说明：OCR 解析与确认导入已成体系。

## 9. fi/gl/voucher-analysis
- 代码对象：`BizfiFiVoucherAnalysisServiceImpl` + `BizfiFiVoucherController` 分析接口
- 业务文档：`docs/bus/fi/gl/voucher-analysis/`
- 提示词：`docs/prompt/fi/gl/voucher-analysis/`
- 草稿记录：
  - `docs/draft/2026/03/31/001-voucher-analysis/`
- 说明：汇总分析与结转清单已成体系。

## 当前结论
这一批已确认存在真实代码落点的核心对象，已经基本具备：
- 业务文档（bus）
- 提示词（prompt）
- 草稿记录（draft）

当前剩余的小缺口主要是：
- 部分较早创建的 `manifest.json` 还没有把 `promptDocs` 和 `draftPaths` 完整回填。
- 这是索引层面的轻微不一致，不影响当前目录结构和内容本身的完整性。
