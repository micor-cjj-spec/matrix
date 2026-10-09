# GL Voucher Backend Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate or refine backend implementation guidance for the GL voucher scene based on BUS and current backend implementation signals in `matrix`.

Observed backend implementation signals include at least:
- voucher lines support
- debit/credit balance validation
- GL posting entries generation
- synchronous batch posting API
- currency precision and rounding enforcement
- approval flow boundary and rejected-edit boundary
- date-range filters in list API
- voucher analysis endpoints

---

## 2. Generation Objective

Generate backend guidance that helps AI or developers continue refining the existing implementation in `matrix` rather than rebuilding a disconnected solution.

The generated result must cover at least:
1. module boundary
2. controller responsibility
3. service orchestration
4. domain object split
5. persistence model
6. accounting validation
7. workflow/state handling
8. transaction boundary
9. batch posting handling
10. precision handling
11. permission and audit handling
12. extension points and gaps

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- business goal
- lifecycle and state transitions
- validation rules
- posting rules
- reverse / void constraints
- relations with period, organization, ledger, source docs, subjects

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`

### 3.3 Current implementation context
Treat current capability signals as important alignment inputs, including:
- voucher line modeling
- balance checks
- posting entry generation
- batch posting
- precision / rounding rules
- approval boundary
- list filtering
- analysis endpoints

---

## 4. Generation Constraints

### 4.1 Existing-code-first constraint
- Do not ignore current implementation signals.
- Prefer refine / align / extend over rebuild.
- Clearly separate already implemented capability, BUS-required capability, and pending items.

### 4.2 Required sections
The output must contain at least:
1. current backend capability summary
2. module boundary
3. domain model guidance
4. controller guidance
5. service orchestration guidance
6. repository/persistence guidance
7. validation and accounting-rule guidance
8. workflow/state handling guidance
9. transaction/idempotency/concurrency guidance
10. exception and error-code guidance
11. permission and audit guidance
12. pending gaps and follow-up items

### 4.3 Backend design topics
The generated result must explicitly consider where applicable:
- voucher header + line split
- save vs submit vs approve vs post vs void vs reverse responsibilities
- rejected-state edit boundary
- debit/credit total checks
- period-open checks
- posting preconditions
- batch posting strategy
- precision and rounding strategy
- reverse/source linkage handling
- audit fields and operator tracking

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Backend Guidance

## 1. Current Backend Capability Summary
## 2. Module Boundary
## 3. Domain Model Guidance
## 4. Controller Guidance
## 5. Service Orchestration Guidance
## 6. Repository and Persistence Guidance
## 7. Validation and Accounting Rule Guidance
## 8. Workflow and State Handling Guidance
## 9. Transaction / Idempotency / Concurrency Guidance
## 10. Exception and Error-Code Guidance
## 11. Permission and Audit Guidance
## 12. Pending Gaps and Follow-up Items
```

---

## 6. Example

- `saveVoucher`: persist draft-compatible header and lines, validate required fields, but keep final posting checks for stricter workflow steps.
- `submitVoucher`: validate completeness, balance, period, and account validity, then move to pending approval.
- `approveVoucher`: enforce permission and state precondition, record approver metadata.
- `postVoucher`: enforce approved-state precondition, posting permission, precision rules, then generate GL entries transactionally.
- `batchPostVouchers`: define fail-fast vs partial-success strategy and response semantics.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it reflects current backend implementation signals;
2. it can directly guide continued development in `matrix`;
3. it covers validation, workflow, transaction, precision, and batch posting;
4. it stays aligned with dictionary and API prompts;
5. it marks unclear BUS items explicitly.
