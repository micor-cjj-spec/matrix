# Matrix Repository Instructions

## 1. Workspace context

Matrix is developed together with a sibling frontend repository.

When a task involves development, bug fixing, features, integration, UI, or API changes, consider both repositories:

- Backend: `matrix`
- Frontend: `matrix-web`

Local development convention currently uses:

- Backend: `C:\Users\20602\IdeaProjects\matrix`
- Frontend: `C:\Users\20602\IdeaProjects\matrix-web`

Use these paths only when they exist in the current environment. Do not assume every AI agent or CI runner has the same local path.

Routing rules:

1. Java, Spring Boot, gateway, authentication, finance services, MyBatis-Plus, configuration, and backend APIs belong primarily to `matrix`.
2. Pages, components, routes, UI behavior, frontend API calls, Vue, and Vite belong primarily to `matrix-web`.
3. Integration, login, permissions, vouchers, ledgers, and master-data flows may require inspecting both repositories.
4. Unless the task is explicitly scoped, consider whether a change has a cross-repository impact.

## 2. Project positioning

Matrix is an enterprise intelligent finance platform covering enterprise modeling, master data, general ledger, vouchers, accounts receivable/payable, financial reporting, shared operations, and an AI finance assistant.

## 3. Technology baseline

- Java 17
- Spring Boot 3.2.5
- Spring Cloud 2023.0.1
- Spring Cloud Alibaba / Nacos
- MyBatis-Plus 3.5.5
- MySQL 8
- Redis
- OpenFeign
- Spring Security and JWT
- Maven multi-module build

Do not silently upgrade framework or dependency versions. Version changes require an explicit compatibility assessment.

## 4. Module boundaries

- `gateway`: API gateway and routing concerns.
- `auth-service`: authentication and authorization.
- `base-service`: enterprise modeling, master data, platform capabilities, and AI assistant capabilities.
- `fi-service`: vouchers, general ledger, accounting periods, AR/AP, reconciliation, and financial reports.
- `share-service`: shared-operation capabilities.
- `common`: shared DTOs, responses, utilities, and abstractions.
- `infra`: infrastructure integrations.
- `config`: configuration-related modules.
- `generator`: code generation utilities.

Place code in the module that owns the business capability. Do not introduce cross-module dependencies only to reuse a small implementation detail.

## 5. Mandatory engineering rules

- Controllers receive parameters, invoke services, and convert responses only.
- Services own business rules, state transitions, idempotency, and transaction boundaries.
- Mappers own persistence operations, not business workflows.
- Prefer constructor injection for new code.
- Use `BigDecimal` for monetary values and exact financial calculations.
- Never use `double` or `float` for financial amounts.
- Use the common `ApiResponse<T>` structure for HTTP APIs.
- Prefer request/response DTOs over exposing persistence entities in new APIs.
- All database changes require a versioned SQL migration under `sql/`.
- Add a new migration for a new production change; do not rewrite released migration history.
- Never commit API keys, tokens, passwords, private certificates, or production credentials.
- Environment-specific addresses must come from environment variables or external configuration.
- Do not commit `.idea/`, `target/`, local logs, or generated build artifacts.
- Do not report completion without validation, or without explicitly explaining why validation could not run.

## 6. Spring transaction rules

- Put transaction boundaries on public service methods.
- Financial mutations should normally use `@Transactional(rollbackFor = Exception.class)`.
- Do not rely on same-class method calls to activate Spring AOP transactions.
- Keep a state change and its dependent accounting entries in one transaction when they form one atomic business operation.
- Define whether a batch uses one transaction for the whole batch or one transaction per item.
- External calls are not rolled back by database transactions; design idempotency, status tracking, retries, and reconciliation explicitly.

Read `.agents/skills/matrix-java-backend/references/transaction.md` before changing transaction-sensitive logic.

## 7. Financial invariants

- A submitted, audited, or posted voucher must contain valid lines.
- Debit and credit totals must be equal before posting.
- A posted voucher must not be edited as a draft.
- A closed accounting period must reject prohibited financial mutations.
- Posting must be idempotent and must not create duplicate ledger entries.
- Reversal must preserve traceability and create the opposite accounting effect.
- Financial state transitions must be explicit and validated.
- Multi-currency amounts and rates must use documented precision and rounding rules.
- Reporting logic must state organization, ledger, currency, and period scope.

Read `.agents/skills/matrix-finance-domain/SKILL.md` for changes affecting financial behavior.

## 8. AI capability rules

- Model credentials must come from environment variables or secret management.
- Every model call must have an explicit timeout.
- Model failures require a controlled error or documented fallback; never fabricate business facts.
- Persisted conversations must enforce ownership and access control.
- Knowledge retrieval results must be bounded in count and size.
- AI suggestions must not directly execute high-risk financial actions without authorization and validation.
- Record trace identifiers and usage metadata when available.

Read `.agents/skills/matrix-ai-development/SKILL.md` before changing AI assistant behavior.

## 9. Skill routing

Read every applicable skill before implementation:

- Java backend, Spring, transactions, Redis, Feign:
  `.agents/skills/matrix-java-backend/SKILL.md`
- Vouchers, ledger, periods, AR/AP, reports, reconciliation:
  `.agents/skills/matrix-finance-domain/SKILL.md`
- Tables, columns, indexes, migrations, SQL:
  `.agents/skills/matrix-database-change/SKILL.md`
- Models, prompts, conversations, retrieval, streaming, tool calling:
  `.agents/skills/matrix-ai-development/SKILL.md`
- Review, risk analysis, pre-merge checks:
  `.agents/skills/matrix-code-review/SKILL.md`

A task may require multiple skills. Adding a voucher field, for example, normally requires backend, finance-domain, database-change, and code-review skills.

## 10. Development workflow

1. Identify the owning module and an existing implementation pattern.
2. Read the applicable skills and references.
3. Describe the affected business states and invariants.
4. Identify API and database compatibility impact.
5. Implement the smallest complete change.
6. Add or update tests for normal, boundary, and failure paths.
7. Compile and test the affected module with its dependencies.
8. Review transaction, concurrency, idempotency, security, and migration risks.
9. Summarize changed files, behavior, validation, and remaining risks.

## 11. Validation commands

```bash
# Validate one module and required dependencies
mvn -pl <module> -am test

# Compile one module and required dependencies
mvn -pl <module> -am clean package -DskipTests

# Validate all modules for cross-module behavior
mvn test
```

Do not skip tests merely to obtain a successful build unless a compile-only check was explicitly requested.

## 12. Definition of done

A change is complete only when:

- The business behavior is implemented end to end.
- Financial invariants remain valid.
- Transaction and concurrency behavior has been reviewed.
- API and database compatibility has been considered.
- Relevant tests or builds have passed.
- No secret, production-only address, IDE file, or build artifact was introduced.
- The final report states what changed, how it was validated, and any remaining risks.
