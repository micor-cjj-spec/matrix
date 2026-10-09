# AI Runtime Refactor Phase 5: Observability and Task-Aware Model Routing

## Objective

Make the AI runtime measurable and allow different business workloads to use different model names without reintroducing multiple primary beans or provider-specific business code.

## Request contract

The public and internal chat contracts now include an optional `taskType` field. Existing clients remain compatible because missing values default to `general`.

Canonical task types:

- `general`
- `knowledge-qa`
- `financial-analysis`
- `tool-calling`
- `evaluation`

Aliases are normalized and unknown values safely fall back to `general`.

## Model selection

`AiTaskRouter` resolves the canonical task and model before Prompt creation. The selected model is written to the request-level Spring AI `ChatOptions`, so the auto-configured `ChatClient` and its observability remain intact.

Configuration:

```text
AI_CHAT_MODEL=gpt-4o-mini
AI_MODEL_KNOWLEDGE_QA=
AI_MODEL_FINANCIAL_ANALYSIS=
AI_MODEL_TOOL_CALLING=
AI_MODEL_EVALUATION=
```

A blank task-specific model falls back to `AI_CHAT_MODEL`.

This phase routes model names within one configured OpenAI-compatible provider. Multiple provider clients and cross-provider fallback remain separate follow-up work.

## Model metrics

`ai-service` records:

- `matrix.ai.model.requests`
- `matrix.ai.model.duration`
- `matrix.ai.model.errors`
- `matrix.ai.model.tokens`

Tags:

- operation: `chat` or `stream`
- task: normalized task type
- model: selected model name
- outcome: `success` or `failure`
- error: exception class category
- type: `prompt` or `completion` for tokens

Streaming token counts remain zero until terminal response metadata is aggregated.

## Remote-client metrics

`base-service` records:

- `matrix.ai.remote.requests`
- `matrix.ai.remote.duration`
- `matrix.ai.remote.attempts`
- `matrix.ai.remote.retries`
- `matrix.ai.remote.circuit.opens`
- `matrix.ai.remote.circuit.rejections`
- `matrix.ai.remote.fallbacks`

A logical request is counted once, while endpoint attempts and retries are counted separately. Circuit-open transitions are distinct from requests rejected while the circuit remains open.

## Cardinality and data safety

Metrics do not tag:

- user messages
- retrieved knowledge
- conversation identifiers
- trace identifiers
- complete exception messages
- complete endpoint URLs

Endpoint tags use only host and port authority. Retry reasons use bounded categories such as `http-5xx`, `http-429`, `transport`, and `sse-error`.

## Prometheus

Both runtime modules include the Prometheus Micrometer registry and expose Actuator metrics and Prometheus endpoints according to their configured application and management context paths.

## Compatibility

- Existing three-field internal model requests still compile through an overloaded constructor.
- Existing public callers do not need to send `taskType`.
- The default model remains unchanged.
- Existing tests can continue using the prior service constructors through isolated test registries.

## Follow-up

1. Aggregate streaming completion metadata and token usage.
2. Add model price configuration and estimated-cost metrics.
3. Add prompt version, tenant context, and audit Advisors.
4. Add multiple provider clients with provider-level health and fallback.
5. Start controlled read-only finance tools, beginning with month-end close checks.
