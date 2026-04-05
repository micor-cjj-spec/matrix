# GL Voucher SQL Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate or refine SQL design guidance for the GL voucher scene based on BUS and current implementation signals.

Observed signals include at least:
- voucher header and lines capability
- balance validation
- posting entry generation
- synchronous batch posting
- precision and rounding rules
- approval and rejected-edit boundary
- reverse linkage display
- date-range filters
- CSV/OCR import related flows

---

## 2. Generation Objective

Generate SQL-oriented guidance that supports formal deliverables such as schema scripts, migration scripts, init data scripts, and fix scripts.

The generated result must cover at least:
1. core table model
2. voucher header table design
3. voucher line table design
4. posting / relation table guidance
5. reverse/source linkage storage guidance
6. status and audit field guidance
7. amount / currency / precision guidance
8. index and query optimization guidance
9. migration and rollback guidance
10. init-data and dictionary dependency guidance

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- core business objects
- workflow and states
- posting / reverse / void rules
- organization / ledger / period / subject / currency relations

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/backend.md`
- `docs/prompt/fi/gl/voucher/frontend.md`

### 3.3 Current implementation context
Consider current capability signals including:
- voucher lines
- posting-generated GL entries
- batch posting
- precision rules
- reverse linkage
- date-range filtering
- import-related traceability needs

---

## 4. Generation Constraints

### 4.1 Existing-schema-alignment constraint
- Do not design an isolated theoretical schema disconnected from current capability.
- Prefer refine / standardize / migrate over total redesign.
- Clearly distinguish assumed current storage, BUS-required fields, and recommended enhancements.

### 4.2 Required sections
The output must contain at least:
1. current storage capability summary
2. core table model
3. voucher header table guidance
4. voucher line table guidance
5. posting / relation table guidance
6. amount / currency / precision guidance
7. index and query optimization guidance
8. migration and rollback guidance
9. init-data and dictionary dependency guidance
10. pending gaps and follow-up items

### 4.3 Schema topics
The generated result must explicitly consider where applicable:
- primary key and voucher business number
- header-line relation
- accounting date / period fields
- organization / ledger / source type fields
- approval / posting / void status fields
- reverse voucher relation fields
- total debit / total credit storage strategy
- line sequence fields
- operator and audit fields
- import source trace fields

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher SQL Guidance

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

---

## 6. Example

| Column | Type Suggestion | Required | Meaning | Notes |
|---|---|---|---|---|
| voucher_no | varchar(64) | yes | voucher business number | unique in defined scope |
| accounting_date | date | yes | accounting date | supports date-range query |
| accounting_period | varchar(16) | yes | accounting period | aligns with period check |
| status | varchar(32) | yes | workflow status | draft/pending/approved/posted... |
| total_debit | decimal(18,2) | yes | total debit amount | used in balance checks |
| total_credit | decimal(18,2) | yes | total credit amount | used in balance checks |
| reverse_voucher_id | bigint | no | reverse/original voucher link | used for linkage display |

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it aligns with current voucher implementation signals;
2. it supports formal SQL deliverables;
3. it covers header, lines, posting relations, precision, and indexing;
4. it stays aligned with dictionary / API / backend / frontend prompts;
5. it marks unclear BUS items explicitly.
