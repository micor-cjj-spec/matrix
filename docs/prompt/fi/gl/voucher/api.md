# GL Voucher API Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate the API contract document for the General Ledger voucher scene based on BUS and current implementation signals.

---

## 2. Generation Objective

Generate a structured API contract document for the voucher scene. The result must support frontend/backend alignment, DTO/VO design, API testing, and review.

The generated result must cover at least:

1. API boundary and responsibility
2. API inventory
3. query APIs
4. write APIs
5. workflow APIs
6. request/response schema
7. pagination/detail schema
8. error-code semantics
9. permission and idempotency requirements
10. pending confirmation items

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- user actions
- workflow actions
- filter conditions
- detail content
- permissions and edit boundaries

### 3.2 Dictionary context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`

### 3.3 Current implementation signals
Current implementation history shows evidence of:
- voucher CRUD flow
- date-range list query
- posting-related APIs
- synchronous batch posting API
- analysis endpoints
- import-related flows in UI

These signals should be reflected as alignment context.

---

## 4. Generation Constraints

### 4.1 Contract constraints
- Do not invent major business APIs without BUS support.
- Keep API field naming aligned with dictionary semantics.
- Separate query APIs, write APIs, workflow APIs, and helper APIs.
- Distinguish single-item operations and batch operations.

### 4.2 Required sections
The output must contain at least:
1. scene/API boundary
2. API inventory
3. query APIs
4. write APIs
5. workflow APIs
6. common response schema
7. error codes
8. permission and idempotency notes
9. pending confirmation items

### 4.3 Per-API requirements
For each important API, include:
- purpose
- URI suggestion
- method suggestion
- request fields
- response fields
- preconditions
- permissions
- idempotency requirements
- error codes

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher API Contract

## 1. Scene and API Boundary
## 2. API Inventory
## 3. Query APIs
## 4. Write APIs
## 5. Workflow APIs
## 6. Common Request/Response Schema
## 7. Error Code Definitions
## 8. Permission and Idempotency Notes
## 9. Pending Confirmation Items
```

---

## 6. Example

| API Name | URI Suggestion | Method | Type | Description |
|---|---|---|---|---|
| Query Voucher List | `/api/gl/vouchers` | GET | Query | query vouchers by number, period, status, date range |
| Query Voucher Detail | `/api/gl/vouchers/{id}` | GET | Query | query voucher header and lines |
| Save Voucher | `/api/gl/vouchers` | POST | Write | create or save draft voucher |
| Submit Voucher | `/api/gl/vouchers/{id}/submit` | POST | Workflow | move draft voucher to pending approval |
| Approve Voucher | `/api/gl/vouchers/{id}/approve` | POST | Workflow | approve voucher |
| Post Voucher | `/api/gl/vouchers/{id}/post` | POST | Workflow | post voucher and generate GL entries |
| Batch Post Vouchers | `/api/gl/vouchers/batch-post` | POST | Workflow | synchronous batch posting |

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it covers current voucher query, write, and workflow capability;
2. it stays aligned with dictionary semantics;
3. it supports frontend/backend/test alignment;
4. it explicitly handles batch posting, error semantics, and permissions where relevant;
5. it marks unclear BUS items instead of inventing contract details.
