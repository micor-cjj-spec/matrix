# GL Voucher Review Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate review and consistency-check guidance for the GL voucher scene based on BUS, prompt documents, and current implementation signals in `matrix` and `matrix-web`.

Observed implementation signals include at least:
- voucher CRUD and workflow pages
- voucher line editor
- balance validation
- posting entry generation
- batch posting API
- precision and rounding rules
- approval boundary and rejected edit boundary
- date-range filtering
- reverse linkage display
- CSV/OCR-related flows
- analysis-related capability

---

## 2. Generation Objective

Generate review-oriented guidance that checks consistency across:
- BUS
- prompt documents
- backend implementation
- frontend implementation
- future deliverables

The generated result must cover at least:
1. BUS to prompt consistency
2. prompt to backend consistency
3. prompt to frontend consistency
4. backend to frontend contract consistency
5. workflow/state consistency
6. field/enum/meaning consistency
7. validation and precision consistency
8. deliverable completeness
9. risks and follow-up actions

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- business objective
- workflow and states
- validation rules
- accounting constraints
- permissions and edit boundaries
- source/reverse relations

### 3.2 Prompt context
Must review against:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/backend.md`
- `docs/prompt/fi/gl/voucher/frontend.md`
- `docs/prompt/fi/gl/voucher/sql.md`
- `docs/prompt/fi/gl/voucher/test.md`

### 3.3 Current implementation context
Consider current signals from backend and frontend, including:
- voucher lines and CRUD flow
- validation and posting logic
- approval boundary and rejected edit boundary
- batch posting
- precision handling
- reverse linkage display
- import-related flows
- analysis capability

---

## 4. Generation Constraints

### 4.1 Cross-layer consistency constraint
The review must compare across:
- BUS vs dictionary
- BUS vs API
- dictionary vs backend model
- API vs frontend interaction
- backend state handling vs frontend action enablement
- SQL assumptions vs backend validation
- test coverage vs implemented capability

### 4.2 Required sections
The output must contain at least:
1. current review scope summary
2. BUS to prompt consistency review
3. prompt to backend consistency review
4. prompt to frontend consistency review
5. backend to frontend contract consistency review
6. workflow and state consistency review
7. validation and precision consistency review
8. deliverable completeness review
9. major risks and debt items
10. follow-up recommendations

### 4.3 Gap-marking constraint
For each issue, label one of:
- BUS missing
- prompt missing
- code missing
- code exists but prompt not aligned
- prompt defined but implementation not confirmed
- needs business confirmation

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Review Guidance

## 1. Current Review Scope Summary
## 2. BUS to Prompt Consistency Review
## 3. Prompt to Backend Consistency Review
## 4. Prompt to Frontend Consistency Review
## 5. Backend to Frontend Contract Consistency Review
## 6. Workflow and State Consistency Review
## 7. Validation and Precision Consistency Review
## 8. Deliverable Completeness Review
## 9. Major Risks and Debt Items
## 10. Follow-up Recommendations
```

---

## 6. Example

| Review Item | Layer A | Layer B | Status | Finding | Action |
|---|---|---|---|---|---|
| Voucher status naming | BUS | dictionary | partially aligned | business wording and enum wording need mapping | normalize state naming |
| Rejected editable rule | BUS | frontend | pending confirmation | UI should support rejected re-edit only if BUS allows | confirm business rule and update prompt/UI |
| Batch posting | backend | test | partially aligned | capability exists but regression matrix may be incomplete | add batch posting regression cases |
| Reverse linkage | frontend | SQL | partially aligned | UI already shows linkage info, storage guidance must support relation field | refine SQL guidance |

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it reviews cross-layer consistency rather than one artifact only;
2. it reflects existing implementation signals from backend and frontend;
3. it can directly drive review checklists and future deliverables;
4. it identifies missing or conflicting items explicitly;
5. it does not invent business conclusions when BUS is unclear.
