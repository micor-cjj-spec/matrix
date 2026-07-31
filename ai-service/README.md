# Matrix AI Service

`ai-service` is the independent model-runtime boundary for Matrix. It runs on Spring Boot 3.5.x and Spring AI 1.1.x while the existing business services remain on their current Spring Boot baseline.

## Responsibility boundary

`base-service` continues to own:

- user authentication and conversation ownership
- conversation and message persistence
- knowledge retrieval and citations
- public `/ai/**` API compatibility
- ai-service endpoint selection, retries, circuit state, and adapter fallback
- AI tool feature flags and organization-scope authorization

`ai-service` owns:

- Spring AI `ChatClient` integration
- provider-neutral Prompt construction
- task-aware model selection
- synchronous and streaming model generation
- controlled Spring AI tool registration and execution
- model and tool metrics with Prometheus export
- internal model API authentication
- optional Nacos service registration

`fi-service` owns finance rules and the read-only month-end workbench used by the first AI tool.

## Required environment variables

```text
AI_API_KEY=your-provider-key
AI_BASE_URL=https://api.openai.com
AI_COMPLETIONS_PATH=/v1/chat/completions
AI_CHAT_MODEL=gpt-4o-mini
AI_INTERNAL_TOKEN=use-a-long-random-shared-secret
```

For an OpenAI-compatible provider, set `AI_BASE_URL`, `AI_COMPLETIONS_PATH`, and `AI_CHAT_MODEL` to the provider's compatible values.

## Task-aware model routing

The public chat request accepts an optional `taskType` field. Supported canonical values are:

```text
general
knowledge-qa
financial-analysis
tool-calling
evaluation
```

Aliases such as `rag`, `finance`, `agent`, and `judge` are normalized. Missing or unknown values fall back to `general`.

Configure task-specific models with:

```text
AI_MODEL_KNOWLEDGE_QA=
AI_MODEL_FINANCIAL_ANALYSIS=
AI_MODEL_TOOL_CALLING=
AI_MODEL_EVALUATION=
```

Blank task-specific values fall back to `AI_CHAT_MODEL`. Routing currently changes the model name within the configured OpenAI-compatible provider; it does not create multiple provider clients.

Example analytical request:

```json
{
  "userMessage": "分析本月管理费用增长原因",
  "taskType": "financial-analysis"
}
```

## Controlled month-end close tool

The first business tool is:

```text
month-end-close-check
```

It reuses the existing `fi-service` month-end workbench and is strictly read-only. It can return period status, readiness score, aggregate voucher counts, blocking checks, warnings, and recommended actions. It cannot post vouchers, approve documents, close or reopen periods, or update finance data.

Public request example:

```json
{
  "userMessage": "检查 2026 年 7 月月结阻塞项，并给出处理顺序",
  "taskType": "tool-calling",
  "toolName": "month-end-close-check",
  "organizationId": 10,
  "accountingPeriod": "2026-07"
}
```

`base-service` validates the feature flag, tool allow-list, accounting period, and organization scope. It then creates a server-controlled tool context containing the user ID, organization ID, period, and request ID. These values are not model-generated tool arguments.

Enable the feature in `base-service`:

```text
AI_MODEL_ADAPTER=spring-ai
AI_TOOL_CALLING_ENABLED=true
AI_TOOL_ALLOW_ALL_ORGANIZATIONS=false
AI_TOOL_ALLOWED_USER_ORG_PAIRS=7:10
```

`AI_TOOL_ALLOWED_USER_ORG_PAIRS` is a migration-only fallback using `userId:organizationId` entries. Production environments should prefer security authorities or a dedicated permission service. `AI_TOOL_ALLOW_ALL_ORGANIZATIONS` should only be enabled in a controlled development environment.

Configure the internal finance client in `ai-service`:

```text
FINANCE_SERVICE_BASE_URL=http://127.0.0.1:10003/api
FINANCE_AI_TOOL_INTERNAL_TOKEN=use-a-separate-long-random-secret
FINANCE_AI_TOOL_TIMEOUT_SECONDS=20
```

Configure the same tool token in `fi-service`:

```text
FINANCE_AI_TOOL_INTERNAL_TOKEN=use-the-same-separate-secret
FINANCE_AI_TOOL_MAX_CHECK_ITEMS=20
FINANCE_AI_TOOL_MAX_WARNINGS=20
```

