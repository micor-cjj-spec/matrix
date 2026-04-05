# GL Voucher Frontend Prompt

## 1. Corresponding BUS Source

- Domain: `fi`
- Subdomain: `gl`
- Biz code: `voucher`
- BUS directory: `docs/bus/fi/gl/voucher/`

This prompt is used to generate or refine frontend implementation guidance for the GL voucher scene based on BUS and current frontend implementation signals in `matrix-web`.

Observed frontend implementation signals include at least:
- full voucher CRUD and toolbar workflow page
- voucher line editor
- filters, jump links, and print template entry
- reverse linkage display in list
- backend error surfacing during save
- CSV import
- OCR preview / confirm / line editing flows
- voucher summary and carry list pages

---

## 2. Generation Objective

Generate frontend guidance that helps AI or developers continue refining the existing implementation in `matrix-web` rather than designing an unrelated brand-new page.

The generated result must cover at least:
1. page boundary and information architecture
2. list page guidance
3. detail/edit page guidance
4. toolbar action guidance
5. voucher line editor guidance
6. validation and error-display guidance
7. import / print / linkage guidance
8. permission and state-driven UI guidance
9. gaps and follow-up items

---

## 3. Input Context

### 3.1 BUS context
Extract from BUS:
- user goals
- workflow actions
- filters and page content
- editable fields and readonly boundaries
- permission-sensitive actions

### 3.2 Prompt alignment context
Must align with:
- `docs/prompt/fi/gl/voucher/dictionary.md`
- `docs/prompt/fi/gl/voucher/api.md`
- `docs/prompt/fi/gl/voucher/backend.md`

### 3.3 Current implementation context
Treat current capability signals as important alignment inputs, including:
- existing voucher CRUD and toolbar workflow
- line editor interaction
- filters / jump links / print support
- reverse linkage display
- backend error surfacing
- CSV/OCR-related flows
- voucher summary/carry pages

---

## 4. Generation Constraints

### 4.1 Existing-page-first constraint
- Do not redesign from scratch if current capability already exists.
- Prefer refine / align / improve over replace.
- Clearly separate already implemented UI capability, BUS-required behavior, and pending items.

### 4.2 Required sections
The output must contain at least:
1. current frontend capability summary
2. page structure and routing guidance
3. list page guidance
4. detail/edit page guidance
5. voucher line editor guidance
6. toolbar and workflow action guidance
7. validation and error-display guidance
8. import / print / linkage guidance
9. permission and state-driven UI guidance
10. pending gaps and follow-up items

### 4.3 Interaction topics
The generated result must explicitly address where applicable:
- list filters including date range / status / keyword
- create/edit/view mode split
- line add/edit/delete behavior
- debit/credit balance indication
- state-based enable/disable logic
- rejected-state re-edit boundary
- reverse linkage display and jump behavior
- print/export entry
- import flow usability and confirmation
- backend validation message surfacing

---

## 5. Output Requirements

Output must be a Markdown document.

Recommended structure:

```md
# Voucher Frontend Guidance

## 1. Current Frontend Capability Summary
## 2. Page Structure and Routing Guidance
## 3. List Page Guidance
## 4. Detail / Edit Page Guidance
## 5. Voucher Line Editor Guidance
## 6. Toolbar and Workflow Action Guidance
## 7. Validation and Error-Display Guidance
## 8. Import / Print / Linkage Guidance
## 9. Permission and State-Driven UI Guidance
## 10. Pending Gaps and Follow-up Items
```

---

## 6. Example

- In draft state, expose save, submit, edit-line, import-line, and delete if BUS allows draft deletion.
- In pending approval, disable editable line operations and expose approve/reject only for users with permission.
- In approved state, expose post action and keep content readonly unless BUS defines unapprove flow.
- In posted state, render main content readonly and expose reverse-related action only if supported.
- Show live debit/credit total difference before submit, but rely on backend for final business validation.

---

## 7. Acceptance Criteria

The generated result is acceptable only if:
1. it reflects current frontend implementation signals;
2. it can directly guide continued development in `matrix-web`;
3. it covers list, detail, line editor, toolbar workflow, validation, and import/linkage concerns;
4. it stays aligned with dictionary / API / backend prompts;
5. it marks unclear BUS items explicitly.
