# GL Voucher Test Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate test and verification guidance for the GL voucher scene based on BUS and current implementation signals.

Observed implementation signals include at least:
- voucher CRUD and workflow page
- voucher line editor
- balance validation
- posting entry generation
- synchronous batch posting
- precision and rounding rules
- approval flow and rejected-edit boundary
- date-range filters
- reverse linkage display
- backend error surfacing
- CSV/OCR-related flows

---

## 2. Generation Objective

Generate test-oriented guidance that supports formal artifacts such as test cases, API verification scripts, regression checklists, and UAT notes.

The generated result must cover at least:
1. test scope and strategy
2. workflow verification
3. negative-path business-rule verification
4. API verification
5. UI verification
6. batch posting verification
7. precision verification
8. reverse-linkage verification
9. import-related verification
10. regression and UAT focus

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- business rules
- workflow and state transitions
- approval / posting / reverse / void constraints
- period and account validation rules
- permissions and edit boundaries

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/backend.md`
- `docs/prompt/fi/gl/voucher/frontend.md`
- `docs/prompt/fi/gl/voucher/sql.md`

### 3.3 Current implementation context
Consider current capability signals including:
- existing CRUD page and workflow toolbar
- line editor interaction
- balance validation
- posting entry generation
- synchronous batch posting
- precision and rounding rules
- approval boundary and rejected re-edit boundary
- reverse linkage display
- CSV/OCR-related flows

---

## 4. Generation Constraints

### 4.1 Existing-capability verification constraint
- Do not write tests as if the feature were not implemented.
- Prefer verification of current capability plus gap identification.
- Clearly separate already testable behavior, BUS-required behavior, and pending items.

### 4.2 Required sections
The output must contain at least:
1. current test scope summary
2. core workflow test matrix
3. API verification guidance
4. UI verification guidance
5. accounting-rule verification guidance
6. batch/concurrency/idempotency verification guidance
7. precision and rounding verification guidance
8. import/linkage verification guidance
9. regression and UAT focus
10. pending gaps and follow-up items

### 4.3 Coverage topics
The generated result must explicitly cover where applicable:
- create / edit / delete voucher
- submit / approve / reject / post / batch post
- void / reverse voucher
- list query and detail query
- unbalanced voucher rejection
- invalid period rejection
- invalid state transition rejection
- rejected-state re-edit
- backend error display on save
- reverse linkage display
- CSV/OCR import verification

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Test Guidance

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

---

## 6. Example

| Case ID | Scenario | Preconditions | Steps | Expected Result | Layer |
|---|---|---|---|---|---|
| VCH-001 | Save draft voucher | user has edit permission | create voucher with valid lines and save | saved successfully in draft-compatible state | API/UI |
| VCH-002 | Submit balanced voucher | draft voucher exists | submit voucher | state moves to pending approval | API/E2E |
| VCH-003 | Submit unbalanced voucher | draft voucher exists | make debit and credit totals unequal and submit | submission rejected with business error | API/UI |
| VCH-004 | Post approved voucher | voucher approved | execute posting | posting succeeds and GL entries are generated | API/Data |
| VCH-005 | Batch post vouchers | multiple approved vouchers exist | call batch posting API | result aligns with defined batch strategy | API/Data |

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it validates actual current capability rather than generic theory;
2. it covers workflow, validation, precision, posting, and linkage concerns;
3. it can directly drive formal test deliverables;
4. it stays aligned with dictionary / API / backend / frontend / SQL prompts;
5. it marks unclear BUS items explicitly.
