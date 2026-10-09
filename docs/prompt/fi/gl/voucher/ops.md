# GL Voucher Ops Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate operations and release guidance for the GL voucher scene based on BUS and current implementation signals in `matrix` and `matrix-web`.

Observed implementation signals include at least:
- voucher CRUD and workflow pages
- voucher line editor
- backend posting logic and posting entry generation
- synchronous batch posting API
- precision and rounding rules
- approval boundary and rejected-edit boundary
- date-range filtering
- reverse linkage display
- CSV/OCR-related flows
- analysis-related capability

---

## 2. Generation Objective

Generate ops-oriented guidance that supports formal deliverables such as deployment notes, configuration notes, release checklist, monitoring checklist, rollback notes, and issue-handling runbooks for the voucher scene.

The generated result must cover at least:
1. release scope definition
2. deployment dependency check
3. backend/frontend release coordination
4. database/script dependency coordination
5. configuration and feature-switch guidance
6. monitoring and alert focus
7. batch posting operational concerns
8. import-related operational concerns
9. rollback and emergency handling
10. pending operational risks and follow-up items

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- critical business path
- high-risk workflow actions
- important validation boundaries
- user-visible impact points
- roles and permission-sensitive actions

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/backend.md`
- `docs/prompt/fi/gl/voucher/frontend.md`
- `docs/prompt/fi/gl/voucher/sql.md`
- `docs/prompt/fi/gl/voucher/test.md`
- `docs/prompt/fi/gl/voucher/review.md`

### 3.3 Current implementation context
Consider current capability signals including:
- voucher workflow actions already exposed in UI
- posting and batch posting already implemented in backend
- precision-sensitive accounting logic exists
- import-related flows may affect operational support
- reverse linkage and analysis capability may affect troubleshooting and audit checks

---

## 4. Generation Constraints

### 4.1 Existing-system constraint
- Do not write ops guidance like a greenfield deployment plan.
- Prefer release coordination, risk control, monitoring, and rollback guidance for an existing running system.
- Clearly distinguish current capability, required ops safeguards, and pending operational gaps.

### 4.2 Required sections
The output must contain at least:
1. current ops scope summary
2. release dependency checklist
3. deployment and rollout guidance
4. configuration and switch guidance
5. monitoring and alert guidance
6. batch posting operational guidance
7. import and data-trace operational guidance
8. rollback and emergency handling guidance
9. release acceptance checklist
10. pending risks and follow-up items

### 4.3 Operational focus topics
The generated result must explicitly consider where applicable:
- backend and frontend version compatibility
- API compatibility for workflow actions
- SQL/script release dependency
- posting-related operational safety
- batch posting timeout/failure/retry concerns
- precision-rule related production verification
- import flow support and troubleshooting boundaries
- business-visible error handling and log traceability
- permission-sensitive operation auditability

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Ops Guidance

## 1. Current Ops Scope Summary
## 2. Release Dependency Checklist
## 3. Deployment and Rollout Guidance
## 4. Configuration and Switch Guidance
## 5. Monitoring and Alert Guidance
## 6. Batch Posting Operational Guidance
## 7. Import and Data-Trace Operational Guidance
## 8. Rollback and Emergency Handling Guidance
## 9. Release Acceptance Checklist
## 10. Pending Risks and Follow-up Items
```

---

## 6. Example

- Before release, verify frontend toolbar actions and backend workflow APIs are version-compatible.
- If SQL changes are involved, ensure schema/data migration is completed before exposing new workflow actions.
- Monitor posting success rate, batch posting latency, and business-exception spikes after release.
- For precision-rule changes, verify persisted amounts and displayed amounts remain consistent in production.
- For CSV/OCR-related capability, ensure operation support staff can identify import source and failure cause from logs or trace fields.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it supports release and operational handling of an existing voucher capability;
2. it covers deployment, compatibility, monitoring, rollback, and operational risk items;
3. it stays aligned with API/backend/frontend/SQL/test/review prompts;
4. it is usable for formal ops deliverables;
5. it marks unclear BUS or system assumptions explicitly.
