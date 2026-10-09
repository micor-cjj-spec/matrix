# General Ledger Voucher Frontend Prompt

## 1. Corresponding BUS Source

- Domain: Finance Cloud
- Module: General Ledger
- Submodule: Voucher Processing
- Scene: Voucher
- BUS directory: `docs/bus/finance-cloud/general-ledger/voucher-processing/voucher/`

This prompt is used to generate or refine frontend implementation guidance for the General Ledger voucher scene based on BUS documents **and** the current frontend implementation status in `matrix-web`.

The generated result must align with the current page and interaction capability rather than assuming a blank new page.

Observed frontend implementation signals from the current repository history include at least the following capability points:

- full voucher toolbar workflow and CRUD page
- voucher line editor
- filters, jump links, and print template
- reverse linkage display in voucher list
- backend error message surfacing during save
- CSV import for vouchers
- OCR import preview and confirm flow
- OCR line editing and integrated import
- voucher summary and carry list pages

These current capabilities must be considered first when generating frontend guidance.

---

## 2. Generation Objective

Based on the BUS scene and current frontend implementation, generate a frontend delivery-oriented prompt for the voucher scene. The output must guide AI or developers to continue refining the existing implementation in `matrix-web`.

The generated guidance must cover at least:

1. page boundary and information architecture
2. voucher list page design
3. voucher create/edit/detail page design
4. toolbar actions and state-based visibility
5. voucher line editor interaction
6. filters, search, and jump-link interactions
7. save/submit/approve/post/void/reverse interaction flow
8. backend error display strategy
9. import flows including CSV/OCR-related interaction if relevant to BUS
10. print/export interaction
11. status, reverse linkage, and traceability display
12. frontend validation and UX fallback rules

The result should be directly usable for:

- page refinement in `matrix-web`
- interaction consistency checks
- UI review and testing guidance
- aligning frontend behavior with BUS and backend capability

---

## 3. Input Context

Generation must consume the following context together.

### 3.1 BUS context
Focus on extracting:

- voucher user roles and user goals
- voucher list / detail / edit lifecycle
- workflow actions and state transitions
- accounting validation rules that should have frontend hints or blockers
- fields and relationships that must be shown or editable

### 3.2 Prompt semantic context
The generated frontend guidance must align with:

- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/dictionary/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/api/README.md`
- `docs/prompt/finance-cloud/general-ledger/voucher-processing/voucher/backend/README.md`

### 3.3 Existing frontend implementation context
When generating frontend guidance, explicitly consider the fact that the current codebase already shows evidence of:

- full CRUD voucher page and workflow toolbar
- line editor interaction
- search/filter capability and jump links
- print template support
- reverse linkage display in list page
- backend error message surfacing while saving
- CSV import support
- OCR preview / confirm / line editing flow
- voucher summary and carry-related list pages

### 3.4 Engineering target context
The target implementation is the existing frontend project `matrix-web`, so the generated guidance must fit an already-built page/module rather than a purely conceptual UI spec.

---

## 4. Generation Constraints

### 4.1 Existing-page-first constraint

- Do not redesign the page from scratch if the capability is already present.
- Prefer “refine / align / improve current interactions” over “replace with a brand-new workflow”.
- Clearly distinguish between already implemented UI capability, BUS-required behavior, and pending enhancement items.

### 4.2 Structure constraint

The output must contain at least these sections:

1. current frontend capability summary
2. page structure and routing boundary
3. list page guidance
4. detail / edit page guidance
5. voucher line editor guidance
6. toolbar and workflow-action guidance
7. validation and error-display guidance
8. import / print / linkage guidance
9. permission and state-driven UI guidance
10. pending gaps and follow-up items

### 4.3 Interaction constraint

The generated guidance must explicitly address the following topics where applicable:

- list filters including status/date range/keyword conditions
- create / edit / view mode split
- voucher line add / edit / delete behavior
- debit/credit balance indication on page
- state-based action enable/disable logic
- rejected-state edit boundary
- reverse linkage display and jump behavior
- printing or export entry
- import flow usability and confirmation steps
- backend validation message surfacing

### 4.4 Validation and UX constraint

The result must distinguish:

- frontend pre-check validation
- backend-returned business validation
- blocking vs non-blocking hints
- save-draft tolerance vs submit/post strictness

### 4.5 Permission and security constraint

The generated guidance must state frontend-side handling for:

- route/page access control
- toolbar action visibility by permission
- disabled state by voucher status
- sensitive field display rules if defined in BUS
- audit-related readonly display where needed

---

## 5. Output Requirements

Output must be a Markdown design-oriented prompt result.

Recommended structure:

```md
# Voucher Frontend Implementation Guidance

## 1. Current Frontend Capability Summary
## 2. Page Structure and Routing Boundary
## 3. List Page Guidance
## 4. Detail / Edit Page Guidance
## 5. Voucher Line Editor Guidance
## 6. Toolbar and Workflow-Action Guidance
## 7. Validation and Error-Display Guidance
## 8. Import / Print / Linkage Guidance
## 9. Permission and State-Driven UI Guidance
## 10. Pending Gaps and Follow-up Items
```

The result should clearly label statements as one of the following when needed:

- already implemented
- should align with BUS
- recommended enhancement
- pending confirmation

---

## 6. Example

### 6.1 Example current capability summary

- Already implemented: voucher CRUD page and toolbar workflow
- Already implemented: voucher line editor
- Already implemented: list filters, jump links, and print template entry
- Already implemented: reverse linkage info display in voucher list
- Already implemented: backend error message surfacing when save fails
- Already implemented: CSV import
- Already implemented: OCR import preview, line editing, and confirm flow
- Already implemented: voucher summary and carry list pages

### 6.2 Example toolbar guidance

- In draft status, enable save, submit, edit-line, import-line, and delete if BUS allows draft deletion.
- In pending approval status, disable editable line operations and expose approve/reject actions only to users with approval permission.
- In approved status, expose posting action; disable content editing unless BUS defines unapprove flow.
- In posted status, render page mainly readonly and expose reverse / reverse-post related entry only if BUS and backend support it.
- In rejected status, allow re-edit only if BUS explicitly allows rejected vouchers to return to editable state.

### 6.3 Example validation guidance

- Frontend should provide immediate empty-field, amount-format, and basic line completeness validation.
- Frontend should show live debit/credit total difference and warn before submit.
- Final business validation must still rely on backend response.
- Save action should surface backend error messages directly when the backend rejects save due to business rules or precision rules.

### 6.4 Example gap section

- BUS confirmation needed: whether print is allowed only after approval/posting or at all visible states.
- BUS confirmation needed: whether CSV and OCR import both belong to the core voucher scene or an extended scene.
- Recommended enhancement: separate list toolbar and detail toolbar permission map into a reusable config layer if current page logic is becoming too coupled.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:

1. it reflects the current frontend implementation signals instead of ignoring them;
2. it can directly guide continued development in `matrix-web`;
3. it covers list, detail, line editor, toolbar workflow, validation, and import/linkage interactions;
4. it aligns with dictionary, API, and backend prompts;
5. it explicitly marks unknown BUS items instead of inventing UI behavior.
