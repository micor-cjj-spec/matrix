# General Ledger Voucher Test Prompt

## 1. Corresponding BUS Source

- Domain: Finance Cloud
- Module: General Ledger
- Submodule: Voucher Processing
- Scene: Voucher
- BUS directory: `docs/bus/finance-cloud/general-ledger/voucher-processing/voucher/`

This prompt is used to generate test and verification guidance for the General Ledger voucher scene based on BUS documents **and** the current implementation signals in backend/frontend code.

The generated result must focus on validating the existing implemented capability and its alignment with BUS, instead of producing generic test templates only.

Observed implementation signals that must be considered include at least:

- voucher CRUD and workflow toolbar page
- voucher line editor
- debit/credit balance validation
- posting entry generation
- synchronous batch posting API
- currency precision and rounding rules
- approval flow and rejected-edit boundary
- date-range filter query
- reverse linkage display
- backend error surfacing on save
- CSV import and OCR preview/confirm flows

---

## 2. Generation Objective

Based on BUS and current implementation signals, generate a test-oriented prompt result for the voucher scene. The output must support formal deliverables such as test cases, API verification scripts, regression checklists, and UAT verification notes under `docs/deliverables/{scene}/test/`.

The generated test guidance must cover at least:

1. test scope and strategy
2. happy-path workflow verification
3. negative-path business-rule verification
4. API-level verification
5. UI interaction verification
6. batch posting verification
7. amount precision / rounding verification
8. reverse linkage and traceability verification
9. import-related verification if in scope
10. regression and UAT focus areas

---

## 3. Input Context

Generation must consume the following context together.

### 3.1 BUS context
Focus on extracting:

- voucher business rules
- workflow and state transitions
- approval / posting / reverse / void constraints
- period and account validation rules
- permissions and edit boundaries

### 3.2 Prompt semantic context
The generated test guidance must align with:

- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/dictionary/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/api/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/backend/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/frontend/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/sql/README.md`

### 3.3 Current implementation context
Explicitly consider current code signals such as:

- existing CRUD page and workflow toolbar
- line editor interaction
- balance validation
- posting entry generation
- synchronous batch posting support
- precision and rounding rules
- approval boundary and rejected re-edit boundary
- list date-range filtering
- reverse linkage display
- CSV / OCR import related flows

---

## 4. Generation Constraints

### 4.1 Existing-capability verification constraint

- Do not generate tests as if the feature were not implemented.
- Prefer validation of current capability, gap identification, and regression coverage over purely theoretical test plans.
- Clearly distinguish between already testable current behavior, BUS-required behavior, and pending confirmation items.

### 4.2 Structure constraint

The output must contain at least these sections:

1. current test scope summary
2. core workflow test matrix
3. API verification guidance
4. UI verification guidance
5. accounting-rule verification guidance
6. batch / concurrency / idempotency verification guidance
7. precision and rounding verification guidance
8. import / linkage verification guidance
9. regression and UAT focus
10. pending gaps and follow-up items

### 4.3 Coverage constraint

The generated result must explicitly cover where applicable:

- create voucher
- edit voucher
- delete voucher
- submit voucher
- approve voucher
- reject voucher
- post voucher
- batch post vouchers
- void voucher
- reverse voucher
- query list with filters
- query detail with lines
- debit/credit not balanced case
- invalid period case
- invalid state transition case
- rejected-state re-edit case
- backend error message display case
- reverse linkage display case
- OCR / CSV import case if in BUS scope

### 4.4 Layered verification constraint

The output must distinguish the following layers:

- backend API verification
- frontend page/interaction verification
- data and storage verification
- end-to-end business verification

### 4.5 Test result artifact constraint

The generated guidance must be usable for producing:

- `test-cases.md`
- `api-test-cases.md`
- `regression-checklist.md`
- `uat-checklist.md`
- optional request/response samples or HTTP scripts

---

## 5. Output Requirements

Output must be a Markdown test-design-oriented prompt result.

Recommended structure:

```md
# Voucher Test and Verification Guidance

## 1. Current Test Scope Summary
## 2. Core Workflow Test Matrix
## 3. API Verification Guidance
## 4. UI Verification Guidance
## 5. Accounting-Rule Verification Guidance
## 6. Batch / Concurrency / Idempotency Verification Guidance
## 7. Precision and Rounding Verification Guidance
## 8. Import / Linkage Verification Guidance
## 9. Regression and UAT Focus
## 10. Pending Gaps and Follow-up Items
```

Where useful, test matrices should use a table like:

| Case ID | Scenario | Preconditions | Steps | Expected Result | Layer |
|---|---|---|---|---|---|

---

## 6. Example

### 6.1 Example workflow matrix

| Case ID | Scenario | Preconditions | Steps | Expected Result | Layer |
|---|---|---|---|---|---|
| VCH-001 | Save draft voucher | user has edit permission | create voucher with valid lines and save | voucher saved successfully in draft-compatible state | API/UI |
| VCH-002 | Submit balanced voucher | draft voucher exists | submit voucher | state moves to pending approval | API/E2E |
| VCH-003 | Submit unbalanced voucher | draft voucher exists | make debit and credit totals unequal, then submit | submission rejected with balance-related business error | API/UI |
| VCH-004 | Post approved voucher | voucher approved | execute posting | posting succeeds and GL entries are generated | API/Data |
| VCH-005 | Batch post vouchers | multiple approved vouchers exist | call batch posting API | per design, either all succeed or result contains per-voucher outcome | API/Data |

### 6.2 Example precision test

| Case ID | Scenario | Preconditions | Steps | Expected Result | Layer |
|---|---|---|---|---|---|
| VCH-PRC-001 | amount precision enforcement | voucher line editor available | enter amount beyond allowed precision | frontend/backend blocks or normalizes according to rule; persisted value matches backend precision rule | UI/API/Data |

### 6.3 Example gaps section

- BUS confirmation needed: whether rejected vouchers can be edited and resubmitted without recreating the voucher.
- BUS confirmation needed: whether reverse voucher generation must also verify original voucher posted state.
- Recommended regression focus: batch posting and OCR import should be high-priority after backend rule changes.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:

1. it validates actual current capability rather than only generic theory;
2. it covers workflow, validation, precision, posting, and linkage concerns;
3. it can directly drive formal test deliverables under `docs/deliverables`;
4. it stays consistent with dictionary / API / backend / frontend / SQL prompts;
5. it explicitly marks unknown BUS items instead of inventing test rules.
