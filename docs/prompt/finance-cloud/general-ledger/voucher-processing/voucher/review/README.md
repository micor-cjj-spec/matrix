# General Ledger Voucher Review Prompt

## 1. Corresponding BUS Source

- Domain: Finance Cloud
- Module: General Ledger
- Submodule: Voucher Processing
- Scene: Voucher
- BUS directory: `docs/bus/finance-cloud/general-ledger/voucher-processing/voucher/`

This prompt is used to generate review and consistency-check guidance for the General Ledger voucher scene based on BUS documents, prompt documents, and the current code/implementation signals.

The generated result must be used to verify whether the voucher scene is internally consistent across:

- BUS
- prompt documents
- backend implementation in `matrix`
- frontend implementation in `matrix-web`
- future non-code deliverables in `docs/deliverables/{scene}/`

Observed implementation signals that must be considered include at least:

- voucher CRUD and workflow pages
- voucher line editor
- debit/credit balance validation
- posting entry generation
- batch posting API
- currency precision and rounding rules
- approval flow and rejected-edit boundary
- list date-range filter
- reverse linkage display
- CSV / OCR import related flows
- analysis-related voucher endpoints/pages

---

## 2. Generation Objective

Based on BUS, prompt files, and current implementation signals, generate a review-oriented prompt result for the voucher scene. The output must help reviewers check whether the scene is aligned end-to-end and identify missing pieces, contradiction points, or technical debt.

The generated review guidance must cover at least:

1. BUS to prompt consistency
2. prompt to backend consistency
3. prompt to frontend consistency
4. backend to frontend contract consistency
5. workflow/state consistency
6. field/enum/meaning consistency
7. validation consistency
8. deliverable completeness check
9. risk identification and follow-up recommendations

---

## 3. Input Context

Generation must consume the following context together.

### 3.1 BUS context
Focus on extracting:

- business objective
- workflow and states
- validation rules
- accounting constraints
- permissions and edit boundaries
- source/reverse/linkage relations

### 3.2 Prompt context
The generated review guidance must align with and review consistency against:

- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/dictionary/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/api/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/backend/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/frontend/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/sql/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/test/README.md`

### 3.3 Code and implementation context
Explicitly consider current capability signals from backend/frontend history, including:

- voucher lines and CRUD flow
- validation and posting logic
- approval boundary and rejected edit boundary
- batch posting support
- currency precision handling
- reverse linkage display
- import-related flow
- analysis capability

### 3.4 Deliverable context
The review result should be usable later for checking formal artifacts under:

- `docs/deliverables/finance-cloud/general-ledger/voucher-processing/voucher/`

---

## 4. Generation Constraints

### 4.1 Cross-layer consistency constraint

The review must not stay at one layer only. It must explicitly compare and check across:

- BUS vs dictionary
- BUS vs API
- dictionary vs backend model
- API vs frontend interaction
- backend state handling vs frontend action enablement
- SQL assumptions vs backend validation
- test coverage vs implemented capability

### 4.2 Structure constraint

The output must contain at least these sections:

1. current scene review scope summary
2. BUS to prompt consistency review
3. prompt to backend consistency review
4. prompt to frontend consistency review
5. backend to frontend contract consistency review
6. workflow and state consistency review
7. validation and precision consistency review
8. deliverable completeness review
9. major risks and debt items
10. follow-up recommendations

### 4.3 Review checklist constraint

The generated result must explicitly review where applicable:

- whether voucher states are consistently named across all layers
- whether list/detail/edit fields align with dictionary
- whether API actions align with toolbar actions
- whether rejected-state edit rule is consistent across BUS/backend/frontend
- whether posting and batch-posting rules are consistent across BUS/backend/test
- whether precision and rounding are consistently described across backend/SQL/test
- whether reverse linkage is modeled consistently across UI/API/data guidance
- whether import flows are clearly in-scope or out-of-scope for the current scene

### 4.4 Gap-marking constraint

For every inconsistency or uncertainty, the result must label one of the following:

- BUS missing
- prompt missing
- code missing
- code already exists but prompt not aligned
- prompt defined but implementation not confirmed
- needs business confirmation

---

## 5. Output Requirements

Output must be a Markdown review-oriented prompt result.

Recommended structure:

```md
# Voucher Consistency and Review Guidance

## 1. Current Scene Review Scope Summary
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

Where useful, review output should use a table like:

| Review Item | Layer A | Layer B | Status | Finding | Action |
|---|---|---|---|---|---|

Status values may include:

- aligned
- partially aligned
- missing
- pending confirmation
- conflict

---

## 6. Example

### 6.1 Example review table

| Review Item | Layer A | Layer B | Status | Finding | Action |
|---|---|---|---|---|---|
| Voucher status naming | BUS | dictionary | partially aligned | BUS uses business wording, dictionary needs normalized enum naming | align enum set and add mapping |
| Rejected editable rule | BUS | frontend | pending confirmation | frontend should support rejected re-edit if BUS allows, but rule boundary not yet fully explicit | confirm with business and update prompt/UI |
| Batch posting | backend | test | partially aligned | batch posting capability exists, but dedicated regression matrix may be incomplete | add batch posting regression cases |
| Reverse linkage display | frontend | SQL guidance | partially aligned | UI already shows linkage info, SQL trace/storage guidance must explicitly support relation field | add reverse-link relation guidance |

### 6.2 Example risks section

- Risk: prompt documents may lag behind actual implemented capability if code evolves quickly.
- Risk: OCR/CSV import may be treated as core scene behavior in frontend but not fully documented in BUS.
- Risk: precision and rounding rules may be enforced in backend only unless test and SQL guidance are aligned.

### 6.3 Example follow-up section

- Normalize state naming and status transition wording across BUS, dictionary, backend, and frontend.
- Add explicit review of batch posting success/failure strategy.
- Clarify whether import-related capability belongs to this core voucher scene or an extension scene.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:

1. it reviews cross-layer consistency rather than a single artifact only;
2. it reflects existing implementation signals from both backend and frontend;
3. it can directly drive review checklists and future deliverables under `docs/deliverables`;
4. it identifies missing or conflicting items explicitly;
5. it does not invent business conclusions when BUS is unclear.
