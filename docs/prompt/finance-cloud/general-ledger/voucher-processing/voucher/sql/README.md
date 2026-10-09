# General Ledger Voucher SQL Prompt

## 1. Corresponding BUS Source

- Domain: Finance Cloud
- Module: General Ledger
- Submodule: Voucher Processing
- Scene: Voucher
- BUS directory: `docs/bus/finance-cloud/general-ledger/voucher-processing/voucher/`

This prompt is used to generate or refine SQL design guidance for the General Ledger voucher scene based on BUS documents **and** the current implementation signals in backend/frontend code.

The generated result must support the already-implemented voucher capability rather than designing an unrelated schema from scratch.

Observed implementation signals that must be considered include at least:

- voucher header and voucher lines capability
- debit/credit balance validation
- GL posting entries generation
- synchronous batch posting API
- currency precision and rounding rules
- approval flow and rejected-edit boundary
- reverse linkage display in UI
- list filtering including date range
- OCR / CSV import related flows

---

## 2. Generation Objective

Based on BUS and current code signals, generate SQL-oriented design guidance for the voucher scene. The output must support formal deliverables such as schema scripts, migration scripts, init scripts, and data-fix scripts under `docs/deliverables/{scene}/sql/`.

The generated guidance must cover at least:

1. core table model
2. voucher header table design
3. voucher line table design
4. posting / entry relation design
5. reverse-linkage or source-linkage storage design
6. status and audit field design
7. amount / currency / precision field design
8. index and query optimization guidance
9. migration and compatibility guidance
10. init data / dictionary dependency guidance
11. data consistency and rollback considerations

---

## 3. Input Context

Generation must consume the following context together.

### 3.1 BUS context
Focus on extracting:

- voucher core objects
- voucher lifecycle
- approval / posting / reverse / void rules
- accounting period, organization, ledger, subject and currency relations
- source document and reverse voucher relations

### 3.2 Prompt semantic context
The generated SQL guidance must align with:

- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/dictionary/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/api/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/backend/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/frontend/README.md`

### 3.3 Current implementation context
Explicitly consider current capability signals such as:

- voucher lines already exist in business flow
- posting generates GL entries
- batch posting exists
- precision / rounding rules exist
- reverse linkage has UI visibility
- list queries support date-range filtering
- import flows may require source trace fields

---

## 4. Generation Constraints

### 4.1 Existing-schema-alignment constraint

- Do not design an isolated theoretical schema unrelated to current capability.
- Prefer schema refinement / standardization / migration guidance over complete re-architecture.
- Clearly distinguish between existing-storage assumptions, BUS-required fields, and recommended enhancements.

### 4.2 Structure constraint

The output must contain at least these sections:

1. current storage capability summary
2. core table model
3. voucher header table guidance
4. voucher line table guidance
5. posting / relation table guidance
6. amount / currency / precision guidance
7. indexes and query optimization guidance
8. migration and rollback guidance
9. init-data and dictionary dependency guidance
10. pending gaps and follow-up items

### 4.3 Schema constraint

The generated guidance must explicitly consider where applicable:

- voucher header primary key and business number
- voucher lines linked by voucher id
- accounting period fields
- organization / ledger / source type fields
- approval status / posting status / void status
- reverse voucher relation fields
- total debit / total credit storage strategy
- line sequence fields
- operator and audit metadata
- version or optimistic lock field if needed
- import source trace field if CSV/OCR flow is retained

### 4.4 Precision constraint

The guidance must explicitly describe:

- amount field type
- local / base / transaction currency handling if present
- rounding scale and storage scale
- precision consistency with backend validation

### 4.5 Indexing constraint

The guidance must include index recommendations for likely queries such as:

- voucher number query
- accounting date / date range query
- accounting period query
- status query
- organization / ledger query
- source document linkage query
- reverse voucher linkage query

---

## 5. Output Requirements

Output must be a Markdown SQL-design-oriented prompt result.

Recommended structure:

```md
# Voucher SQL Design Guidance

## 1. Current Storage Capability Summary
## 2. Core Table Model
## 3. Voucher Header Table Guidance
## 4. Voucher Line Table Guidance
## 5. Posting / Relation Table Guidance
## 6. Amount / Currency / Precision Guidance
## 7. Index and Query Optimization Guidance
## 8. Migration and Rollback Guidance
## 9. Init-Data and Dictionary Dependency Guidance
## 10. Pending Gaps and Follow-up Items
```

Where useful, table guidance should include a table like:

| Column | Type Suggestion | Required | Meaning | Source | Notes |
|---|---|---|---|---|---|

---

## 6. Example

### 6.1 Example header fields

| Column | Type Suggestion | Required | Meaning | Source | Notes |
|---|---|---|---|---|---|
| id | bigint | yes | voucher primary key | system | technical id |
| voucher_no | varchar(64) | yes | voucher business number | system/business rule | unique within defined scope |
| accounting_date | date | yes | voucher accounting date | user input | supports date range query |
| accounting_period | varchar(16) | yes | accounting period | derived/input | must align with period check |
| status | varchar(32) | yes | voucher workflow status | business | draft/pending/approved/posted... |
| total_debit | decimal(18,2) | yes | total debit amount | computed | for balance check and query |
| total_credit | decimal(18,2) | yes | total credit amount | computed | for balance check and query |
| reverse_voucher_id | bigint | no | linked reverse/original voucher | business action | used for linkage display |

### 6.2 Example line fields

| Column | Type Suggestion | Required | Meaning | Source | Notes |
|---|---|---|---|---|---|
| id | bigint | yes | line primary key | system | technical id |
| voucher_id | bigint | yes | linked voucher id | system | foreign key or logical relation |
| line_no | int | yes | line sequence number | frontend/backend | preserves display order |
| account_id | bigint | yes | account subject id | user input | must pass account validation |
| dr_cr_direction | varchar(16) | yes | debit/credit direction | user input | used in balance logic |
| amount | decimal(18,2) | yes | line amount | user input | precision must align with backend |

### 6.3 Example gaps section

- BUS confirmation needed: whether posted vouchers support unpost or reverse-only strategy.
- Recommended enhancement: add explicit source_trace_type and source_trace_id if OCR/CSV/import traceability becomes required.
- Recommended enhancement: add composite indexes for organization + period + status query pattern if current list volume grows.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:

1. it aligns with current voucher implementation signals;
2. it supports formal SQL deliverables under `docs/deliverables`;
3. it covers header, lines, posting relations, precision, and index strategy;
4. it stays consistent with dictionary / API / backend prompts;
5. it explicitly marks unknown BUS items instead of inventing schema rules.
