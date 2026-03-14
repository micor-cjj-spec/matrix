# 财务单据字段字典（V1）

> 目标：统一 AR/AP/收款/付款/核销/合并调整单据的数据底座，支撑后续风控、自动凭证、合并报表、分析报表。
> 更新时间：2026-03-14

## 1. 适用范围
- 应收单（AR）
- 应付单（AP）
- 收款单
- 付款单
- 结算/核销单
- 合并调整单（新增建议）

---

## 2. 通用字段（所有单据头）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| company_id | bigint | Y | 公司/主体ID |
| book_id | bigint | Y | 账套ID |
| doc_type | varchar(32) | Y | 单据类型（AR/AP/RECEIPT/PAYMENT/SETTLEMENT/CONSOLIDATION_ADJ） |
| doc_no | varchar(64) | Y | 单据编号（期间内唯一） |
| biz_date | date | Y | 业务日期 |
| period | char(7) | Y | 会计期间（YYYY-MM） |
| counterparty_id | bigint | N | 往来方ID |
| counterparty_name | varchar(128) | N | 往来方名称快照 |
| internal_flag | tinyint | Y | 是否内部交易（0/1） |
| internal_company_id | bigint | N | 内部对手主体ID |
| dept_id | bigint | N | 部门ID |
| project_id | bigint | N | 项目ID |
| biz_unit_id | bigint | N | 业务单元ID |
| currency_code | varchar(16) | Y | 原币币种 |
| exchange_rate | decimal(18,6) | Y | 汇率 |
| amount_fc | decimal(18,2) | Y | 原币金额 |
| amount_lc | decimal(18,2) | Y | 本位币金额 |
| tax_amount | decimal(18,2) | N | 税额 |
| amount_ex_tax | decimal(18,2) | N | 不含税金额 |
| risk_level | varchar(16) | N | 风险等级（LOW/MID/HIGH） |
| risk_tags | json | N | 风险标签（JSON） |
| credit_hit_over_limit | tinyint | N | 是否命中超限 |
| credit_hit_overdue | tinyint | N | 是否命中超期 |
| credit_blocked | tinyint | N | 是否被硬拦截 |
| risk_suggestion | varchar(255) | N | 建议动作 |
| status | varchar(16) | Y | 状态（DRAFT/SUBMITTED/AUDITED/POSTED/REJECTED） |
| source_type | varchar(32) | N | 来源类型 |
| source_id | bigint | N | 来源ID |
| source_no | varchar(64) | N | 来源编号 |
| remark | varchar(500) | N | 备注 |
| created_by | varchar(64) | Y | 创建人 |
| created_time | datetime | Y | 创建时间 |
| updated_by | varchar(64) | Y | 更新人 |
| updated_time | datetime | Y | 更新时间 |
| submitted_by | varchar(64) | N | 提交人 |
| submitted_time | datetime | N | 提交时间 |
| audited_by | varchar(64) | N | 审核人 |
| audited_time | datetime | N | 审核时间 |
| posted_by | varchar(64) | N | 过账人 |
| posted_time | datetime | N | 过账时间 |

---

## 3. 通用分录行字段（所有可生成凭证的单据行）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| line_no | int | Y | 行号 |
| summary | varchar(255) | Y | 摘要 |
| account_code | varchar(32) | Y | 会计科目编码 |
| account_name | varchar(128) | N | 会计科目名称快照 |
| debit_amount | decimal(18,2) | Y | 借方金额 |
| credit_amount | decimal(18,2) | Y | 贷方金额 |
| currency_code | varchar(16) | Y | 原币币种 |
| exchange_rate | decimal(18,6) | Y | 汇率 |
| amount_fc | decimal(18,2) | N | 原币金额 |
| amount_lc | decimal(18,2) | Y | 本位币金额 |
| assist_customer_id | bigint | N | 客户辅助核算 |
| assist_supplier_id | bigint | N | 供应商辅助核算 |
| assist_dept_id | bigint | N | 部门辅助核算 |
| assist_project_id | bigint | N | 项目辅助核算 |
| assist_employee_id | bigint | N | 员工辅助核算 |
| tax_code | varchar(32) | N | 税码 |
| tax_rate | decimal(8,4) | N | 税率 |
| cashflow_item | varchar(64) | N | 现金流量项目 |
| source_line_id | bigint | N | 来源行ID |

规则：
- 借贷互斥；同一行借贷不能同时 > 0
- 单据行至少 2 行
- 单据借贷合计必须平衡

---

## 4. 单据类型扩展字段

### 4.1 应收单（AR）
- invoice_no（发票号）
- due_date（到期日）
- aging_days（账龄天数）
- collection_plan_date（计划回款日）
- salesperson_id（业务员）
- settlement_status（结清状态）

### 4.2 应付单（AP）
- bill_no（来票/账单号）
- due_date
- payment_plan_date（计划付款日）
- buyer_id（采购员）
- settlement_status

### 4.3 收款单
- bank_account_in_id（收款账户）
- payer_name（付款方）
- receipt_method（收款方式）
- related_ar_doc_no（对应应收单号）
- writeoff_amount（核销金额）

### 4.4 付款单
- bank_account_out_id（付款账户）
- payee_name（收款方）
- payment_method（付款方式）
- related_ap_doc_no（对应应付单号）
- writeoff_amount（核销金额）

### 4.5 结算/核销单
- settle_type（收款核销/付款核销/预收预付核销）
- target_doc_no（目标单据号）
- settle_amount（核销金额）
- settle_diff_amount（差额）
- settle_diff_reason（差额原因）

### 4.6 合并调整单（建议新增）
- consolidation_batch_no（合并批次）
- elimination_type（抵销类型）
- investee_company_id（被投资主体）
- holding_ratio（持股比例）
- nci_amount（少数股东权益金额）
- consolidation_note（调整说明）

---

## 5. 最小必填集（MVP）

单据头必须：
- company_id, book_id, doc_type, doc_no, biz_date, period
- currency_code, exchange_rate, amount_lc
- status, created_by, created_time, updated_by, updated_time

单据行必须：
- line_no, summary, account_code, debit_amount, credit_amount, amount_lc

---

## 6. 约束建议
- 唯一键：`uk_company_book_type_no(company_id, book_id, doc_type, doc_no)`
- 索引：
  - `idx_period_status(period, status)`
  - `idx_counterparty(counterparty_id, biz_date)`
  - `idx_internal_flag(internal_flag, internal_company_id)`
- 金额精度：本位币 2 位，汇率 6 位，四舍五入 HALF_UP
- 状态流转：DRAFT/REJECTED -> SUBMITTED -> AUDITED -> POSTED

---

## 7. DTO/VO 改造清单（后端）
- 头实体：补通用字段 + 对应扩展字段
- 行实体：补辅助核算/税/现金流字段
- 入参 DTO：新增字段并做分组校验（按 doc_type）
- 出参 VO：返回快照字段（counterparty_name/account_name）
- 校验器：
  - 借贷平衡
  - 扩展字段按类型校验
  - 内部交易字段联动校验

---

## 8. 前端改造清单（matrix-web）
- 单据编辑页：动态字段区（按 doc_type 展示）
- 分录行：支持辅助核算列、税码、现金流项目
- 导入模板：统一保留全字段列，非适用可空
- 列表筛选：期间、状态、内部交易、往来方、项目

---

## 9. 迁移策略
1. 先补字段（可空）+ 默认值
2. 再改接口 DTO/VO
3. 最后收紧必填与业务校验
4. 增量回填历史数据（批处理）