Internal finance endpoint:

```text
POST /api/internal/ai/tools/month-end-close-check
X-Matrix-AI-Tool-Token: <FINANCE_AI_TOOL_INTERNAL_TOKEN>
```

The internal result is bounded and deliberately excludes voucher details. It always includes `readOnly=true`; the AI client rejects a result that is not explicitly marked read-only.

Tool requests require `AI_MODEL_ADAPTER=spring-ai`. If the tool or finance service fails, the request returns an error and never falls back to a normal chat model without verified finance data.

## Run

From the repository root:

```bash
mvn -pl ai-service spring-boot:run
```

The service listens on port `10020` by default with context path `/api`.

## Internal model endpoints

All model endpoints require:

```text
X-Matrix-Internal-Token: <AI_INTERNAL_TOKEN>
```

Endpoints:

```text
GET  /api/internal/model/status
POST /api/internal/model/chat
POST /api/internal/model/chat/stream
```

The streaming endpoint uses named SSE events:

```text
start
delta
done
error
```

## Static-address mode

Static mode remains the default and does not require Nacos:

```text
AI_MODEL_ADAPTER=spring-ai
AI_SERVICE_BASE_URL=http://127.0.0.1:10020/api
AI_SERVICE_DISCOVERY_ENABLED=false
AI_INTERNAL_TOKEN=the-same-shared-secret
```

## Nacos discovery mode

`ai-service` uses the Spring Boot 3.5-compatible Spring Cloud 2025.0.x and Spring Cloud Alibaba 2025.0.x line.

Registration is disabled by default. Enable it only after the target Nacos environment is compatible:

```text
AI_DISCOVERY_ENABLED=true
NACOS_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=public
NACOS_GROUP=DEV_GROUP
```

When using the `public` or empty namespace with Spring Cloud Alibaba 2025.0.x, use Nacos Server 3.x.

Enable discovery from `base-service`:

```text
AI_SERVICE_DISCOVERY_ENABLED=true
AI_SERVICE_ID=ai-service
AI_SERVICE_STATIC_FALLBACK_ENABLED=true
AI_SERVICE_BASE_URL=http://127.0.0.1:10020/api
```

Discovered instances publish `matrix.context-path=/api`. The client rotates candidate order between requests and appends the static endpoint only when static fallback is enabled.

## Retry and circuit settings

```text
AI_SERVICE_MAX_ATTEMPTS=2
AI_SERVICE_CIRCUIT_FAILURE_THRESHOLD=3
AI_SERVICE_CIRCUIT_OPEN_SECONDS=30
AI_FALLBACK_ENABLED=true
```

Retryable failures include connection failures, timeouts, HTTP 408, HTTP 429, and HTTP 5xx responses. HTTP 4xx responses other than 408 and 429 are not retried.

For ordinary chat requests, the existing `prompt-http` adapter is the final fallback when enabled. Streaming chat only retries or falls back before the first delta has been emitted. Controlled tool requests never use this fallback because an unverified generated answer must not replace real finance data.

## Metrics

Both `ai-service` and `base-service` expose:

```text
/api/actuator/metrics
/api/actuator/prometheus
```

Model-runtime metrics:

```text
matrix.ai.model.requests
matrix.ai.model.duration
matrix.ai.model.errors
matrix.ai.model.tokens
```

Remote-client metrics:

```text
matrix.ai.remote.requests
matrix.ai.remote.duration
matrix.ai.remote.attempts
matrix.ai.remote.retries
matrix.ai.remote.circuit.opens
matrix.ai.remote.circuit.rejections
matrix.ai.remote.fallbacks
```

Controlled tool metrics:

```text
matrix.ai.tools.calls
matrix.ai.tools.duration
```

Metric tags never contain prompts, retrieved document content, user IDs, organization IDs, accounting periods, request IDs, full exception messages, or full URLs.

## Current limitations

- streaming token usage is not yet aggregated
- circuit state is local to each `base-service` process
- Nacos registration is optional and disabled by default
- RAG still runs in `base-service`
- model routing currently uses one configured OpenAI-compatible provider
- organization tool authorization should eventually use a dedicated organization-permission service
- the finance tool client uses a static service URL in this phase
- persistent tool execution audit records and human-confirmed write tools are not yet implemented
