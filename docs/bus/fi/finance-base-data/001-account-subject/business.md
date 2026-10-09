# 会计科目业务说明

## 1. 业务名称
会计科目

## 2. 业务定位
会计科目是财务基础资料的核心主数据，用于维护科目层级、业务单元归属、损益类型、报表项目映射和现金类标记，为总账、报表、现金流及 AR/AP 凭证生成提供基础口径。

## 3. 业务目标
- 维护会计科目主数据及层级关系
- 关联业务单元、报表项目和一级科目
- 配置现金、银行、现金等价物、外币、数量核算等属性
- 为报表、现金流和凭证分录控制提供基础元数据

## 4. 数据来源
前端 `FinanceBaseDataView.vue` 通过 `/finance/base-data/account-subject` 进入 `AccountSubjectView.vue`，并通过 `AccountSubjectForm.vue` 维护；后端由 `BizfiFiAccountController / BizfiFiAccountServiceImpl` 基于 `bizfi_fi_account` 表提供 CRUD 和查询能力。
