# GL Voucher Mock Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate mock and sample data guidance for the GL voucher scene based on BUS and current implementation signals.

Observed implementation signals include at least:
- voucher CRUD and workflow page
- voucher line editor
- date-range filtering
- reverse linkage display
- backend error surfacing during save
- batch posting capability
- CSV/OCR-related flows
- analysis-related capability

---

## 2. Generation Objective

Generate mock-oriented guidance that supports formal deliverables such as request/response samples, demo data, edge-case samples, UI demo fixtures, and API validation samples for the voucher scene.

The generated result must cover at least:
1. core request samples
2. core response samples
3. list/detail sample data
4. workflow-action sample data
5. error-response samples
6. reverse-linkage samples
7. batch-posting samples
8. import-related sample data
9. edge and boundary samples
10. pending sample gaps and follow-up items

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- key business objects
- page display fields
- workflow actions
- validation rules
- status transitions
- source/reverse relations

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/frontend.md`
- `docs/prompt/fi/gl/voucher/backend.md`
- `docs/prompt/fi/gl/voucher/test.md`

### 3.3 Current implementation context
Consider current capability signals including:
- list and detail page display
- line editor data structure
- workflow action requests/responses
- reverse linkage display needs
- batch posting request/response needs
- import-related preview/confirm data shape

---

## 4. Generation Constraints

### 4.1 Existing-capability-alignment constraint
- Do not generate mock data unrelated to current voucher capability.
- Prefer sample sets that support current pages, APIs, tests, and demos.
- Clearly separate stable sample data, edge-case sample data, and pending confirmation data.

### 4.2 Required sections
The output must contain at least:
1. sample scope summary
2. list query sample data
3. detail sample data
4. create/save sample data
5. workflow-action sample data
6. error sample data
7. reverse-linkage sample data
8. batch-posting sample data
9. import-related sample data
10. edge-case samples and pending gaps

### 4.3 Sample topics
The generated result must explicitly provide or describe where applicable:
- balanced voucher sample
- unbalanced voucher sample
- draft voucher sample
- pending approval voucher sample
- approved voucher sample
- posted voucher sample
- rejected voucher sample
- reverse-linked voucher sample
- batch-post request and result sample
- save-error response sample
- CSV/OCR preview sample if in scope

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Mock Guidance

## 1. Sample Scope Summary
## 2. List Query Sample Data
## 3. Detail Sample Data
## 4. Create / Save Sample Data
## 5. Workflow-Action Sample Data
## 6. Error Sample Data
## 7. Reverse-Linkage Sample Data
## 8. Batch-Posting Sample Data
## 9. Import-Related Sample Data
## 10. Edge-Case Samples and Pending Gaps
```

Use JSON blocks where useful.

---

## 6. Example

### 6.1 Example list item

```json
{
  "id": 1001,
  "voucherNo": "GL202604060001",
  "accountingDate": "2026-04-06",
  "accountingPeriod": "2026-04",
  "status": "APPROVED",
  "totalDebit": 1000.00,
  "totalCredit": 1000.00,
  "hasReverseLink": false
}
```

### 6.2 Example detail sample

```json
{
  "id": 1001,
  "voucherNo": "GL202604060001",
  "status": "DRAFT",
  "lines": [
    {
      "lineNo": 1,
      "accountCode": "660101",
      "drCrDirection": "DEBIT",
      "amount": 1000.00,
      "summary": "Office expense"
    },
    {
      "lineNo": 2,
      "accountCode": "100201",
      "drCrDirection": "CREDIT",
      "amount": 1000.00,
      "summary": "Bank payment"
    }
  ]
}
```

### 6.3 Example error response

```json
{
  "code": "GL_VOUCHER_003",
  "message": "Debit and credit are not balanced",
  "details": {
    "totalDebit": 1000.00,
    "totalCredit": 900.00
  }
}
```

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it supports current list/detail/workflow/test/demo needs;
2. it covers balanced, unbalanced, workflow-state, linkage, and error samples;
3. it stays aligned with dictionary / API / frontend / backend / test prompts;
4. it can drive formal mock/sample deliverables;
5. it marks unclear BUS items explicitly.
