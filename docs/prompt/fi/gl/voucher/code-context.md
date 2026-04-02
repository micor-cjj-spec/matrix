# 凭证代码上下文说明

本文件用于补充 `docs/prompt/fi/gl/voucher/` 下现有 Prompt 的代码事实，避免后续继续生成时只依赖业务文档而忽略真实实现。

## 必须参考的代码路径
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucher.java`
- `fi-service/src/main/java/single/cjj/fi/gl/entity/BizfiFiVoucherLine.java`
- `fi-service/src/main/java/single/cjj/fi/gl/controller/BizfiFiVoucherController.java`

## 当前代码事实
1. 凭证头实体包含编号、日期、摘要、金额、状态、制单/审核/过账信息。
2. 凭证明细实体包含科目编码、借贷金额、币种、汇率、原币金额、现金流量项目。
3. 控制器已经支持：保存草稿、更新、提交、审核、过账、批量过账、驳回、冲销、OCR 导入解析与确认、详情查询、按凭证反查来源单据、列表、汇总分析、结转清单、明细查询与保存。
4. 来源单据反查依赖 `BizfiFiArapDocService`，说明当前凭证已与往来单建立联动。

## 使用要求
- 后续生成后端代码时，优先增强现有 `voucher` 相关实现，不要重新起一套完全平行的凭证模块。
- 后续生成前端代码时，按钮和页面结构必须覆盖已存在的控制器接口能力。
- 后续生成测试方案时，必须包含 OCR 导入、批量过账、冲销、来源单据反查、明细保存等场景。
