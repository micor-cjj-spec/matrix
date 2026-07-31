# Matrix AI Service

`ai-service` is the independent model-runtime boundary for Matrix. It runs on Spring Boot 3.5.x and Spring AI 1.1.x while the existing business services remain on their current Spring Boot baseline.

## Responsibility boundary

`base-service` continues to own:

- user authentication and conversation ownership
- conversation and message persistence
- knowledge retrieval and citations
- public `/ai/**` API compatibility
- ai-service endpoint selection, retries, circuit state, and adapter fallback

`ai-service` owns:

- Spring AI `ChatClient` integration
- provider-neutral Prompt construction
- synchronous model generation
- streaming model generation
- model response metadata and token usage
- internal model API authentication
- optional Nacos service registration

## Required environment variables

```text
AI_API_KEY=your-provider-key
AI_BASE_URL=https://api.openai.com
AI_COMPLETIONS_PATH=/v1/chat/completions
AI_CHAT_MODEL=gpt-4o-mini
AI_INTERNAL_TOKEN=use-a-long-random-shared-secret
```

For an OpenAI-compatible provider, set `AI_BASE_URL`, `AI_COMPLETIONS_PATH`, and `AI_CHAT_MODEL` to the provider's compatible values.

## Run

From the repository root:

```bash
mvn -pl ai-service spring-boot:run
```

The service listens on port `10020` by default with context path `/api`.

## Internal endpoints

All endpoints require:

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

When the selected adapter is `spring-ai`, the existing `prompt-http` adapter is used as the final fallback when enabled. Streaming requests only retry or fall back before the first delta has been emitted; after partial output, the error is returned instead of mixing two model responses.

## Current limitations

- streaming token usage is not yet aggregated
- circuit state is local to each `base-service` process
- Nacos registration is optional and disabled by default
- RAG still runs in `base-service`
- provider routing inside `ai-service` still uses one configured OpenAI-compatible provider
- tools and human confirmation are not yet enabled
