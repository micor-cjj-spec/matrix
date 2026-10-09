# General Ledger Voucher Backend Prompt

## 1. Corresponding BUS Source

- Domain: Finance Cloud
- Module: General Ledger
- Submodule: Voucher Processing
- Scene: Voucher
- BUS directory: `docs/bus/finance-cloud/general-ledger/voucher-processing/voucher/`

This prompt is used to generate or refine backend implementation guidance for the General Ledger voucher scene based on BUS documents **and** the current backend implementation status in `matrix`.

The generated result must align with the current code capability rather than starting from zero.

Observed backend implementation signals from the current repository history include at least the following capability points:

- voucher lines support
- balance validation
- GL posting entries generation
- synchronous batch posting API
- currency precision and rounding rules
- approval flow boundary and rejected-edit boundary
- date-range filters in voucher list API
- voucher analysis endpoints

These existing capabilities must be treated as important context when generating backend guidance.

---

## 2. Generation Objective

Based on the BUS scene and current backend implementation, generate a backend delivery-oriented design prompt for the voucher scene. The output must help AI or developers continue evolving the existing implementation in `matrix`, rather than rebuilding an unrelated solution.

The generated backend guidance must cover at least:

1. module boundary and responsibility split
2. controller/API responsibility
3. service orchestration and transaction boundary
4. domain objects and aggregates
5. voucher header and voucher line persistence model
6. balance validation and accounting validation rules
7. approval / posting / reverse / void workflow handling
8. batch posting handling
9. rounding and currency precision handling
10. error handling and business exception design
11. permissions, audit, idempotency, and edit boundary
12. extension points for analysis, OCR import, or upstream voucher generation

The generated result should be directly usable for:

- controller/service/repository refinement
- DTO / VO / entity alignment
- transaction and workflow implementation refinement
- code review guidance
- consistency checks against BUS and current code

---

## 3. Input Context

Generation must consume the following context together.

### 3.1 BUS context
Focus on extracting:

- voucher business objective
- voucher lifecycle and state transitions
- accounting constraints
- posting rules
- approval and reverse-operation rules
- relations with organization, ledger, accounting period, source documents, and accounting subjects

### 3.2 Prompt semantic context
The generated backend guidance must align with:

- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/dictionary/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/api/README.md`

### 3.3 Existing backend implementation context
When generating backend guidance, explicitly consider the fact that the current codebase already shows evidence of:

- voucher line modeling
- debit/credit balance checks
- posting entry generation into GL entries
- synchronous batch posting capability
- currency precision and rounding enforcement
- approval workflow boundary handling
- list filtering with date range
- analytical endpoints around voucher data

### 3.4 Engineering target context
The target implementation is the existing backend project `matrix`, so the generated guidance must be suitable for an already-developed Java backend, not a greenfield pseudo solution.

---

## 4. Generation Constraints

### 4.1 Existing-code-first constraint

- Do not generate a fully greenfield architecture that ignores the current implementation status.
- Prefer “refine / align / extend current implementation” over “rebuild all layers”.
- Clearly distinguish between **already implemented capability**, **capability inferred from BUS**, and **capability still to be completed**.

### 4.2 Structure constraint

The output must contain at least these sections:

1. current backend capability summary
2. backend module boundary
3. core domain model
4. controller layer guidance
5. service orchestration guidance
6. repository / persistence guidance
7. validation and accounting rule handling
8. workflow and state transition handling
9. transaction, idempotency, and concurrency guidance
10. exception and error-code guidance
11. permission and audit guidance
12. gaps and follow-up items

### 4.3 Backend design constraint

The generated guidance must explicitly address the following topics where applicable:

- voucher header + voucher line split
- save vs submit vs approve vs post vs void vs reverse responsibilities
- state-based edit boundary
- rejected status re-edit boundary
- debit/credit total balance check
- accounting period open/close check
- posting precondition checks
- batch posting strategy
- rounding and currency precision strategy
- source voucher linkage or reverse linkage handling
- audit trail fields and operator tracking

### 4.4 Transaction and consistency constraint

The guidance must explicitly state:

- which operations should be transactional
- where optimistic checks or state preconditions are required
- which operations need idempotency
- how batch posting should handle partial failure or fail-fast strategy
- how posting entries and voucher status changes stay consistent

### 4.5 Security and permission constraint

The guidance must include backend-side handling for:

- query permission
- edit permission
- approval permission
- posting permission
- void / reverse permission
- data visibility boundary if present in BUS
- audit logging for critical operations

---

## 5. Output Requirements

Output must be a Markdown design-oriented prompt result.

Recommended structure:

```md
# Voucher Backend Implementation Guidance

## 1. Current Backend Capability Summary
## 2. Backend Module Boundary
## 3. Core Domain Model
### 3.1 Voucher Header
### 3.2 Voucher Line
### 3.3 Related Objects
## 4. Controller Layer Guidance
## 5. Service Orchestration Guidance
## 6. Repository and Persistence Guidance
## 7. Validation and Accounting Rule Handling
## 8. Workflow and State Transition Handling
## 9. Transaction / Idempotency / Concurrency Handling
## 10. Exception and Error-Code Guidance
## 11. Permission and Audit Guidance
## 12. Gaps and Follow-up Items
```

The result must clearly label statements as one of the following when necessary:

- already implemented
- should align with BUS
- recommended enhancement
- pending confirmation

---

## 6. Example

### 6.1 Example current capability summary

- Already implemented: voucher line support
- Already implemented: debit/credit balance validation before submission or posting
- Already implemented: GL posting entry generation during posting flow
- Already implemented: synchronous batch posting API
- Already implemented: currency precision and rounding rule enforcement
- Already implemented: approval flow boundary and rejected-edit boundary
- Already implemented: voucher list date-range filter
- Already implemented: voucher analysis endpoints

### 6.2 Example service orchestration guidance

- `saveVoucher`: persist voucher header and lines in draft-compatible state, validate required fields, but keep posting-only checks out of save if BUS allows draft incompleteness.
- `submitVoucher`: validate voucher structure completeness, debit/credit balance, accounting period, and subject validity, then move state from draft to pending approval.
- `approveVoucher`: enforce approval permission and state precondition, then move to approved state and record approver metadata.
- `postVoucher`: enforce approved-state precondition, posting permission, accounting period availability, and currency rounding rules, then generate GL posting entries and update posting status transactionally.
- `batchPostVouchers`: define whether fail-fast or partial success is allowed; if partial success is allowed, response must return per-voucher result details.

### 6.3 Example gaps section

- BUS confirmation needed: whether reverse-post and unapprove are both supported in this scene.
- BUS confirmation needed: whether voucher delete is allowed only in draft state or also in rejected state.
- Recommended enhancement: separate posting domain service from general voucher edit service if current service becomes too large.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:

1. it reflects the current backend implementation signals instead of ignoring them;
2. it can directly guide continued development in `matrix`;
3. it covers validation, workflow, transaction, precision, and batch-posting concerns;
4. it aligns with dictionary and API prompts;
5. it explicitly marks unknown BUS items instead of inventing rules.
